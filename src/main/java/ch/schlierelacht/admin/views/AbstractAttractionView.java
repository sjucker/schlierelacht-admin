package ch.schlierelacht.admin.views;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.ImageType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Attraction;
import ch.schlierelacht.admin.jooq.tables.pojos.Image;
import ch.schlierelacht.admin.service.AttractionFileService;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ch.schlierelacht.admin.dto.AttractionType.FOOD;
import static ch.schlierelacht.admin.dto.ImageType.ADDITIONAL;
import static ch.schlierelacht.admin.dto.ImageType.MAIN;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION;
import static ch.schlierelacht.admin.jooq.Tables.ATTRACTION_IMAGE;
import static ch.schlierelacht.admin.jooq.tables.Image.IMAGE;
import static ch.schlierelacht.admin.service.CloudflareService.MAX_IMAGE_SIZE_BYTES;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public abstract class AbstractAttractionView extends VerticalLayout {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_FILE_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    private final AttractionDao attractionDao;
    private final CloudflareService cloudflareService;
    private final DSLContext dslContext;
    private final AttractionFileService attractionFileService;
    private Grid<Attraction> grid;
    private AttractionDialog dialog;

    public AbstractAttractionView(AttractionDao attractionDao, CloudflareService cloudflareService,
                                  DSLContext dslContext, AttractionFileService attractionFileService) {
        this.attractionDao = attractionDao;
        this.cloudflareService = cloudflareService;
        this.dslContext = dslContext;
        this.attractionFileService = attractionFileService;

        setSizeFull();
    }

    /**
     * The UI (dialog, grid, toolbar) is built on first attach rather than in the constructor so that any
     * subclass state the extension hooks rely on (its own fields and injected collaborators) is fully
     * initialized by then — subclass field/constructor assignments run only after {@code super(...)} returns.
     */
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (dialog != null) {
            return;
        }

        this.dialog = new AttractionDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });

        add(new H2("%s verwalten".formatted(getViewLabel())));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    /**
     * The attraction type(s) this view manages. Single-type views (e.g. artists, food) return one type; a multi-type
     * view offers a type selector in the dialog and a type column in the grid.
     */
    protected abstract @NonNull Set<AttractionType> getAttractionTypes();

    /**
     * Label used in the H2 heading, toolbar button and dialog header. Defaults to the single managed type's description
     * (preserving the existing single-type wording); multi-type views must override it.
     */
    protected @NonNull String getViewLabel() {
        var types = getAttractionTypes();
        if (types.size() == 1) {
            return types.iterator().next().getDescription();
        }
        throw new IllegalStateException("Views managing multiple attraction types must override getViewLabel()");
    }

    /**
     * Whether the type is picked per attraction (type ComboBox in the dialog, type column in the grid). Defaults to
     * true when more than one type is managed.
     */
    protected boolean isTypeSelectable() {
        return getAttractionTypes().size() > 1;
    }

    /**
     * Whether the operator ("Betreiber") field/column applies. It is only meaningful for {@link AttractionType#FOOD},
     * so it shows exactly for views that manage that type (the dialog additionally hides it unless FOOD is selected).
     */
    protected boolean isOperatorApplicable() {
        return getAttractionTypes().contains(FOOD);
    }

    // --- Extension hooks --------------------------------------------------------------------------
    // No-ops by default so the single-type/generic views (artist, food, attractions) are unaffected.
    // A subclass can attach data that lives outside the ATTRACTION row itself — e.g. the Wirtschaft
    // view edits an attraction together with its single programm entry (location + date/time).

    /**
     * Add extra columns to the grid (rendered after Name/Typ/Betreiber).
     */
    protected void addExtraColumns(Grid<Attraction> grid) {
    }

    /**
     * Prepare any lookup data the extra grid columns need, before the grid items are set.
     */
    protected void prepareExtraColumnData(List<Attraction> attractions) {
    }

    /**
     * Add extra fields to the dialog form (inserted before the description/markdown area).
     */
    protected void addExtraFormFields(FormLayout form) {
    }

    /**
     * Load the extra fields from the given attraction when the dialog opens (or clear for a new one).
     */
    protected void loadExtraFields(Attraction attraction) {
    }

    /**
     * Validate the extra fields on save; return {@code false} (after notifying) to abort the save.
     */
    protected boolean validateExtraFields() {
        return true;
    }

    /**
     * Persist the extra fields after the attraction has been inserted/updated.
     */
    protected void saveExtraFields(Long attractionId) {
    }

    /**
     * Remove dependent data before the attraction is deleted (e.g. its programm entries).
     */
    protected void deleteExtraFields(Long attractionId) {
    }

    private Grid<Attraction> createGrid() {
        var g = new Grid<Attraction>();
        g.addComponentColumn(a -> new Button(EDIT.create(), _ -> dialog.open(a)))
         .setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Attraction::getName).setHeader("Name").setSortable(true);
        if (isTypeSelectable()) {
            g.addColumn(a -> AttractionType.fromDb(a.getType()).map(AttractionType::getDescription).orElse(""))
             .setHeader("Typ").setSortable(true);
        }
        if (isOperatorApplicable()) {
            g.addColumn(Attraction::getOperator).setHeader("Betreiber").setSortable(true);
        }
        addExtraColumns(g);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("%s hinzufügen".formatted(getViewLabel()),
                                   _ -> dialog.open(newAttraction()));
        return new HorizontalLayout(addButton);
    }

    private Attraction newAttraction() {
        var attraction = new Attraction();
        if (!isTypeSelectable()) {
            attraction.setType(getAttractionTypes().iterator().next().toDb());
        }
        return attraction;
    }

    private void refreshGrid() {
        var types = getAttractionTypes().stream()
                                        .map(AttractionType::toDb)
                                        .toArray(ch.schlierelacht.admin.jooq.enums.AttractionType[]::new);
        var attractions = attractionDao.fetchByType(types).stream()
                                       .sorted(Comparator.comparing(Attraction::getExternalId,
                                                                    nullsLast(naturalOrder())))
                                       .toList();
        prepareExtraColumnData(attractions);
        grid.setItems(attractions);
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private class AttractionDialog extends Dialog {
        private final Binder<Attraction> binder = new Binder<>(Attraction.class);
        private final ComboBox<AttractionType> typeSelect = isTypeSelectable() ? new ComboBox<>("Typ") : null;
        private final TextField operator = new TextField("Betreiber");
        private final VerticalLayout imageInfoLayout = new VerticalLayout();
        private final TextField mainImageDescription = new TextField("Beschreibung Hauptbild");
        private final VerticalLayout additionalImagesLayout = new VerticalLayout();
        private final Map<String, TextField> additionalImagesDescription = new HashMap<>();
        private final Map<String, UploadMetadata> additionalImagesMetadata = new HashMap<>();
        private final Map<String, byte[]> additionalImagesData = new HashMap<>();
        private final VerticalLayout fileInfoLayout = new VerticalLayout();
        private final TextField fileDescription = new TextField("Beschreibung Datei");
        private final Div preview = new Div();
        private UploadMetadata mainImageMetadata;
        private byte[] mainImageData;
        private UploadMetadata fileMetadata;
        private byte[] fileData;

        public AttractionDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("%s bearbeiten".formatted(getViewLabel()));
            setWidth("800px");

            var form = new FormLayout();

            if (typeSelect != null) {
                typeSelect.setItems(getAttractionTypes());
                typeSelect.setItemLabelGenerator(AttractionType::getDescription);
                typeSelect.setRequired(true);
                typeSelect.setWidthFull();
                // The operator field only applies to FOOD, so toggle it as the selected type changes.
                if (isOperatorApplicable()) {
                    typeSelect.addValueChangeListener(event -> operator.setVisible(event.getValue() == FOOD));
                }
            }

            var name = new TextField("Name");
            name.setMaxLength(255);

            var description = new TextArea("Beschreibung");
            description.setMinRows(8);
            description.setWidthFull();
            description.setValueChangeMode(ValueChangeMode.EAGER);

            preview.setWidthFull();
            description.addValueChangeListener(event -> updatePreview(event.getValue()));

            var previewLabel = new Span("Markdown Vorschau:");
            var previewLayout = new VerticalLayout(previewLabel, preview);
            previewLayout.setPadding(false);
            previewLayout.setSpacing(false);

            var website = new TextField("Website");
            var instagram = new TextField("Instagram");
            var facebook = new TextField("Facebook");
            var youtube = new TextField("Youtube");
            var externalId = new TextField("External ID (z.B. 'dj-mario')");

            operator.setMaxLength(255);

            if (typeSelect != null) {
                form.add(typeSelect);
                form.setColspan(typeSelect, 2);
            }
            form.add(name, externalId, website, instagram, facebook, youtube);
            if (isOperatorApplicable()) {
                form.add(operator);
            }
            addExtraFormFields(form);
            form.add(description, previewLayout);
            form.setColspan(description, 2);
            form.setColspan(previewLayout, 2);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            if (typeSelect != null) {
                binder.forField(typeSelect).asRequired()
                      .bind(a -> AttractionType.fromDb(a.getType()).orElse(null),
                            (a, t) -> {
                                if (t != null) {
                                    a.setType(t.toDb());
                                }
                            });
            }
            binder.forField(name).asRequired().bind(Attraction::getName, Attraction::setName);
            binder.forField(description).bind(Attraction::getDescription, Attraction::setDescription);
            binder.forField(website).bind(Attraction::getWebsite, Attraction::setWebsite);
            binder.forField(instagram).bind(Attraction::getInstagram, Attraction::setInstagram);
            binder.forField(facebook).bind(Attraction::getFacebook, Attraction::setFacebook);
            binder.forField(youtube).bind(Attraction::getYoutube, Attraction::setYoutube);
            binder.forField(externalId).bind(Attraction::getExternalId, Attraction::setExternalId);
            if (isOperatorApplicable()) {
                binder.forField(operator).bind(Attraction::getOperator, Attraction::setOperator);
            }

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
            mainUpload.setAcceptedMimeTypes("image/*");
            mainUpload.setMaxFiles(1);
            mainUpload.setMaxFileSize((int) MAX_IMAGE_SIZE_BYTES);

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
            additionalUpload.setAcceptedMimeTypes("image/*");
            additionalUpload.setMaxFileSize((int) MAX_IMAGE_SIZE_BYTES);
            additionalUpload.addFileRemovedListener(event -> {
                additionalImagesMetadata.remove(event.getFileName());
                additionalImagesData.remove(event.getFileName());
                var textField = additionalImagesDescription.get(event.getFileName());
                additionalImagesLayout.remove(textField);
                additionalImagesDescription.remove(event.getFileName());
            });

            fileDescription.setWidthFull();

            var fileUploadHandler = UploadHandler.inMemory((metadata, data) -> {
                fileMetadata = metadata;
                fileData = data;
            });
            var fileUpload = new Upload(fileUploadHandler);
            fileUpload.setAcceptedMimeTypes(ALLOWED_FILE_MIME_TYPES.toArray(String[]::new));
            fileUpload.setMaxFiles(1);
            fileUpload.setMaxFileSize((int) MAX_FILE_SIZE);
            fileUpload.addFileRemovedListener(_ -> {
                fileMetadata = null;
                fileData = null;
            });

            var mainImageHint = new Span("Optional (max. 1, max. %d MB). Wird ein neues Bild hochgeladen wird das bestehende automatisch ersetzt."
                                                 .formatted(MAX_IMAGE_SIZE_BYTES / (1024 * 1024)));
            mainImageHint.addClassName(LumoUtility.FontSize.SMALL);

            add(form,
                new Hr(),
                new H3("Hauptbild"), mainImageHint, mainUpload, mainImageDescription,
                new Hr(),
                new H3("Weitere Bilder (max. %d MB pro Bild)".formatted(MAX_IMAGE_SIZE_BYTES / (1024 * 1024))),
                additionalUpload, additionalImagesLayout, imageInfoLayout,
                new Hr(),
                new H3("Dateien (PDF/Office, max. 10 MB)"), fileUpload, fileDescription, fileInfoLayout);

            var save = new Button("Speichern");
            save.addClickListener(_ -> {
                if (saveAttraction()) {
                    onSuccessCallback.run();
                } else {
                    save.setEnabled(true);
                }
            });
            save.setDisableOnClick(true);
            save.addThemeVariants(LUMO_PRIMARY);

            binder.addStatusChangeListener(_ -> save.setEnabled(binder.isValid()));

            var delete = new Button("Löschen", _ -> {
                deleteAttraction();
                onSuccessCallback.run();
            });
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

            var cancel = new Button("Abbrechen", _ -> close());
            getFooter().add(delete, cancel, save);
        }

        private void updatePreview(String md) {
            if (isBlank(md)) {
                preview.getElement().setProperty("innerHTML", "");
            } else {
                var parser = Parser.builder().build();
                var document = parser.parse(md);
                var renderer = HtmlRenderer.builder().build();
                preview.getElement().setProperty("innerHTML", renderer.render(document));
            }
        }

        public void open(Attraction attraction) {
            binder.setBean(attraction);
            // Operator only applies to FOOD; show it accordingly (single-type FOOD views always, others when FOOD is set).
            if (isOperatorApplicable()) {
                operator.setVisible(AttractionType.fromDb(attraction.getType()).orElse(null) == FOOD);
            }
            updatePreview(attraction.getDescription());
            imageInfoLayout.removeAll();
            additionalImagesLayout.removeAll();
            additionalImagesDescription.clear();
            additionalImagesMetadata.clear();
            additionalImagesData.clear();
            mainImageDescription.setValue("");
            mainImageDescription.setVisible(false);
            fileInfoLayout.removeAll();
            fileDescription.clear();
            fileMetadata = null;
            fileData = null;

            if (attraction.getId() != null) {
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
                            deleteImage(binder.getBean().getId(), imageId, cloudflareId);
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

                // Show existing files
                attractionFileService.findByAttractionId(attraction.getId()).forEach(file -> {
                    var label = new Span(file.description() + " (" + file.filename() + ", " + formatFileSize(file.filesize()) + ")");
                    var row = new HorizontalLayout();
                    row.setAlignItems(Alignment.CENTER);
                    row.setSpacing(true);

                    var deleteBtn = new Button("Löschen");
                    deleteBtn.addClickListener(_ -> {
                        try {
                            attractionFileService.delete(file.id());
                            fileInfoLayout.remove(row);
                        } catch (Exception ex) {
                            log.error("Error deleting file {} for attraction {}", file.id(), binder.getBean().getId(), ex);
                            showNotification("Datei konnte nicht gelöscht werden", LUMO_ERROR);
                        }
                    });
                    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

                    row.add(label, deleteBtn);
                    fileInfoLayout.add(row);
                });
            }
            loadExtraFields(attraction);
            super.open();
        }

        private boolean saveAttraction() {
            if (!binder.validate().isOk()) {
                showNotification("Alle erforderliche Felder ausfüllen.", LUMO_WARNING);
                return false;
            }

            var attraction = binder.getBean();
            boolean creating = attraction.getId() == null;

            // Main image is optional (0 or 1). Only its description is required when one is uploaded.
            if (mainImageData != null && isBlank(mainImageDescription.getValue())) {
                showNotification("Beschreibung für Hauptbild ist erforderlich", LUMO_ERROR);
                return false;
            }
            if (mainImageData != null && mainImageData.length > MAX_IMAGE_SIZE_BYTES) {
                showNotification("Hauptbild ist zu gross (max. %d MB)".formatted(MAX_IMAGE_SIZE_BYTES / (1024 * 1024)), LUMO_ERROR);
                return false;
            }

            for (var additionalImage : additionalImagesMetadata.entrySet()) {
                var descField = additionalImagesDescription.get(additionalImage.getKey());
                if (descField == null || isBlank(descField.getValue())) {
                    showNotification("Beschreibung für " + additionalImage.getKey() + " ist erforderlich", LUMO_ERROR);
                    return false;
                }
                var data = additionalImagesData.get(additionalImage.getKey());
                if (data != null && data.length > MAX_IMAGE_SIZE_BYTES) {
                    showNotification("Bild ist zu gross (max. %d MB): %s".formatted(MAX_IMAGE_SIZE_BYTES / (1024 * 1024), additionalImage.getKey()),
                                     LUMO_ERROR);
                    return false;
                }
            }

            if (fileData != null) {
                if (isBlank(fileDescription.getValue())) {
                    showNotification("Beschreibung für die Datei ist erforderlich", LUMO_ERROR);
                    return false;
                }
                if (!ALLOWED_FILE_MIME_TYPES.contains(fileMetadata.contentType())) {
                    showNotification("Dateityp nicht erlaubt (nur PDF/Office): " + fileMetadata.contentType(), LUMO_ERROR);
                    return false;
                }
                if (fileData.length > MAX_FILE_SIZE) {
                    showNotification("Datei ist zu gross (max. 10 MB)", LUMO_ERROR);
                    return false;
                }
            }

            if (!validateExtraFields()) {
                return false;
            }

            if (creating) {
                var it = dslContext.newRecord(ATTRACTION, attraction);
                it.insert();
                attraction.setId(it.getId());
            } else {
                attractionDao.update(attraction);
            }

            if (mainImageData != null) {
                // Guarantee at most one main image: drop any existing main image before adding the new one.
                deleteExistingMainImages(attraction.getId());
                uploadAndLinkImage(attraction.getId(), mainImageData, mainImageMetadata.fileName(), mainImageMetadata.contentType(), MAIN, mainImageDescription.getValue());
            }

            for (var additionalImage : additionalImagesData.entrySet()) {
                var desc = additionalImagesDescription.get(additionalImage.getKey()).getValue();
                uploadAndLinkImage(attraction.getId(), additionalImage.getValue(), additionalImage.getKey(),
                                   additionalImagesMetadata.get(additionalImage.getKey()).contentType(), ADDITIONAL, desc);
            }

            if (fileData != null) {
                var uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();
                attractionFileService.create(attraction.getId(), fileMetadata.fileName(), fileMetadata.contentType(),
                                             fileData.length, fileData, fileDescription.getValue(), uploadedBy);
            }

            saveExtraFields(attraction.getId());

            mainImageData = null;
            mainImageMetadata = null;
            additionalImagesData.clear();
            additionalImagesMetadata.clear();
            additionalImagesDescription.clear();
            fileData = null;
            fileMetadata = null;

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

        /**
         * Removes every existing main image of the attraction (relation, image row and Cloudflare asset).
         * Loops over all matches so any legacy duplicates are cleaned up too, leaving at most one main image
         * once a replacement is linked.
         */
        private void deleteExistingMainImages(Long attractionId) {
            dslContext.select(IMAGE.ID, IMAGE.CLOUDFLARE_ID)
                      .from(IMAGE)
                      .join(ATTRACTION_IMAGE).on(IMAGE.ID.eq(ATTRACTION_IMAGE.IMAGE_ID))
                      .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(attractionId)
                                                           .and(ATTRACTION_IMAGE.TYPE.eq(MAIN.toDb())))
                      .fetch()
                      .forEach(r -> deleteImage(attractionId, r.get(IMAGE.ID), r.get(IMAGE.CLOUDFLARE_ID)));
        }

        private void deleteImage(Long attractionId, Long imageId, String cloudflareId) {
            dslContext.deleteFrom(ATTRACTION_IMAGE)
                      .where(ATTRACTION_IMAGE.ATTRACTION_ID.eq(attractionId)
                                                           .and(ATTRACTION_IMAGE.IMAGE_ID.eq(imageId)))
                      .execute();

            dslContext.deleteFrom(IMAGE)
                      .where(IMAGE.ID.eq(imageId))
                      .execute();

            cloudflareService.delete(cloudflareId);
        }

        private void deleteAttraction() {
            var attraction = binder.getBean();
            if (attraction != null && attraction.getId() != null) {
                deleteExtraFields(attraction.getId());
                attractionDao.delete(attraction);
                close();
            }
        }
    }
}
