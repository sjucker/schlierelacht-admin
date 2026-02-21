package ch.schlierelacht.admin.views.settings;

import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

@PageTitle("Settings")
@Route(value = "settings", layout = MainLayout.class)
@Menu(order = 10, icon = LineAwesomeIconUrl.COG_SOLID)
@PermitAll
public class SettingsView extends VerticalLayout {

    public SettingsView(SettingsService settingsService) {
        add(new H2("Passwort ändern"));

        var formLayout = new FormLayout();

        var currentPasswordField = new PasswordField("Aktuelles Passwort");
        currentPasswordField.setRequired(true);
        var newPasswordField = new PasswordField("Neues Passwort");
        newPasswordField.setRequired(true);
        var confirmPasswordField = new PasswordField("Neues Passwort bestätigen");
        confirmPasswordField.setRequired(true);

        var changePasswordButton = new Button("Ändern", _ -> {
            var currentPassword = currentPasswordField.getValue();
            var newPassword = newPasswordField.getValue();
            var confirmPassword = confirmPasswordField.getValue();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showNotification("Alle Felder ausfüllen", LUMO_ERROR);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showNotification("Neues Passwort stimmt nicht überein", LUMO_ERROR);
                return;
            }

            var result = settingsService.changePassword(currentPassword, newPassword);
            switch (result) {
                case SUCCESS -> {
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    showNotification("Passwort erfolgreich geändert", LUMO_SUCCESS);
                }
                case INCORRECT_CURRENT_PASSWORD -> showNotification("Aktuelles Passwort ist falsch", LUMO_ERROR);
                case USER_NOT_FOUND -> showNotification("Benutzer nicht gefunden", LUMO_ERROR);
            }
        });
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(currentPasswordField, newPasswordField, confirmPasswordField, changePasswordButton);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.setMaxWidth("400px");

        add(formLayout);
    }

}
