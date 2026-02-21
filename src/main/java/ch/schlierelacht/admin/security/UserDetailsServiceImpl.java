package ch.schlierelacht.admin.security;

import ch.schlierelacht.admin.jooq.tables.daos.LoginDao;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LoginDao loginDao;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        return loginDao.fetchOptionalByEmail(email)
                       .map(login -> new User(login.getEmail(), login.getPassword(),
                                              login.getActive() != null ? login.getActive() : true,
                                              true, true, true,
                                              Collections.emptyList()))
                       .orElseThrow(() -> new UsernameNotFoundException("No user present with email: " + email));
    }
}
