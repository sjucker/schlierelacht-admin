package ch.schlierelacht.admin.views.location;

import ch.schlierelacht.admin.dto.LocationType;
import ch.schlierelacht.admin.jooq.tables.daos.LocationDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Location;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.Comparator;

import static ch.schlierelacht.admin.mapper.EnumMapper.INSTANCE;
import static ch.schlierelacht.admin.util.MapUtil.getGoogleMapsCoordinates;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.html.AnchorTarget.BLANK;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

@PageTitle("Standorte")
@Route(value = "locations", layout = MainLayout.class)
@PermitAll
public class LocationView extends VerticalLayout {

    private final LocationDao locationDao;
    private final Grid<Location> grid;
    private final LocationDialog dialog;

    public LocationView(LocationDao locationDao) {
        this.locationDao = locationDao;
        this.dialog = new LocationDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("Standorte verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Location> createGrid() {
        var g = new Grid<Location>();
        g.addComponentColumn(l -> new Button(EDIT.create(), _ -> dialog.open(l))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Location::getName).setHeader("Name").setSortable(true);
        g.addColumn(l -> INSTANCE.fromDb(l.getType()).getDescription()).setHeader("Typ").setSortable(true);
        g.addColumn(Location::getExternalId).setHeader("External ID");
        g.addColumn(Location::getMapId).setHeader("Map ID");
        g.addColumn(Location::getSortOrder).setHeader("Sortierung");
        g.addComponentColumn(l -> new Anchor(getGoogleMapsCoordinates(l.getLatitude(), l.getLongitude()), "Google Maps", BLANK));
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addLocationButton = new Button("Standort hinzufügen", _ -> {
            Location location = new Location();
            location.setSortOrder(0);
            dialog.open(location);
        });

        return new HorizontalLayout(addLocationButton);
    }

    private void refreshGrid() {
        grid.setItems(locationDao.findAll().stream().sorted(Comparator.comparing(Location::getSortOrder)).toList());
    }

    private class LocationDialog extends Dialog {

        private final Binder<Location> binder = new Binder<>(Location.class);

        public LocationDialog(Runnable onSuccessCallback) {
            setHeaderTitle("Standort bearbeiten");

            var form = new FormLayout();

            var type = new ComboBox<LocationType>("Typ");
            type.setItems(LocationType.values());
            type.setItemLabelGenerator(LocationType::getDescription);

            var externalId = new TextField("External ID");
            var name = new TextField("Name");
            var latitude = new BigDecimalField("Breitengrad");
            var longitude = new BigDecimalField("Längengrad");
            var sortOrder = new IntegerField("Sortierung");
            var cloudflareId = new TextField("Cloudflare ID");
            var mapId = new TextField("Map ID");

            form.add(name, type, externalId, sortOrder, latitude, longitude, cloudflareId, mapId);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(name)
                  .asRequired()
                  .bind(Location::getName, Location::setName);

            binder.forField(type)
                  .asRequired()
                  .bind(l -> l.getType() != null ? INSTANCE.fromDb(l.getType()) : null,
                        (l, v) -> l.setType(v != null ? INSTANCE.toDb(v) : null));

            binder.forField(externalId)
                  .asRequired()
                  .bind(Location::getExternalId, Location::setExternalId);

            binder.forField(latitude)
                  .asRequired()
                  .bind(Location::getLatitude, Location::setLatitude);

            binder.forField(longitude)
                  .asRequired()
                  .bind(Location::getLongitude, Location::setLongitude);

            binder.forField(sortOrder)
                  .asRequired()
                  .bind(Location::getSortOrder, Location::setSortOrder);

            binder.forField(cloudflareId)
                  .bind(Location::getCloudflareId, Location::setCloudflareId);
            binder.forField(mapId)
                  .bind(Location::getMapId, Location::setMapId);

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

        public void open(Location location) {
            binder.setBean(location);
            super.open();
        }

        private void save() {
            if (binder.validate().isOk()) {
                var location = binder.getBean();
                if (location.getId() == null) {
                    locationDao.insert(location);
                } else {
                    locationDao.update(location);
                }
                close();
            }
        }

        private void delete() {
            var location = binder.getBean();
            if (location != null && location.getId() != null) {
                locationDao.delete(location);
                close();
            }
        }
    }
}
