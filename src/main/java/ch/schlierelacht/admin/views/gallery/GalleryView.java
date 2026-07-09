package ch.schlierelacht.admin.views.gallery;

import ch.schlierelacht.admin.jooq.tables.daos.GalleryImageDao;
import ch.schlierelacht.admin.jooq.tables.pojos.GalleryCategory;
import ch.schlierelacht.admin.jooq.tables.pojos.GalleryImage;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.service.GalleryService;
import ch.schlierelacht.admin.util.DateUtil;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.schlierelacht.admin.jooq.Tables.GALLERY_IMAGE;
import static ch.schlierelacht.admin.service.CloudflareService.MAX_IMAGE_SIZE_BYTES;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.ARROW_DOWN;
import static com.vaadin.flow.component.icon.VaadinIcon.ARROW_UP;
import static com.vaadin.flow.component.icon.VaadinIcon.TRASH;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@PageTitle("Galerie")
@Route(value = "gallery", layout = MainLayout.class)
@PermitAll
public class GalleryView extends VerticalLayout {

    private final GalleryImageDao galleryImageDao;
    private final DSLContext dslContext;
    private final CloudflareService cloudflareService;
    private final GalleryService galleryService;
    private final Grid<GalleryImage> grid;
    private final Grid<GalleryCategory> categoryGrid;
    private final UploadDialog dialog;

