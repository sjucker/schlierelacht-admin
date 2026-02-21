package ch.schlierelacht.admin.views.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class NotificationUtil {

    public static void showNotification(String message, NotificationVariant variant) {
        var notification = Notification.show(message);
        notification.addThemeVariants(variant);
        notification.setPosition(Position.TOP_CENTER);
    }

}
