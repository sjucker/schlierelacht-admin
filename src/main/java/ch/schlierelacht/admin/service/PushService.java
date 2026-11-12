package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.PushRegistrationDTO;
import ch.schlierelacht.admin.util.DateUtil;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.schlierelacht.admin.jooq.Tables.PUSH_DEVICE_TOKEN;
import static org.apache.commons.lang3.StringUtils.right;

/**
 * Stores mobile FCM device tokens and sends push notifications to the
 * {@value #GENERAL_TOPIC} topic through the Firebase Admin SDK. Sending is only
 * possible when a {@link FirebaseMessaging} bean is configured (see
 * {@code FirebaseConfig}); {@link #isSendingAvailable()} reflects that.
 * <p>
 * Logging: device tokens appear only abbreviated (last characters), and every
 * push is logged with its sender, title, route and the FCM message id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    /**
     * Broadcast topic every device subscribes to on startup.
     */
    public static final String GENERAL_TOPIC = "general";

    private final DSLContext dslContext;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    /**
     * Upserts a device token (idempotent on the unique token column).
     */
    @Transactional
    public void register(PushRegistrationDTO dto) {
        var now = DateUtil.now();
        dslContext.insertInto(PUSH_DEVICE_TOKEN)
                  .set(PUSH_DEVICE_TOKEN.TOKEN, dto.token())
                  .set(PUSH_DEVICE_TOKEN.PLATFORM, dto.platform())
                  .set(PUSH_DEVICE_TOKEN.LOCALE, dto.locale())
                  .set(PUSH_DEVICE_TOKEN.CREATED_AT, now)
                  .set(PUSH_DEVICE_TOKEN.UPDATED_AT, now)
                  .onConflict(PUSH_DEVICE_TOKEN.TOKEN)
                  .doUpdate()
                  .set(PUSH_DEVICE_TOKEN.PLATFORM, dto.platform())
                  .set(PUSH_DEVICE_TOKEN.LOCALE, dto.locale())
                  .set(PUSH_DEVICE_TOKEN.UPDATED_AT, now)
                  .execute();
        log.debug("Registered push token …{} (platform={}, locale={})", abbreviate(dto.token()), dto.platform(), dto.locale());
    }

    public boolean isSendingAvailable() {
        return firebaseMessaging.getIfAvailable() != null;
    }

    /**
     * Number of registered device tokens.
     */
    @Transactional(readOnly = true)
    public int deviceCount() {
        return dslContext.fetchCount(PUSH_DEVICE_TOKEN);
    }

    /**
     * Sends a notification to the {@value #GENERAL_TOPIC} topic.
     */
    public void sendToGeneralTopic(String title, String body, String route) {
        var sender = currentUser();
        var messaging = requireMessaging(sender);
        var message = Message.builder()
                             .setTopic(GENERAL_TOPIC)
                             .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                             .putData("route", route == null ? "" : route)
                             .build();
        log.info("Sending push to topic '{}' by {}: title='{}', route='{}', body={} chars",
                 GENERAL_TOPIC, sender, title, route, body == null ? 0 : body.length());
        try {
            var messageId = messaging.send(message);
            log.info("Push to topic '{}' sent by {}: messageId={}", GENERAL_TOPIC, sender, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("Push to topic '{}' by {} failed: errorCode={}, messagingErrorCode={}, httpStatus={}",
                      GENERAL_TOPIC, sender, e.getErrorCode(), e.getMessagingErrorCode(),
                      e.getHttpResponse() == null ? null : e.getHttpResponse().getStatusCode(), e);
            throw new IllegalStateException("Push an Topic fehlgeschlagen: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Push to topic '{}' by {} failed", GENERAL_TOPIC, sender, e);
            throw new IllegalStateException("Push an Topic fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private FirebaseMessaging requireMessaging(String sender) {
        var messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.warn("Push by {} rejected: Firebase is not configured (FIREBASE_CREDENTIALS missing or invalid)", sender);
            throw new IllegalStateException(
                    "Firebase ist nicht konfiguriert (FIREBASE_CREDENTIALS fehlt oder ist ungültig).");
        }
        return messaging;
    }

    /**
     * The logged-in admin user sending the push (the Vaadin request thread carries the security context).
     */
    private static String currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "unknown" : authentication.getName();
    }

    /**
     * Device tokens are credentials for pushing to one device: log only their last characters.
     */
    private static String abbreviate(String token) {
        return right(token, 8);
    }
}
