package ch.schlierelacht.admin.security;

import static ch.schlierelacht.admin.util.DateUtil.now;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import ch.schlierelacht.admin.jooq.tables.daos.LoginDao;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEvents {

    private final LoginDao loginDao;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        if (success.getSource() instanceof UsernamePasswordAuthenticationToken token && token.getPrincipal() instanceof UserDetails user) {
            var login = loginDao.findById(user.getUsername());
            login.setLastLogin(now());
            loginDao.update(login);
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        if (failure.getSource() instanceof UsernamePasswordAuthenticationToken token) {
            log.warn("Authentication failed for {}: {}", token.getPrincipal(), failure.getException().getMessage());
        }
    }
}
