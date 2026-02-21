package ch.schlierelacht.views.login;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
public class LoginView extends LoginOverlay implements BeforeEnterObserver {

    private final AuthenticationContext authenticationContext;

    public LoginView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        setAction(RouteUtil.getRoutePath(VaadinService.getCurrent().getContext(), getClass()));

        var i18n = LoginI18n.createDefault();

        var i18nHeader = new LoginI18n.Header();
        i18nHeader.setTitle("Schlierefäscht - Admin");
        i18nHeader.setDescription("Melde dich mit deiner E-Mail an.");
        i18n.setHeader(i18nHeader);

        var i18nForm = i18n.getForm();
        i18nForm.setTitle("Login");
        i18nForm.setUsername("E-Mail");
        i18nForm.setPassword("Passwort");
        i18nForm.setSubmit("Login");
        i18nForm.setForgotPassword("Passwort vergessen");

        var i18nErrorMessage = i18n.getErrorMessage();
        i18nErrorMessage.setTitle("E-Mail oder Passwort inkorrekt");
        i18nErrorMessage.setMessage("Stelle sicher, dass du die richtige E-Mail verwendest.");
        i18nErrorMessage.setUsername("E-Mail eingeben");
        i18nErrorMessage.setPassword("Passwort eingeben");
        setI18n(i18n);

        setForgotPasswordButtonVisible(false);
        setOpened(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            setOpened(false);
            event.forwardTo("");
        }

        setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }
}
