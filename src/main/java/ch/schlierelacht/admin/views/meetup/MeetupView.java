package ch.schlierelacht.admin.views.meetup;

import ch.schlierelacht.admin.jooq.tables.pojos.MeetupRegistration;
import ch.schlierelacht.admin.dto.MeetupJahrgang;
import ch.schlierelacht.admin.service.MeetupService;
import ch.schlierelacht.admin.util.DateUtil;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

@PageTitle("Jahrgangstreffen")
@Route(value = "meetup", layout = MainLayout.class)
@PermitAll
public class MeetupView extends VerticalLayout {

    private final MeetupService meetupService;
    private final Grid<MeetupRegistration> grid;

    public MeetupView(MeetupService meetupService) {
        this.meetupService = meetupService;
        setSizeFull();

        add(new H2("Jahrgangstreffen Anmeldungen"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<MeetupRegistration> createGrid() {
        var g = new Grid<MeetupRegistration>();
        g.addComponentColumn(r -> {
            var delete = new Button(TRASH.create(), _ -> {
                meetupService.delete(r.getId());
                refreshGrid();
                showNotification("Eintrag gelöscht", LUMO_SUCCESS);
            });
            delete.addThemeVariants(LUMO_ERROR);
            return delete;
        }).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(MeetupRegistration::getLastname).setHeader("Nachname").setSortable(true);
        g.addColumn(MeetupRegistration::getFirstname).setHeader("Vorname").setSortable(true);
        g.addColumn(MeetupRegistration::getEmail).setHeader("E-Mail").setSortable(true);
        g.addColumn(r -> MeetupJahrgang.fromDb(r.getJahrgang()).map(MeetupJahrgang::getDescription).orElse("")).setHeader("Jahrgang").setSortable(true);
        g.addColumn(r -> Boolean.TRUE.equals(r.getShowOnList()) ? "Ja" : "Nein").setHeader("Teilnehmerliste").setSortable(true);
        g.addColumn(r -> DateUtil.formatDateTime(r.getRegisteredAt())).setHeader("Registriert am").setSortable(true);
        return g;
    }

    private HorizontalLayout createToolbar() {
        var handler = DownloadHandler.fromInputStream(event -> {
            byte[] csv = buildCsv();
            return new DownloadResponse(new ByteArrayInputStream(csv), "anmeldungen.csv", "text/csv; charset=UTF-8", csv.length);
        });
        return new HorizontalLayout(new Anchor(handler, "CSV exportieren"));
    }

    private void refreshGrid() {
        grid.setItems(meetupService.findAll());
    }

    private byte[] buildCsv() {
        var sb = new StringBuilder();
        sb.append("Nachname,Vorname,E-Mail,Jahrgang,Teilnehmerliste,Registriert am\n");
        for (var r : meetupService.findAll()) {
            var jahrgang = MeetupJahrgang.fromDb(r.getJahrgang()).map(MeetupJahrgang::getDescription).orElse("");
            sb.append(csvEscape(r.getLastname())).append(',')
              .append(csvEscape(r.getFirstname())).append(',')
              .append(csvEscape(r.getEmail())).append(',')
              .append(csvEscape(jahrgang)).append(',')
              .append(Boolean.TRUE.equals(r.getShowOnList()) ? "Ja" : "Nein").append(',')
              .append(DateUtil.formatDateTime(r.getRegisteredAt())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
