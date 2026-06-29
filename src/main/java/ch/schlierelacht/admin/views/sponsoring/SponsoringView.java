package ch.schlierelacht.admin.views.sponsoring;

import ch.schlierelacht.admin.dto.SponsoringType;
import ch.schlierelacht.admin.jooq.tables.daos.SponsoringDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Sponsoring;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.service.SponsoringService;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropEvent;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.ARROWS_LONG_V;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@PageTitle("Sponsoring")
@Route(value = "sponsoring", layout = MainLayout.class)
@PermitAll
public class SponsoringView extends VerticalLayout {

    private final SponsoringDao sponsoringDao;
    private final SponsoringService sponsoringService;
    private final CloudflareService cloudflareService;
    private final Grid<Sponsoring> grid;
    private final SponsoringDialog dialog;
    private Sponsoring draggedItem;

    public SponsoringView(SponsoringDao sponsoringDao, SponsoringService sponsoringService, CloudflareService cloudflareService) {
        this.sponsoringDao = sponsoringDao;
        this.sponsoringService = sponsoringService;
        this.cloudflareService = cloudflareService;
        this.dialog = new SponsoringDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("Sponsoring verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Sponsoring> createGrid() {
        var g = new Grid<Sponsoring>();
        g.addComponentColumn(_ -> {
            var handle = ARROWS_LONG_V.create();
            handle.setSize("16px");
            handle.getStyle().set("cursor", "grab").set("color", "var(--lumo-secondary-text-color)");
            handle.setTooltipText("Zum Sortieren ziehen");
            return handle;
        }).setHeader("").setWidth("50px").setTextAlign(CENTER).setFlexGrow(0);
        g.addComponentColumn(s -> new Button(EDIT.create(), _ -> dialog.open(s))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Sponsoring::getName).setHeader("Name");
        g.addColumn(sponsoring -> SponsoringType.fromDb(sponsoring.getType()).map(SponsoringType::getDescription).orElse(null)).setHeader("Typ");
        g.addColumn(Sponsoring::getUrl).setHeader("URL");
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });

