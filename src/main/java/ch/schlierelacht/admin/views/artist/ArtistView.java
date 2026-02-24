package ch.schlierelacht.admin.views.artist;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Attraction;
import ch.schlierelacht.admin.jooq.tables.pojos.Image;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static ch.schlierelacht.admin.dto.ImageType.ADDITIONAL;
import static ch.schlierelacht.admin.dto.ImageType.MAIN;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_IMAGE;
import static ch.schlierelacht.admin.jooq.tables.Image.IMAGE;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@PageTitle("Künstler")
@Route(value = "artists", layout = MainLayout.class)
@PermitAll
public class ArtistView extends VerticalLayout {

    private final AttractionDao attractionDao;
    private final CloudflareService cloudflareService;
    private final DSLContext dslContext;

    private final Grid<Attraction> grid;
    private final ArtistDialog dialog;

    public ArtistView(AttractionDao attractionDao, CloudflareService cloudflareService, DSLContext dslContext) {
        this.attractionDao = attractionDao;
        this.cloudflareService = cloudflareService;
        this.dslContext = dslContext;

        this.dialog = new ArtistDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });

        setSizeFull();
        add(new H2("Künstler verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Attraction> createGrid() {
        var g = new Grid<Attraction>();
        g.addComponentColumn(a -> new Button(EDIT.create(), _ -> dialog.open(a)))
         .setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Attraction::getName).setHeader("Name").setSortable(true);
        g.addColumn(Attraction::getWebsite).setHeader("Website");
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addArtistButton = new Button("Künstler hinzufügen", _ -> dialog.open(newArtist()));
        return new HorizontalLayout(addArtistButton);
    }

    private static Attraction newArtist() {
        var attraction = new Attraction();
        attraction.setType(AttractionType.ARTIST.toDb());
        return attraction;
    }

    private void refreshGrid() {
        grid.setItems(attractionDao.findAll());
    }

    private class ArtistDialog extends Dialog {
        private final Binder<Attraction> binder = new Binder<>(Attraction.class);
        private final VerticalLayout imageInfoLayout = new VerticalLayout();
        private final TextField mainImageDescription = new TextField("Beschreibung Hauptbild");
        private final VerticalLayout additionalImagesLayout = new VerticalLayout();
        private final Map<String, TextField> additionalImagesDescription = new HashMap<>();
        private final Map<String, UploadMetadata> additionalImagesMetadata = new HashMap<>();
        private final Map<String, byte[]> additionalImagesData = new HashMap<>();
        private UploadMetadata mainImageMetadata;
        private byte[] mainImageData;

        public ArtistDialog(Runnable onSuccessCallback) {
            setHeaderTitle("Künstler bearbeiten");
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
            var externalId = new TextField("External ID (z.B. 'dj-mario'");

            form.add(name, externalId, website, instagram, facebook, youtube, description);
            form.setColspan(description, 2);
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
                if (saveArtist()) {
                    onSuccessCallback.run();
                }
            });
            save.addThemeVariants(LUMO_PRIMARY);

            var delete = new Button("Löschen", _ -> {
                deleteArtist();
                onSuccessCallback.run();
            });
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

            var cancel = new Button("Abbrechen", _ -> close());
            getFooter().add(delete, cancel, save);
        }

        public void open(Attraction artist) {
            binder.setBean(artist);
            imageInfoLayout.removeAll();
            additionalImagesLayout.removeAll();
            additionalImagesDescription.clear();
            additionalImagesMetadata.clear();
            additionalImagesData.clear();
            mainImageDescription.setValue("");
            mainImageDescription.setVisible(false);

            if (artist.getId() != null) {
                // Show existing images
                var images = dslContext.select(IMAGE.CLOUDFLARE_ID, ATTRACTION_IMAGE.TYPE, IMAGE.DESCRIPTION)
                                       .from(IMAGE)
                                       .join(ATTRACTION_IMAGE).on(IMAGE.ID.eq(ATTRACTION_IMAGE.IMAGE_ID))
                                       .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(artist.getId()))
                                       .fetch();
                images.forEach(r -> {
                    var cloudflareId = r.get(IMAGE.CLOUDFLARE_ID);
                    var type = ImageType.fromDb(r.get(ATTRACTION_IMAGE.TYPE)).orElseThrow();
                    var imgDesc = r.get(IMAGE.DESCRIPTION);
                    var img = new CloudflareImage(cloudflareService, cloudflareId, imgDesc);
                    img.setWidth("100px");
                    imageInfoLayout.add(new HorizontalLayout(new Span(type.getDescription() + " (" + imgDesc + "):"), img));
                });
            }
            super.open();
        }

        private boolean saveArtist() {
            if (!binder.validate().isOk()) {
                showNotification("Alle erforderliche Felder ausfüllen.", LUMO_WARNING);
                return false;
            }

            var artist = binder.getBean();
            boolean creating = artist.getId() == null;

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
                var it = dslContext.newRecord(ATTRACTION, artist);
                it.insert();
                artist.setId(it.getId());
            } else {
                attractionDao.update(artist);
            }

            if (mainImageData != null) {
                uploadAndLinkImage(artist.getId(), mainImageData, mainImageMetadata.fileName(), mainImageMetadata.contentType(), MAIN, mainImageDescription.getValue());
            }

            for (var additionalImage : additionalImagesData.entrySet()) {
                var desc = additionalImagesDescription.get(additionalImage.getKey()).getValue();
                uploadAndLinkImage(artist.getId(), additionalImage.getValue(), additionalImage.getKey(),
                                   additionalImagesMetadata.get(additionalImage.getKey()).contentType(), ADDITIONAL, desc);
            }

            close();
            return true;
        }

        private void uploadAndLinkImage(Long artistId, byte[] data, String fileName, String mimeType, ImageType type, String description) {
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data));
                if (bi == null) {
                    showNotification("Ungültiges Bild: " + fileName, LUMO_ERROR);
                    return;
                }

                Optional<String> cloudflareId = cloudflareService.upload(data, fileName, mimeType, data.length, artistId.toString(), "");

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
                              .set(ATTRACTION_IMAGE.ATTRACTION_ID, artistId)
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

        private void deleteArtist() {
            var artist = binder.getBean();
            if (artist != null && artist.getId() != null) {
                // jOOQ should handle cascade delete for artist_image, but we might want to delete images from cloudflare too
                // For now, simple delete
                attractionDao.delete(artist);
                close();
            }
        }
    }
}
