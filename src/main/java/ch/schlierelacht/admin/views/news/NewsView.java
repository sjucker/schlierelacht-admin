package ch.schlierelacht.admin.views.news;

import ch.schlierelacht.admin.jooq.tables.daos.NewsDao;
import ch.schlierelacht.admin.jooq.tables.pojos.News;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.util.DateUtil;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jspecify.annotations.NonNull;

import static ch.schlierelacht.admin.views.util.DatePickerUtil.SWISS_LOCALE;
import static ch.schlierelacht.admin.views.util.DatePickerUtil.getGermanI18n;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@PageTitle("News")
@Route(value = "news", layout = MainLayout.class)
@PermitAll
public class NewsView extends VerticalLayout {

    private final NewsDao newsDao;
    private final CloudflareService cloudflareService;
    private final Grid<News> grid;
    private final NewsDialog dialog;

    public NewsView(NewsDao newsDao, CloudflareService cloudflareService) {
        this.newsDao = newsDao;
        this.cloudflareService = cloudflareService;
        this.dialog = new NewsDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("News verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<News> createGrid() {
        var g = new Grid<News>();
        g.addComponentColumn(n -> new Button(EDIT.create(), _ -> dialog.open(n))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(News::getDate).setHeader("Datum").setSortable(true);
        g.addColumn(News::getTitle).setHeader("Titel").setSortable(true);
        g.addColumn(n -> {
            var text = n.getIntroText();
            return text != null && text.length() > 80 ? text.substring(0, 80) + "…" : text;
        }).setHeader("Intro-Text");
        g.addColumn(n -> Boolean.TRUE.equals(n.getActive()) ? "Ja" : "Nein").setHeader("Aktiv").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("Eintrag hinzufügen", _ -> {
            var news = new News();
            news.setDate(DateUtil.today());
            news.setActive(false);
            dialog.open(news);
        });
        return new HorizontalLayout(addButton);
    }

    private void refreshGrid() {
        grid.setItems(newsDao.findAll().stream()
                             .sorted((a, b) -> {
                                 if (a.getDate() == null) return 1;
                                 if (b.getDate() == null) return -1;
                                 return b.getDate().compareTo(a.getDate());
                             })
                             .toList());
    }

    private class NewsDialog extends Dialog {

        private final Binder<News> binder = new Binder<>(News.class);
        private final Div fullTextPreview = new Div();

        public NewsDialog(@NonNull Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("News-Eintrag bearbeiten");
            setWidth("700px");

            var form = new FormLayout();

            var date = new DatePicker("Datum");
            date.setLocale(SWISS_LOCALE);
            date.setI18n(getGermanI18n());
            var title = new TextField("Titel");
            var introText = new TextArea("Intro-Text");
            introText.setMinHeight("100px");
            var fullText = new TextArea("Volltext");
            fullText.setMinHeight("200px");
            fullText.setWidthFull();
            fullText.setValueChangeMode(ValueChangeMode.TIMEOUT);

            fullTextPreview.setWidthFull();
            fullText.addValueChangeListener(e -> updatePreview(e.getValue()));

            var fullTextPreviewLayout = new VerticalLayout(new Span("Markdown Vorschau:"), fullTextPreview);
            fullTextPreviewLayout.setPadding(false);
            fullTextPreviewLayout.setSpacing(false);

            var active = new Checkbox("Aktiv");

            var cloudflareId = new TextField("Cloudflare ID");
            cloudflareId.setReadOnly(true);

            var upload = createUpload(cloudflareId);

            var imagePreview = new VerticalLayout();
            imagePreview.setPadding(false);
            imagePreview.setSpacing(false);

            form.add(date, active, title, introText, fullText, fullTextPreviewLayout, new H4("Bild"), upload, imagePreview);
            form.setColspan(title, 2);
            form.setColspan(introText, 2);
            form.setColspan(fullText, 2);
            form.setColspan(fullTextPreviewLayout, 2);
            form.setColspan(upload, 2);
            form.setColspan(imagePreview, 2);
            form.setResponsiveSteps(new ResponsiveStep("0", 1),
                                    new ResponsiveStep("500px", 2));

            add(form);

            binder.forField(date)
                  .asRequired()
                  .bind(News::getDate, News::setDate);

            binder.forField(title)
                  .asRequired()
                  .bind(News::getTitle, News::setTitle);

            binder.forField(introText)
                  .asRequired()
                  .bind(News::getIntroText, News::setIntroText);

            binder.forField(fullText)
                  .asRequired()
                  .bind(News::getFullText, News::setFullText);

            binder.forField(active)
                  .bind(News::getActive, News::setActive);

            binder.forField(cloudflareId)
                  .bind(News::getCloudflareId, News::setCloudflareId);

            cloudflareId.addValueChangeListener(e -> {
                imagePreview.removeAll();
                if (isNotBlank(e.getValue())) {
                    var img = new CloudflareImage(cloudflareService, e.getValue(), "Vorschau");
                    img.setWidth("300px");
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

            var delete = new Button("Löschen", _ -> delete());
            delete.addThemeVariants(LUMO_ERROR);

            var cancel = new Button("Abbrechen");
            cancel.addClickListener(_ -> close());

            getFooter().add(delete, cancel, save);
        }

        private Upload createUpload(TextField cloudflareId) {
            var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
                var news = binder.getBean();
                var id = news.getId() == null ? "new" : news.getId().toString();
                var resultId = cloudflareService.upload(data, metadata.fileName(), metadata.contentType(), metadata.contentLength(), id, "NewsView");
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

        public void open(News news) {
            binder.setBean(news);
            updatePreview(news.getFullText());
            super.open();
        }

        private void updatePreview(String md) {
            if (isBlank(md)) {
                fullTextPreview.getElement().setProperty("innerHTML", "");
            } else {
                var parser = Parser.builder().build();
                var document = parser.parse(md);
                var renderer = HtmlRenderer.builder().build();
                fullTextPreview.getElement().setProperty("innerHTML", renderer.render(document));
            }
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var news = binder.getBean();
                if (news.getId() == null) {
                    newsDao.insert(news);
                } else {
                    newsDao.update(news);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var news = binder.getBean();
            if (news != null && news.getId() != null) {
                newsDao.delete(news);
                close();
            }
        }
    }
}