        g.setRowsDraggable(true);
        g.setDropMode(GridDropMode.BETWEEN);
        g.addDragStartListener(event -> draggedItem = event.getDraggedItems().getFirst());
        g.addDragEndListener(_ -> draggedItem = null);
        g.addDropListener(this::onDrop);
        return g;
    }

    private void onDrop(GridDropEvent<Sponsoring> event) {
        var target = event.getDropTargetItem().orElse(null);
        if (draggedItem == null || target == null || target.equals(draggedItem)) {
            return;
        }

        if (!Objects.equals(draggedItem.getType(), target.getType())) {
            showNotification("Sponsoren können nur innerhalb desselben Typs sortiert werden.", LUMO_ERROR);
            return;
        }

        var type = draggedItem.getType();
        List<Sponsoring> ordered = new ArrayList<>(sponsoringDao.fetchByType(type).stream()
                                                                .sorted(Comparator.comparing(Sponsoring::getSortOrder).thenComparing(Sponsoring::getName))
                                                                .toList());

        ordered.removeIf(s -> s.getId().equals(draggedItem.getId()));
        int targetIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(target.getId())) {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex == -1) {
            return;
        }
        int insertIndex = event.getDropLocation() == GridDropLocation.BELOW ? targetIndex + 1 : targetIndex;
        ordered.add(insertIndex, draggedItem);

        sponsoringService.reorder(SponsoringType.fromDb(type).orElseThrow(), ordered.stream().map(Sponsoring::getId).toList());
        refreshGrid();
    }

    private HorizontalLayout createToolbar() {
        var addSponsoringButton = new Button("Eintrag hinzufügen", _ -> dialog.open(new Sponsoring()));

        return new HorizontalLayout(addSponsoringButton);
    }

    private int nextSortOrder(ch.schlierelacht.admin.jooq.enums.SponsoringType type) {
        return sponsoringDao.fetchByType(type).stream()
                            .map(Sponsoring::getSortOrder)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .map(max -> max + 1)
                            .orElse(0);
    }

    private void refreshGrid() {
        grid.setItems(sponsoringDao.findAll().stream()
                                   .sorted(Comparator.comparing(Sponsoring::getType)
                                                     .thenComparing(Sponsoring::getSortOrder)
                                                     .thenComparing(Sponsoring::getName))
                                   .toList());
    }

    private class SponsoringDialog extends Dialog {

        private final Binder<Sponsoring> binder = new Binder<>(Sponsoring.class);

        public SponsoringDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Sponsoring Eintrag bearbeiten");

            var form = new FormLayout();

            var type = new ComboBox<SponsoringType>("Typ");
            type.setItems(SponsoringType.values());
            type.setItemLabelGenerator(SponsoringType::getDescription);

            var name = new TextField("Name");
            var url = new TextField("URL");
            var cloudflareId = new TextField("Cloudflare ID");
            cloudflareId.setReadOnly(true);

            var upload = getUpload(cloudflareId);

            var imagePreview = new VerticalLayout();
            imagePreview.setPadding(false);
            imagePreview.setSpacing(false);

            form.add(name, type, url, cloudflareId, upload, imagePreview);
            form.setColspan(imagePreview, 2);
            form.setColspan(upload, 2);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(name)
                  .asRequired()
                  .bind(Sponsoring::getName, Sponsoring::setName);

            binder.forField(type)
                  .asRequired()
                  .bind(sponsoring -> SponsoringType.fromDb(sponsoring.getType()).orElse(null),
                        (t, v) -> t.setType(v != null ? v.toDb() : null));

            binder.forField(url)
                  .bind(Sponsoring::getUrl, Sponsoring::setUrl);

            binder.forField(cloudflareId)
                  .bind(Sponsoring::getCloudflareId, Sponsoring::setCloudflareId);

            cloudflareId.addValueChangeListener(e -> {
                imagePreview.removeAll();
                if (isNotBlank(e.getValue())) {
                    var img = new CloudflareImage(cloudflareService, e.getValue(), "Vorschau");
                    img.setWidth("200px");
                    imagePreview.add(img);
                }
            });

            var save = new Button("Speichern");
            save.addClickListener(_ -> {
                if (save()) {
                    onSuccessCallback.run();
                } else {
                    save.setEnabled(true);
                }
            });
            save.setDisableOnClick(true);
            save.addThemeVariants(LUMO_PRIMARY);

            binder.addStatusChangeListener(_ -> save.setEnabled(binder.isValid()));

            var delete = new Button("Löschen", _ -> {
                delete();
                onSuccessCallback.run();
            });
            delete.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);

            var cancel = new Button("Abbrechen");
            cancel.addClickListener(_ -> close());

            getFooter().add(delete, cancel, save);
        }

        private Upload getUpload(TextField cloudflareId) {
            var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
                var sponsoring = binder.getBean();
                var id = sponsoring.getId() == null ? "new" : sponsoring.getId().toString();
                var resultId = cloudflareService.upload(data, metadata.fileName(), metadata.contentType(), metadata.contentLength(), id, "SponsoringView");
                resultId.ifPresent(cloudflareId::setValue);
            });
            uploadHandler.whenComplete(success -> {
                if (!success) {
                    showNotification("Upload fehlgeschlagen", com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                }
            });
            var upload = new Upload(uploadHandler);
            upload.setAcceptedMimeTypes("image/*");
            upload.setMaxFiles(1);
            return upload;
        }

        public void open(Sponsoring sponsoring) {
            binder.setBean(sponsoring);
            super.open();
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var sponsoring = binder.getBean();
                if (sponsoring.getId() == null) {
                    sponsoring.setSortOrder(nextSortOrder(sponsoring.getType()));
                    sponsoringDao.insert(sponsoring);
                } else {
                    sponsoringDao.update(sponsoring);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var sponsoring = binder.getBean();
            if (sponsoring != null && sponsoring.getId() != null) {
                sponsoringDao.delete(sponsoring);
                close();
            }
        }
    }
}
