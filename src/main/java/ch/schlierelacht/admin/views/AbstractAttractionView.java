package ch.schlierelacht.admin.views;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.daos.TagDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Attraction;
import ch.schlierelacht.admin.jooq.tables.pojos.Image;
import ch.schlierelacht.admin.jooq.tables.pojos.Tag;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static ch.schlierelacht.admin.dto.ImageType.ADDITIONAL;
import static ch.schlierelacht.admin.dto.ImageType.MAIN;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_IMAGE;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_TAG;
import static ch.schlierelacht.admin.jooq.tables.Image.IMAGE;
import static ch.schlierelacht.admin.jooq.tables.Tag.TAG;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public abstract class AbstractAttractionView extends VerticalLayout {

    private final AttractionDao attractionDao;
    private final TagDao tagDao;
    private final CloudflareService cloudflareService;
    private final DSLContext dslContext;
    private final Grid<Attraction> grid;
    private final AttractionDialog dialog;

    public AbstractAttractionView(AttractionDao attractionDao, TagDao tagDao, CloudflareService cloudflareService, DSLContext dslContext) {
        this.attractionDao = attractionDao;
        this.tagDao = tagDao;
        this.cloudflareService = cloudflareService;
        this.dslContext = dslContext;

        this.dialog = new AttractionDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });

        setSizeFull();
        add(new H2("%s verwalten".formatted(getAttractionType().getDescription())));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    protected abstract @NonNull AttractionType getAttractionType();

    private Grid<Attraction> createGrid() {
        var g = new Grid<Attraction>();
        g.addComponentColumn(a -> new Button(EDIT.create(), _ -> dialog.open(a)))
         .setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Attraction::getName).setHeader("Name").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("%s hinzufügen".formatted(getAttractionType().getDescription()),
                                   _ -> dialog.open(newAttraction()));
        return new HorizontalLayout(addButton);
    }

    private Attraction newAttraction() {
        var attraction = new Attraction();
        attraction.setType(getAttractionType().toDb());
        return attraction;
    }

    private void refreshGrid() {
        grid.setItems(attractionDao.fetchByType(getAttractionType().toDb()));
    }

    private class AttractionDialog extends Dialog {
        private final Binder<Attraction> binder = new Binder<>(Attraction.class);
        private final MultiSelectComboBox<Tag> tags = new MultiSelectComboBox<>("Tags");
        private final VerticalLayout imageInfoLayout = new VerticalLayout();
        private final TextField mainImageDescription = new TextField("Beschreibung Hauptbild");
        private final VerticalLayout additionalImagesLayout = new VerticalLayout();
        private final Map<String, TextField> additionalImagesDescription = new HashMap<>();
        private final Map<String, UploadMetadata> additionalImagesMetadata = new HashMap<>();
        private final Map<String, byte[]> additionalImagesData = new HashMap<>();
        private UploadMetadata mainImageMetadata;
        private byte[] mainImageData;

        public AttractionDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setHeaderTitle("%s bearbeiten".formatted(getAttractionType().getDescription()));
            setWidth("800px");

            var form = new FormLayout();
            var name = new TextField("Name");
            name.setMaxLength(255);

            var description = new TextArea("Beschreibung");
            description.setMinRows(8);

            var website = new TextField("Website");
            var instagram = new TextField("Instagram");
            var facebook = new TextField("Facebook");
            var youtube = new TextField("Youtube");
            var externalId = new TextField("External ID (z.B. 'dj-mario')");

            tags.setItems(tagDao.fetchByType(getAttractionType().toDb()));
            tags.setItemLabelGenerator(Tag::getName);
            tags.setWidthFull();

            form.add(name, externalId, website, instagram, facebook, youtube, tags, description);
            form.setColspan(description, 2);
            form.setColspan(tags, 2);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            binder.forField(name).asRequired().bind(Attraction::getName, Attraction::setName);
            binder.forField(description).bind(Attraction::getDescription, Attraction::setDescription);
            binder.forField(website).bind(Attraction::getWebsite, Attraction::setWebsite);
            binder.forField(instagram).bind(Attraction::getInstagram, Attraction::setInstagram);
            binder.forField(facebook).bind(Attraction::getFacebook, Attraction::setFacebook);
            binder.forField(youtube).bind(Attraction::getYoutube, Attraction::setYoutube);
            binder.forField(externalId).bind(Attraction::getExternalId, Attraction::setExternalId);

            mainImageDescription.setRequired(true);
            mainImageDescription.setWidthFull();

            var mainUploadHandler = UploadHandler.inMemory((metadata, data) -> {
                mainImageMetadata = metadata;
                mainImageData = data;
            });
            mainUploadHandler.whenComplete(success -> {
                if (success) {
                    mainImageDescription.setVisible(true);
                }
            });
            var mainUpload = new Upload(mainUploadHandler);
            mainUpload.addFileRemovedListener(_ -> {
                mainImageMetadata = null;
                mainImageData = null;
            });
            mainUpload.setAcceptedFileTypes("image/*");
            mainUpload.setMaxFiles(1);

            var additionalUploadHandler = UploadHandler.inMemory((metadata, data) -> {
                additionalImagesMetadata.put(metadata.fileName(), metadata);
                additionalImagesData.put(metadata.fileName(), data);
            });
            additionalUploadHandler.whenComplete((event, success) -> {
                if (success) {
                    var descField = new TextField("Beschreibung für " + event.fileName());
                    descField.setRequired(true);
                    descField.setWidthFull();
                    additionalImagesDescription.put(event.fileName(), descField);
                    additionalImagesLayout.add(descField);
                }
            });

            var additionalUpload = new Upload(additionalUploadHandler);
            additionalUpload.setAcceptedFileTypes("image/*");
            additionalUpload.addFileRemovedListener(event -> {
                additionalImagesMetadata.remove(event.getFileName());
                additionalImagesData.remove(event.getFileName());
                var textField = additionalImagesDescription.get(event.getFileName());
                additionalImagesLayout.remove(textField);
                additionalImagesDescription.remove(event.getFileName());
            });

            add(form,
                new H3("Hauptbild"), mainUpload, mainImageDescription,
                new Hr(),
                new H3("Weitere Bilder"), additionalUpload, additionalImagesLayout, imageInfoLayout);

            var save = new Button("Speichern", _ -> {
                if (saveAttraction()) {
                    onSuccessCallback.run();
                }
            });
            save.addThemeVariants(LUMO_PRIMARY);

            var delete = new Button("Löschen", _ -> {
                deleteAttraction();
                onSuccessCallback.run();
            });
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

            var cancel = new Button("Abbrechen", _ -> close());
            getFooter().add(delete, cancel, save);
        }

        public void open(Attraction attraction) {
            binder.setBean(attraction);
            imageInfoLayout.removeAll();
            additionalImagesLayout.removeAll();
            additionalImagesDescription.clear();
            additionalImagesMetadata.clear();
            additionalImagesData.clear();
            mainImageDescription.setValue("");
            mainImageDescription.setVisible(false);

            if (attraction.getId() != null) {
                // Show existing tags
                var existingTags = dslContext.select(TAG.asterisk())
                                             .from(TAG)
                                             .join(ATTRACTION_TAG).on(TAG.ID.eq(ATTRACTION_TAG.TAG_ID))
                                             .where(ATTRACTION_TAG.ATTRACTION_ID.eq(attraction.getId()))
                                             .fetchInto(Tag.class);
                tags.setValue(new HashSet<>(existingTags));

                // Show existing images
                var images = dslContext.select(IMAGE.ID, IMAGE.CLOUDFLARE_ID, ATTRACTION_IMAGE.TYPE, IMAGE.DESCRIPTION)
                                       .from(IMAGE)
                                       .join(ATTRACTION_IMAGE).on(IMAGE.ID.eq(ATTRACTION_IMAGE.IMAGE_ID))
                                       .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(attraction.getId()))
                                       .fetch();
                images.forEach(r -> {
                    var imageId = r.get(IMAGE.ID);
                    var cloudflareId = r.get(IMAGE.CLOUDFLARE_ID);
                    var type = ImageType.fromDb(r.get(ATTRACTION_IMAGE.TYPE)).orElseThrow();
                    var imgDesc = r.get(IMAGE.DESCRIPTION);

                    var img = new CloudflareImage(cloudflareService, cloudflareId, imgDesc);
                    img.setWidth("200px");

                    var label = new Span(type.getDescription() + " (" + imgDesc + "):");
                    var row = new HorizontalLayout();
                    row.setAlignItems(Alignment.CENTER);
                    row.setSpacing(true);

                    var deleteBtn = new Button("Löschen");
                    deleteBtn.addClickListener(_ -> {
                        // Delete relation and image, and from Cloudflare
                        try {
                            dslContext.deleteFrom(ATTRACTION_IMAGE)
                                      .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(binder.getBean().getId())
                                                                           .and(ATTRACTION_IMAGE.IMAGE_ID.eq(imageId)))
                                      .execute();

                            dslContext.deleteFrom(IMAGE)
                                      .where(IMAGE.ID.eq(imageId))
                                      .execute();

                            cloudflareService.delete(cloudflareId);

                            imageInfoLayout.remove(row);
                        } catch (Exception ex) {
                            log.error("Error deleting image {} for attraction {}", imageId, binder.getBean().getId(), ex);
                            showNotification("Bild konnte nicht gelöscht werden", LUMO_ERROR);
                        }
                    });
                    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

                    row.add(label, img, deleteBtn);
                    imageInfoLayout.add(row);
                });
            }
            super.open();
        }

        private boolean saveAttraction() {
            if (!binder.validate().isOk()) {
                showNotification("Alle erforderliche Felder ausfüllen.", LUMO_WARNING);
                return false;
            }

            var attraction = binder.getBean();
            boolean creating = attraction.getId() == null;

            if (creating && mainImageData == null) {
                showNotification("Hauptbild ist erforderlich", LUMO_ERROR);
                return false;
            }
            if (creating && isBlank(mainImageDescription.getValue())) {
                showNotification("Beschreibung für Hauptbild ist erforderlich", LUMO_ERROR);
                return false;
            }

            for (var additionalImage : additionalImagesMetadata.entrySet()) {
                var descField = additionalImagesDescription.get(additionalImage.getKey());
                if (descField == null || isBlank(descField.getValue())) {
                    showNotification("Beschreibung für " + additionalImage.getKey() + " ist erforderlich", LUMO_ERROR);
                    return false;
                }
            }

            if (creating) {
                var it = dslContext.newRecord(ATTRACTION, attraction);
                it.insert();
                attraction.setId(it.getId());
            } else {
                attractionDao.update(attraction);
            }

            if (mainImageData != null) {
                uploadAndLinkImage(attraction.getId(), mainImageData, mainImageMetadata.fileName(), mainImageMetadata.contentType(), MAIN, mainImageDescription.getValue());
            }

            for (var additionalImage : additionalImagesData.entrySet()) {
                var desc = additionalImagesDescription.get(additionalImage.getKey()).getValue();
                uploadAndLinkImage(attraction.getId(), additionalImage.getValue(), additionalImage.getKey(),
                                   additionalImagesMetadata.get(additionalImage.getKey()).contentType(), ADDITIONAL, desc);
            }

            // Save tags
            dslContext.deleteFrom(ATTRACTION_TAG)
                      .where(ATTRACTION_TAG.ATTRACTION_ID.eq(attraction.getId()))
                      .execute();

            for (Tag selectedTag : tags.getValue()) {
                dslContext.insertInto(ATTRACTION_TAG)
                          .set(ATTRACTION_TAG.ATTRACTION_ID, attraction.getId())
                          .set(ATTRACTION_TAG.TAG_ID, selectedTag.getId())
                          .execute();
            }

            close();
            return true;
        }

        private void uploadAndLinkImage(Long attractionId, byte[] data, String fileName, String mimeType, ImageType type, String description) {
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data));
                if (bi == null) {
                    showNotification("Ungültiges Bild: " + fileName, LUMO_ERROR);
                    return;
                }

                Optional<String> cloudflareId = cloudflareService.upload(data, fileName, mimeType, data.length, attractionId.toString(), "");

                if (cloudflareId.isPresent()) {
                    Image image = new Image();
                    image.setCloudflareId(cloudflareId.get());
                    image.setDescription(description);
                    image.setWidth(bi.getWidth());
                    image.setHeight(bi.getHeight());

                    var imageRecord = dslContext.newRecord(IMAGE, image);
                    imageRecord.insert();
                    image.setId(imageRecord.getId());

                    dslContext.insertInto(ATTRACTION_IMAGE)
                              .set(ATTRACTION_IMAGE.ATTRACTION_ID, attractionId)
                              .set(ATTRACTION_IMAGE.IMAGE_ID, image.getId())
                              .set(ATTRACTION_IMAGE.TYPE, type.toDb())
                              .execute();
                } else {
                    showNotification("Upload fehlgeschlagen für: " + fileName, LUMO_ERROR);
                }
            } catch (IOException e) {
                log.error("Error processing image upload", e);
                showNotification("Fehler beim Verarbeiten von: " + fileName, LUMO_ERROR);
            }
        }

        private void deleteAttraction() {
            var attraction = binder.getBean();
            if (attraction != null && attraction.getId() != null) {
                attractionDao.delete(attraction);
                close();
            }
        }
    }
}
