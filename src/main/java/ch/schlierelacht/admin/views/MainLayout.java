package ch.schlierelacht.admin.views;

import ch.schlierelacht.admin.views.artist.ArtistView;
import ch.schlierelacht.admin.views.gastro.GastroView;
import ch.schlierelacht.admin.views.location.LocationView;
import ch.schlierelacht.admin.views.programm.ProgrammView;
import ch.schlierelacht.admin.views.settings.SettingsView;
import ch.schlierelacht.admin.views.tag.TagView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxSizing;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Height;
import com.vaadin.flow.theme.lumo.LumoUtility.ListStyleType;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.core.userdetails.UserDetails;

import static org.vaadin.lineawesome.LineAwesomeIcon.CALENDAR_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.COG_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.MAP_MARKER_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.PIZZA_SLICE_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.TAGS_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.USER_SOLID;

@Slf4j
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final AuthenticationContext authContext;
    private final BuildProperties buildProperties;

    public static class MenuItemInfo extends ListItem {

        private final Class<? extends Component> view;

        public MenuItemInfo(String menuTitle, Component icon, Class<? extends Component> view) {
            this.view = view;
            RouterLink link = new RouterLink();
            // Use Lumo classnames for various styling
            link.addClassNames(Display.FLEX, Gap.XSMALL, Height.MEDIUM, AlignItems.CENTER, Padding.Horizontal.SMALL,
                               TextColor.BODY);
            link.setRoute(view);

            Span text = new Span(menuTitle);
            // Use Lumo classnames for various styling
            text.addClassNames(FontWeight.MEDIUM, FontSize.MEDIUM, Whitespace.NOWRAP);

            if (icon != null) {
                link.add(icon);
            }
            link.add(text);
            add(link);
        }

        public Class<?> getView() {
            return view;
        }
    }

    public MainLayout(AuthenticationContext authContext,
                      BuildProperties buildProperties) {
        this.authContext = authContext;
        this.buildProperties = buildProperties;
        addToNavbar(createHeaderContent());
    }

    private Component createHeaderContent() {
        Header header = new Header();
        header.addClassNames(BoxSizing.BORDER, Display.FLEX, FlexDirection.COLUMN, Width.FULL);

        Div layout = new Div();
        layout.addClassNames(Display.FLEX, AlignItems.CENTER, Padding.Horizontal.LARGE);

        H1 appName = new H1("Schlierefäscht - Admin");
        appName.addClassNames(Margin.Vertical.MEDIUM, Margin.End.AUTO, FontSize.LARGE);
        layout.add(appName);

        var version = new Span(buildProperties.getVersion());
        version.addClassNames(FontSize.XXSMALL);
        layout.add(version);

        authContext.getAuthenticatedUser(UserDetails.class)
                   .ifPresent(_ -> {
                       var logout = new Button("Logout", _ -> authContext.logout());
                       logout.addClassNames(Margin.Left.MEDIUM);
                       layout.add(logout);
                   });

        Nav nav = new Nav();
        nav.addClassNames(Display.FLEX, Overflow.AUTO, Padding.Horizontal.MEDIUM, Padding.Vertical.XSMALL);

        // Wrap the links in a list; improves accessibility
        UnorderedList list = new UnorderedList();
        list.addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE);
        nav.add(list);

        for (MenuItemInfo menuItem : createMenuItems()) {
            list.add(menuItem);
        }

        header.add(layout, nav);
        return header;
    }

    private MenuItemInfo[] createMenuItems() {
        return new MenuItemInfo[]{
                new MenuItemInfo("Standorte", MAP_MARKER_SOLID.create(), LocationView.class),
                new MenuItemInfo("Gastronomie", PIZZA_SLICE_SOLID.create(), GastroView.class),
                new MenuItemInfo("Künstler", USER_SOLID.create(), ArtistView.class),
                new MenuItemInfo("Programm", CALENDAR_SOLID.create(), ProgrammView.class),
                new MenuItemInfo("Tags", TAGS_SOLID.create(), TagView.class),
                new MenuItemInfo("Einstellungen", COG_SOLID.create(), SettingsView.class),
        };
    }
}
