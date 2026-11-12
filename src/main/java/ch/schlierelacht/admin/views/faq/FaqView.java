package ch.schlierelacht.admin.views.faq;

import ch.schlierelacht.admin.jooq.tables.daos.FaqDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Faq;
import ch.schlierelacht.admin.service.FaqService;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.grid.ColumnTextAlign.END;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * FAQ for the app: questions with markdown answers, grouped by their group name. The grid lists all entries in the
 * app's order ({@link FaqService}), the inactive ones included.
 */
@PageTitle("FAQ")
@Route(value = "faq", layout = MainLayout.class)
@PermitAll
public class FaqView extends VerticalLayout {

    private final FaqDao faqDao;
    private final FaqService faqService;
    private final Grid<Faq> grid;
    private final FaqDialog dialog;

    public FaqView(FaqDao faqDao, FaqService faqService) {
        this.faqDao = faqDao;
        this.faqService = faqService;
        this.dialog = new FaqDialog();
        setSizeFull();

        add(new H2("FAQ verwalten"),
            new Paragraph("Einträge mit derselben Gruppe erscheinen in der App zusammen. Die Gruppen folgen der kleinsten " +
                          "Reihenfolge-Nummer ihrer Einträge, die Einträge innerhalb einer Gruppe ihrer Nummer (bei gleicher " +
                          "Nummer alphabetisch). Massgebend sind nur die aktiven Einträge, denn nur sie sind in der App " +
                          "sichtbar; Gruppen ohne aktiven Eintrag stehen hier am Schluss."));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Faq> createGrid() {
        var g = new Grid<Faq>();
        g.addComponentColumn(f -> new Button(EDIT.create(), _ -> dialog.open(f))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(f -> f.getGroupName().trim()).setHeader("Gruppe").setSortable(true);
        g.addColumn(Faq::getSortOrder).setHeader("Reihenfolge").setSortable(true).setTextAlign(END).setFlexGrow(0).setWidth("130px");
        g.addColumn(Faq::getTitle).setHeader("Titel").setSortable(true).setFlexGrow(3);
        g.addColumn(f -> Boolean.TRUE.equals(f.getActive()) ? "Ja" : "Nein").setHeader("Aktiv").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("Eintrag hinzufügen", _ -> {
            var faq = new Faq();
            faq.setSortOrder(0);
            faq.setActive(false);
            dialog.open(faq);
        });
        return new HorizontalLayout(addButton);
    }

    private void refreshGrid() {
        // the app's order (FaqService is the one place it lives), plus the inactive entries
        grid.setItems(faqService.findAllInAppOrder());
    }

    private class FaqDialog extends Dialog {

        private final Binder<Faq> binder = new Binder<>(Faq.class);
        private final Div contentPreview = new Div();
        private final ComboBox<String> group = new ComboBox<>("Gruppe");

        public FaqDialog() {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("FAQ-Eintrag bearbeiten");
            setWidth("700px");

            var form = new FormLayout();

            // existing groups to pick from (avoids typos), or type a new one
            group.setAllowCustomValue(true);
            group.addCustomValueSetListener(e -> group.setValue(e.getDetail().trim()));
            group.setHelperText("Bestehende Gruppe wählen oder neue eingeben");

            var sortOrder = new IntegerField("Reihenfolge");
            sortOrder.setStepButtonsVisible(true);
            var active = new Checkbox("Aktiv (in der App sichtbar)");
            var title = new TextField("Titel (Frage)");
            var content = new TextArea("Inhalt (Antwort, Markdown)");
            content.setMinHeight("200px");
            content.setWidthFull();
            content.setValueChangeMode(ValueChangeMode.TIMEOUT);
            content.addValueChangeListener(e -> updatePreview(e.getValue()));

            contentPreview.setWidthFull();
            var previewLayout = new VerticalLayout(new Span("Markdown Vorschau:"), contentPreview);
            previewLayout.setPadding(false);
            previewLayout.setSpacing(false);

            form.add(group, sortOrder, active, title, content, previewLayout);
            form.setColspan(active, 2);
            form.setColspan(title, 2);
            form.setColspan(content, 2);
            form.setColspan(previewLayout, 2);
            form.setResponsiveSteps(new ResponsiveStep("0", 1),
                                    new ResponsiveStep("500px", 2));
            add(form);

            binder.forField(group)
                  .asRequired()
                  .withValidator(v -> !isBlank(v), "Gruppe angeben")
                  .bind(Faq::getGroupName, (f, v) -> f.setGroupName(v == null ? null : v.trim()));

            binder.forField(sortOrder)
                  .asRequired()
                  .bind(Faq::getSortOrder, Faq::setSortOrder);

            binder.forField(active)
                  .bind(Faq::getActive, Faq::setActive);

            binder.forField(title)
                  .asRequired()
                  .bind(Faq::getTitle, Faq::setTitle);

            binder.forField(content)
                  .asRequired()
                  .bind(Faq::getContent, Faq::setContent);

            var save = new Button("Speichern");
            save.addClickListener(_ -> {
                if (save()) {
                    refreshGrid();
                    showNotification("Speichern erfolgreich", LUMO_SUCCESS);
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

        public void open(Faq faq) {
            group.setItems(faqService.findGroupNames());
            binder.setBean(faq);
            updatePreview(faq.getContent());
            super.open();
        }

        private void updatePreview(String md) {
            if (isBlank(md)) {
                contentPreview.getElement().setProperty("innerHTML", "");
            } else {
                var document = Parser.builder().build().parse(md);
                contentPreview.getElement().setProperty("innerHTML", HtmlRenderer.builder().build().render(document));
            }
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var faq = binder.getBean();
                if (faq.getId() == null) {
                    faqDao.insert(faq);
                } else {
                    faqDao.update(faq);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var faq = binder.getBean();
            if (faq != null && faq.getId() != null) {
                faqDao.delete(faq);
                close();
                refreshGrid();
                showNotification("Löschen erfolgreich", LUMO_SUCCESS);
            }
        }
    }
}
