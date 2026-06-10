package ch.schlierelacht.admin.views.downloads;

import ch.schlierelacht.admin.dto.DownloadCategory;
import ch.schlierelacht.admin.dto.DownloadDTO;
import ch.schlierelacht.admin.service.DownloadService;
import ch.schlierelacht.admin.util.DateUtil;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

@PageTitle("Downloads")
@Route(value = "downloads", layout = MainLayout.class)
@PermitAll
public class DownloadsView extends VerticalLayout {

    private final DownloadService downloadService;
    private final Grid<DownloadDTO> grid;
    private final DownloadDialog dialog;

    public DownloadsView(DownloadService downloadService) {
        this.downloadService = downloadService;
        this.dialog = new DownloadDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("Downloads verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<DownloadDTO> createGrid() {
        var g = new Grid<DownloadDTO>();
        g.addComponentColumn(d -> new Button(EDIT.create(), _ -> dialog.open(d))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(d -> d.category().getDescription()).setHeader("Kategorie").setSortable(true);
        g.addColumn(DownloadDTO::description).setHeader("Bezeichnung").setSortable(true);
        g.addColumn(DownloadDTO::filename).setHeader("Dateiname").setSortable(true);
        g.addColumn(DownloadDTO::filetype).setHeader("Typ").setSortable(true);
        g.addColumn(d -> formatFileSize(d.filesize())).setHeader("Grösse").setSortable(true);
        g.addColumn(d -> DateUtil.formatDateTime(d.uploadedAt())).setHeader("Hochgeladen am").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("Eintrag hinzufügen", _ -> dialog.open(null));
        return new HorizontalLayout(addButton);
    }

    private void refreshGrid() {
        grid.setItems(downloadService.findAll());
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private class DownloadDialog extends Dialog {

        private final Runnable onSuccessCallback;
        private DownloadDTO current;

        private byte[] uploadedData;
        private String uploadedFilename;
        private String uploadedFiletype;
        private long uploadedFilesize;

        private final Select<DownloadCategory> categorySelect = new Select<>();
        private final TextField descriptionField = new TextField("Bezeichnung");
        private final Button save = new Button("Speichern");

        public DownloadDialog(@NonNull Runnable onSuccessCallback) {
            this.onSuccessCallback = onSuccessCallback;
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Download bearbeiten");
            setWidth("500px");

            categorySelect.setLabel("Kategorie");
            categorySelect.setItems(DownloadCategory.values());
            categorySelect.setItemLabelGenerator(DownloadCategory::getDescription);

            var upload = createUpload();

            var form = new FormLayout();
            form.add(categorySelect, descriptionField, upload);
            form.setColspan(descriptionField, 2);
            form.setColspan(upload, 2);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("400px", 2));
            add(form);

            save.addClickListener(_ -> {
                if (save()) {
                    onSuccessCallback.run();
                } else {
                    save.setEnabled(true);
                }
            });
            save.setDisableOnClick(true);
            save.addThemeVariants(LUMO_PRIMARY);

            var delete = new Button("Löschen", _ -> delete());
            delete.addThemeVariants(LUMO_ERROR);

            var cancel = new Button("Abbrechen", _ -> close());

            getFooter().add(delete, cancel, save);
        }

        private Upload createUpload() {
            var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
                uploadedData = data;
                uploadedFilename = metadata.fileName();
                uploadedFiletype = metadata.contentType();
                uploadedFilesize = metadata.contentLength();
            });
            uploadHandler.whenComplete(success -> {
                if (!success) {
                    showNotification("Upload fehlgeschlagen", com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                }
            });
            var upload = new Upload(uploadHandler);
            upload.setMaxFiles(1);
            return upload;
        }

        public void open(DownloadDTO download) {
            current = download;
            uploadedData = null;
            uploadedFilename = null;
            uploadedFiletype = null;
            uploadedFilesize = 0;
            save.setEnabled(true);

            if (download != null) {
                categorySelect.setValue(download.category());
                descriptionField.setValue(download.description());
            } else {
                categorySelect.clear();
                descriptionField.clear();
            }
            super.open();
        }

        private boolean save() {
            if (categorySelect.isEmpty() || descriptionField.getValue().isBlank()) {
                showNotification("Bitte Kategorie und Bezeichnung ausfüllen", com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                return false;
            }
            if (current == null && uploadedData == null) {
                showNotification("Bitte eine Datei hochladen", com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                return false;
            }

            if (current == null) {
                var uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();
                downloadService.create(
                        categorySelect.getValue(),
                        descriptionField.getValue(),
                        uploadedFilename,
                        uploadedFiletype,
                        uploadedFilesize,
                        uploadedData,
                        uploadedBy
                );
            } else {
                downloadService.updateMetadata(current.id(), categorySelect.getValue(), descriptionField.getValue());
            }
            close();
            return true;
        }

        private void delete() {
            if (current != null) {
                downloadService.delete(current.id());
                close();
                onSuccessCallback.run();
            }
        }
    }
}
