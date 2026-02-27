package ch.schlierelacht.admin.views.programm;

import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.daos.LocationDao;
import ch.schlierelacht.admin.jooq.tables.daos.ProgrammDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Attraction;
import ch.schlierelacht.admin.jooq.tables.pojos.Location;
import ch.schlierelacht.admin.jooq.tables.pojos.Programm;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;

@PageTitle("Programm")
@Route(value = "programm", layout = MainLayout.class)
@PermitAll
public class ProgrammView extends VerticalLayout {

    private final ProgrammDao programmDao;
    private final AttractionDao attractionDao;
    private final LocationDao locationDao;
    private final Grid<Programm> grid;
    private final ProgrammDialog dialog;

    private Map<Long, String> attractionNames;
    private Map<Long, String> locationNames;

    public ProgrammView(ProgrammDao programmDao, AttractionDao attractionDao, LocationDao locationDao) {
        this.programmDao = programmDao;
        this.attractionDao = attractionDao;
        this.locationDao = locationDao;

        this.dialog = new ProgrammDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });

        setSizeFull();
        add(new H2("Programm verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Programm> createGrid() {
        var g = new Grid<Programm>();
        g.addComponentColumn(p -> new Button(EDIT.create(), _ -> dialog.open(p))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);

        g.addColumn(p -> attractionNames.getOrDefault(p.getAttractionId(), "Unbekannt")).setHeader("Attraktion").setSortable(true);
        g.addColumn(p -> locationNames.getOrDefault(p.getLocationId(), "Unbekannt")).setHeader("Standort").setSortable(true);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        g.addColumn(p -> p.getFromDate() != null ? p.getFromDate().format(dateFormatter) : "").setHeader("Von Datum").setSortable(true);
        g.addColumn(p -> p.getFromTime() != null ? p.getFromTime().format(timeFormatter) : "").setHeader("Von Zeit").setSortable(true);
        g.addColumn(p -> p.getToDate() != null ? p.getToDate().format(dateFormatter) : "").setHeader("Bis Datum").setSortable(true);
        g.addColumn(p -> p.getToTime() != null ? p.getToTime().format(timeFormatter) : "").setHeader("Bis Zeit").setSortable(true);

        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addProgrammButton = new Button("Programm-Eintrag hinzufügen", _ -> {
            Programm programm = new Programm();
            dialog.open(programm);
        });

        return new HorizontalLayout(addProgrammButton);
    }

    private void refreshGrid() {
        attractionNames = attractionDao.findAll().stream().collect(Collectors.toMap(Attraction::getId, Attraction::getName));
        locationNames = locationDao.findAll().stream().collect(Collectors.toMap(Location::getId, Location::getName));

        grid.setItems(programmDao.findAll().stream()
                                 .sorted(Comparator.comparing(Programm::getFromDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                                   .thenComparing(Programm::getFromTime, Comparator.nullsLast(Comparator.naturalOrder())))
                                 .toList());
    }

    private class ProgrammDialog extends Dialog {

        private final Binder<Programm> binder = new Binder<>(Programm.class);

        public ProgrammDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setHeaderTitle("Programm-Eintrag bearbeiten");

            var form = new FormLayout();

            var attraction = new ComboBox<Attraction>("Attraktion");
            attraction.setItems(attractionDao.findAll());
            attraction.setItemLabelGenerator(Attraction::getName);

            var location = new ComboBox<Location>("Standort");
            location.setItems(locationDao.findAll());
            location.setItemLabelGenerator(Location::getName);

            var fromDate = new DatePicker("Von Datum");
            var fromTime = new TimePicker("Von Zeit");
            var toDate = new DatePicker("Bis Datum");
            var toTime = new TimePicker("Bis Zeit");

            form.add(attraction, location, fromDate, fromTime, toDate, toTime);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(attraction)
                  .asRequired()
                  .bind(p -> p.getAttractionId() != null ? attractionDao.fetchOneById(p.getAttractionId()) : null,
                        (p, v) -> p.setAttractionId(v != null ? v.getId() : null));

            binder.forField(location)
                  .asRequired()
                  .bind(p -> p.getLocationId() != null ? locationDao.fetchOneById(p.getLocationId()) : null,
                        (p, v) -> p.setLocationId(v != null ? v.getId() : null));

            binder.forField(fromDate)
                  .asRequired()
                  .bind(Programm::getFromDate, Programm::setFromDate);

            binder.forField(fromTime)
                  .bind(Programm::getFromTime, Programm::setFromTime);

            binder.forField(toDate)
                  .bind(Programm::getToDate, Programm::setToDate);

            binder.forField(toTime)
                  .bind(Programm::getToTime, Programm::setToTime);

            var save = new Button("Speichern", _ -> {
                save();
                onSuccessCallback.run();
            });
            save.addThemeVariants(LUMO_PRIMARY);

            var delete = new Button("Löschen", _ -> {
                delete();
                onSuccessCallback.run();
            });
            delete.addThemeVariants(LUMO_ERROR);

            var cancel = new Button("Abbrechen");
            cancel.addClickListener(_ -> close());

            getFooter().add(delete, cancel, save);
        }

        public void open(Programm programm) {
            binder.setBean(programm);
            super.open();
        }

        private void save() {
            if (binder.validate().isOk()) {
                var programm = binder.getBean();
                if (programm.getId() == null) {
                    programmDao.insert(programm);
                } else {
                    programmDao.update(programm);
                }
                close();
            } else {
                showNotification("Alle erforderlichen Felder ausfüllen", LUMO_WARNING);
            }
        }

        private void delete() {
            var programm = binder.getBean();
            if (programm != null && programm.getId() != null) {
                programmDao.delete(programm);
                close();
            }
        }
    }
}
