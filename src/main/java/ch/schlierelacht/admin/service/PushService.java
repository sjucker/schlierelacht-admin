package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.PushRegistrationDTO;
import ch.schlierelacht.admin.util.DateUtil;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.schlierelacht.admin.jooq.Tables.PUSH_DEVICE_TOKEN;

/**
 * Stores mobile FCM device tokens and sends push notifications through the
 * Firebase Admin SDK. Sending is only possible when a {@link FirebaseMessaging}
 * bean is configured (see {@code FirebaseConfig}); {@link #isSendingAvailable()}
 * reflects that.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    /** FCM caps multicast sends at 500 tokens per request. */
    private static final int MULTICAST_BATCH_SIZE = 500;

    /** Broadcast topic every device subscribes to on startup. */
    public static final String GENERAL_TOPIC = "general";

    private final DSLContext dslContext;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    /** Upserts a device token (idempotent on the unique token column). */
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
    }

    public boolean isSendingAvailable() {
        return firebaseMessaging.getIfAvailable() != null;
    }

    /** Number of registered device tokens. */
    @Transactional(readOnly = true)
    public int deviceCount() {
        return dslContext.fetchCount(PUSH_DEVICE_TOKEN);
    }

    /**
     * Sends a notification to every registered device token. Returns the number
     * of messages FCM accepted.
     */
    @Transactional(readOnly = true)
    public int sendToAllDevices(String title, String body, String route) {
        var messaging = requireMessaging();
        var tokens = dslContext.select(PUSH_DEVICE_TOKEN.TOKEN)
                               .from(PUSH_DEVICE_TOKEN)
                               .fetch(PUSH_DEVICE_TOKEN.TOKEN);
        if (tokens.isEmpty()) {
            return 0;
        }
        var notification = Notification.builder().setTitle(title).setBody(body).build();
        var success = 0;
        for (var start = 0; start < tokens.size(); start += MULTICAST_BATCH_SIZE) {
            var batch = tokens.subList(start, Math.min(start + MULTICAST_BATCH_SIZE, tokens.size()));
            var message = MulticastMessage.builder()
                                          .addAllTokens(batch)
                                          .setNotification(notification)
                                          .putData("route", route == null ? "" : route)
                                          .build();
            try {
                BatchResponse response = messaging.sendEachForMulticast(message);
                success += response.getSuccessCount();
                if (response.getFailureCount() > 0) {
                    log.warn("Push send: {} of {} failed in batch", response.getFailureCount(), batch.size());
                }
            } catch (Exception e) {
                log.error("Push send failed for a batch of {} tokens", batch.size(), e);
            }
        }
        return success;
    }

    /** Sends a notification to the {@value #GENERAL_TOPIC} topic. */
    public void sendToGeneralTopic(String title, String body, String route) {
        var messaging = requireMessaging();
        var message = Message.builder()
                             .setTopic(GENERAL_TOPIC)
                             .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                             .putData("route", route == null ? "" : route)
                             .build();
        try {
            messaging.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Push an Topic fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private FirebaseMessaging requireMessaging() {
        var messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            throw new IllegalStateException(
                    "Firebase ist nicht konfiguriert (app.firebase-credentials fehlt).");
        }
        return messaging;
    }
}
