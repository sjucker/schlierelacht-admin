package ch.schlierelacht.admin.views.sponsoring;

import ch.schlierelacht.admin.dto.SponsoringType;
import ch.schlierelacht.admin.jooq.tables.daos.SponsoringDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Sponsoring;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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
import org.jspecify.annotations.NonNull;

import java.util.Comparator;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@PageTitle("Sponsoring")
@Route(value = "sponsoring", layout = MainLayout.class)
@PermitAll
public class SponsoringView extends VerticalLayout {

    private final SponsoringDao sponsoringDao;
    private final CloudflareService cloudflareService;
    private final Grid<Sponsoring> grid;
    private final SponsoringDialog dialog;

    public SponsoringView(SponsoringDao sponsoringDao, CloudflareService cloudflareService) {
        this.sponsoringDao = sponsoringDao;
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
        g.addComponentColumn(s -> new Button(EDIT.create(), _ -> dialog.open(s))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Sponsoring::getName).setHeader("Name").setSortable(true);
        g.addColumn(sponsoring -> SponsoringType.fromDb(sponsoring.getType()).map(SponsoringType::getDescription).orElse(null)).setHeader("Typ").setSortable(true);
        g.addColumn(Sponsoring::getUrl).setHeader("URL");
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addSponsoringButton = new Button("Eintrag hinzufügen", _ -> {
            Sponsoring sponsoring = new Sponsoring();
            dialog.open(sponsoring);
        });

        return new HorizontalLayout(addSponsoringButton);
    }

    private void refreshGrid() {
        grid.setItems(sponsoringDao.findAll().stream().sorted(Comparator.comparing(Sponsoring::getName)).toList());
    }

    private class SponsoringDialog extends Dialog {

        private final Binder<Sponsoring> binder = new Binder<>(Sponsoring.class);

        public SponsoringDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
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
            upload.setAcceptedFileTypes("image/*");
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
