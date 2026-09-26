package ch.schlierelacht.admin.views.notification;

import ch.schlierelacht.admin.service.PushService;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

/**
 * Admin view to compose and send a push notification to the broadcast topic
 * ({@code general}), which every app installation subscribes to.
 */
@PageTitle("Push senden")
@Route(value = "push", layout = MainLayout.class)
@PermitAll
public class PushNotificationView extends VerticalLayout {

    private final PushService pushService;

    public PushNotificationView(PushService pushService) {
        this.pushService = pushService;
        setMaxWidth("640px");

        add(new H2("Push-Benachrichtigung senden"));

        if (!pushService.isSendingAvailable()) {
            var warning = new Paragraph(
                    "Firebase ist nicht konfiguriert (app.firebase-credentials fehlt). "
                            + "Das Versenden ist deaktiviert.");
            warning.getStyle().set("color", "var(--lumo-error-text-color)");
            add(warning);
        }

        add(new Paragraph(pushService.deviceCount() + " registrierte Geräte."));

        var title = new TextField("Titel");
        title.setWidthFull();
        var body = new TextArea("Nachricht");
        body.setWidthFull();
        var route = new TextField("Route (optional, z.B. \"news\")");
        route.setWidthFull();

        var send = new Button("Senden");
        send.addThemeVariants(LUMO_PRIMARY);
        send.setDisableOnClick(true);
        send.setEnabled(pushService.isSendingAvailable());
        send.addClickListener(_ -> {
            try {
                if (title.isEmpty() || body.isEmpty()) {
                    showNotification("Titel und Nachricht sind erforderlich.", LUMO_ERROR);
                    return;
                }
                pushService.sendToGeneralTopic(title.getValue(), body.getValue(), route.getValue());
                showNotification("An Topic \"general\" gesendet.", LUMO_SUCCESS);
                title.clear();
                body.clear();
                route.clear();
            } catch (RuntimeException ex) {
                showNotification("Fehler: " + ex.getMessage(), LUMO_ERROR);
            } finally {
                send.setEnabled(pushService.isSendingAvailable());
            }
        });

        add(title, body, route, send);
    }
}