    public GalleryView(GalleryImageDao galleryImageDao, DSLContext dslContext, CloudflareService cloudflareService,
                       GalleryService galleryService) {
        this.galleryImageDao = galleryImageDao;
        this.dslContext = dslContext;
        this.cloudflareService = cloudflareService;
        this.galleryService = galleryService;
        this.dialog = new UploadDialog();
        setSizeFull();

        add(new H2("Galerie verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        add(new H2("Kategorien"));
        categoryGrid = createCategoryGrid();
        add(categoryGrid);

        refreshGrid();
        refreshCategoryGrid();
    }

    private Grid<GalleryCategory> createCategoryGrid() {
        var g = new Grid<GalleryCategory>();
        g.addColumn(GalleryCategory::getName).setHeader("Kategorie");
        g.addComponentColumn(cat -> {
            var up = new Button(ARROW_UP.create(), _ -> {
                galleryService.moveUp(cat.getId());
                refreshCategoryGrid();
            });
            var down = new Button(ARROW_DOWN.create(), _ -> {
                galleryService.moveDown(cat.getId());
                refreshCategoryGrid();
            });
            up.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            down.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(up, down);
        }).setHeader("Reihenfolge").setWidth("120px").setFlexGrow(0);
        g.setAllRowsVisible(true);
        return g;
    }

    private void refreshCategoryGrid() {
        categoryGrid.setItems(galleryService.listCategoriesOrdered());
    }

    private Grid<GalleryImage> createGrid() {
        var g = new Grid<GalleryImage>();
        g.addComponentColumn(img -> {
            var thumb = new CloudflareImage(cloudflareService, img.getCloudflareId(), img.getCategory());
            thumb.setWidth("120px");
            return thumb;
        }).setHeader("Bild").setWidth("140px").setFlexGrow(0);
        g.addColumn(GalleryImage::getCategory).setHeader("Kategorie").setSortable(true);
        g.addColumn(img -> DateUtil.formatDateTime(img.getUploadedAt()))
         .setHeader("Hochgeladen")
         .setComparator(GalleryImage::getUploadedAt)
         .setSortable(true);
        g.addColumn(GalleryImage::getUploadedBy).setHeader("Hochgeladen von").setSortable(true);
        g.addComponentColumn(img -> {
            var delete = new Button(TRASH.create(), _ -> deleteImage(img));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return delete;
        }).setHeader("").setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        return g;
    }

    private HorizontalLayout createToolbar() {
        return new HorizontalLayout(new Button("Bilder hochladen", _ -> dialog.open()));
    }

    private void refreshGrid() {
        // Newest first.
        grid.setItems(galleryImageDao.findAll().stream()
                                     .sorted(Comparator.comparing(GalleryImage::getUploadedAt, nullsFirst(naturalOrder()))
                                                       .reversed())
                                     .toList());
    }

    private void deleteImage(GalleryImage img) {
        try {
            cloudflareService.delete(img.getCloudflareId());
            galleryImageDao.deleteById(img.getId());
            refreshGrid();
            showNotification("Bild gelöscht", LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error deleting gallery image {}", img.getId(), e);
            showNotification("Bild konnte nicht gelöscht werden", LUMO_ERROR);
        }
    }

    private List<String> loadCategories() {
        return dslContext.selectDistinct(GALLERY_IMAGE.CATEGORY)
                         .from(GALLERY_IMAGE)
                         .orderBy(GALLERY_IMAGE.CATEGORY.asc())
                         .fetch(GALLERY_IMAGE.CATEGORY);
    }

    private class UploadDialog extends Dialog {

        // Uploaded files buffered in memory until the user saves.
        private final ComboBox<String> category = new ComboBox<>("Kategorie");
        private final Map<String, byte[]> filesData = new LinkedHashMap<>();
        private final Map<String, UploadMetadata> filesMeta = new LinkedHashMap<>();

        public UploadDialog() {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Bilder hochladen");
            setWidth("600px");

            category.setRequired(true);
            category.setWidthFull();
            category.setHelperText("Bestehende Kategorie wählen oder neue eingeben");
            // Free-text: typing a new category and confirming selects it as the value.
            category.setAllowCustomValue(true);
            category.addCustomValueSetListener(e -> category.setValue(e.getDetail()));

            var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
                filesData.put(metadata.fileName(), data);
                filesMeta.put(metadata.fileName(), metadata);
            });
            uploadHandler.whenComplete(success -> {
                if (!success) {
                    showNotification("Upload fehlgeschlagen", LUMO_ERROR);
                }
            });
            var upload = new Upload(uploadHandler);
            upload.setAcceptedMimeTypes("image/*");
            upload.setMaxFileSize((int) MAX_IMAGE_SIZE_BYTES);
            upload.addFileRemovedListener(e -> {
                filesData.remove(e.getFileName());
                filesMeta.remove(e.getFileName());
            });

            var maxSizeHint = new Span("Maximale Dateigrösse: " + (MAX_IMAGE_SIZE_BYTES / (1024 * 1024)) + " MB");
            maxSizeHint.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

            var layout = new VerticalLayout(category, upload, maxSizeHint);
            layout.setPadding(false);
            add(layout);

            var save = new Button("Speichern");
            save.addClickListener(_ -> {
                save();
                save.setEnabled(true);
            });
            save.setDisableOnClick(true);
            save.addThemeVariants(LUMO_PRIMARY);

            var cancel = new Button("Abbrechen", _ -> close());
            getFooter().add(cancel, save);
        }

        public void open() {
            category.setItems(loadCategories());
            category.clear();
            filesData.clear();
            filesMeta.clear();
            super.open();
        }

        private void save() {
            var selectedCategory = category.getValue();
            if (isBlank(selectedCategory)) {
                showNotification("Kategorie ist erforderlich", LUMO_WARNING);
                return;
            }
            if (filesData.isEmpty()) {
                showNotification("Mindestens ein Bild hochladen", LUMO_WARNING);
                return;
            }

            var trimmedCategory = selectedCategory.trim();
            var uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();

            // A single failing image must not abort the whole batch: upload each independently, collect failures.
            var failed = new ArrayList<String>();
            int uploaded = 0;
            for (var entry : filesData.entrySet()) {
                var fileName = entry.getKey();
                var data = entry.getValue();
                var meta = filesMeta.get(fileName);
                if (data.length > MAX_IMAGE_SIZE_BYTES) {
                    log.warn("Skipping gallery image {} ({} bytes): exceeds max size of {} bytes",
                             fileName, data.length, MAX_IMAGE_SIZE_BYTES);
                    failed.add(fileName);
                    continue;
                }
                Optional<String> cloudflareId;
                try {
                    cloudflareId = cloudflareService.upload(data, fileName, meta.contentType(), meta.contentLength(),
                                                            "gallery", "GalleryView");
                } catch (Exception e) {
                    log.error("Error uploading gallery image {} to Cloudflare", fileName, e);
                    cloudflareId = Optional.empty();
                }
                if (cloudflareId.isEmpty()) {
                    failed.add(fileName);
                    continue;
                }
                var image = new GalleryImage();
                image.setCategory(trimmedCategory);
                image.setCloudflareId(cloudflareId.get());
                image.setUploadedAt(DateUtil.now());
                image.setUploadedBy(uploadedBy);
                galleryImageDao.insert(image);
                uploaded++;
            }

            if (uploaded > 0) {
                galleryService.ensureCategory(trimmedCategory);
                refreshGrid();
                refreshCategoryGrid();
            }

            if (failed.isEmpty()) {
                showNotification("Speichern erfolgreich", LUMO_SUCCESS);
                close();
            } else if (uploaded > 0) {
                showNotification("%d von %d Bildern hochgeladen. Fehlgeschlagen: %s"
                                         .formatted(uploaded, uploaded + failed.size(), String.join(", ", failed)),
                                 LUMO_WARNING);
                close();
            } else {
                // Everything failed – keep the dialog open so the user can retry without re-selecting files.
                showNotification("Upload fehlgeschlagen für: " + String.join(", ", failed), LUMO_ERROR);
            }
        }
    }
}
