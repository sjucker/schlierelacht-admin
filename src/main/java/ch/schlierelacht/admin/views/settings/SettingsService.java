package ch.schlierelacht.admin.views.settings;

import ch.schlierelacht.admin.jooq.tables.daos.LoginDao;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static ch.schlierelacht.admin.views.settings.SettingsService.ChangePasswordResult.INCORRECT_CURRENT_PASSWORD;
import static ch.schlierelacht.admin.views.settings.SettingsService.ChangePasswordResult.SUCCESS;
import static ch.schlierelacht.admin.views.settings.SettingsService.ChangePasswordResult.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final PasswordEncoder passwordEncoder;
    private final LoginDao loginDao;
    private final AuthenticationContext authContext;

    public ChangePasswordResult changePassword(@NonNull String currentPassword, @NonNull String newPassword) {
        return authContext.getAuthenticatedUser(UserDetails.class)
                          .flatMap(userDetails -> loginDao.fetchOptionalByEmail(userDetails.getUsername()))
                          .map(login -> {
                              if (passwordEncoder.matches(currentPassword, login.getPassword())) {
                                  login.setPassword(passwordEncoder.encode(newPassword));
                                  loginDao.update(login);
                                  return SUCCESS;
                              } else {
                                  return INCORRECT_CURRENT_PASSWORD;
                              }
                          })
                          .orElse(USER_NOT_FOUND);
    }

    public enum ChangePasswordResult {
        SUCCESS,
        INCORRECT_CURRENT_PASSWORD,
        USER_NOT_FOUND
    }
}
