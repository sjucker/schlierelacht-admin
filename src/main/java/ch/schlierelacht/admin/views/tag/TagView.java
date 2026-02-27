package ch.schlierelacht.admin.views.tag;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.jooq.tables.daos.TagDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Tag;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.Comparator;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;

@PageTitle("Tags")
@Route(value = "tags", layout = MainLayout.class)
@PermitAll
public class TagView extends VerticalLayout {

    private final TagDao tagDao;
    private final Grid<Tag> grid;
    private final TagDialog dialog;

    public TagView(TagDao tagDao) {
        this.tagDao = tagDao;
        this.dialog = new TagDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("Tags verwalten"));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Tag> createGrid() {
        var g = new Grid<Tag>();
        g.addComponentColumn(t -> new Button(EDIT.create(), _ -> dialog.open(t))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(Tag::getName).setHeader("Name").setSortable(true);
        g.addColumn(t -> AttractionType.fromDb(t.getType()).map(AttractionType::getDescription).orElse("")).setHeader("Typ").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addTagButton = new Button("Tag hinzufügen", _ -> {
            Tag tag = new Tag();
            dialog.open(tag);
        });

        return new HorizontalLayout(addTagButton);
    }

    private void refreshGrid() {
        grid.setItems(tagDao.findAll().stream().sorted(Comparator.comparing(Tag::getName)).toList());
    }

    private class TagDialog extends Dialog {

        private final Binder<Tag> binder = new Binder<>(Tag.class);

        public TagDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setHeaderTitle("Tag bearbeiten");

            var form = new FormLayout();

            var name = new TextField("Name");
            var type = new ComboBox<AttractionType>("Typ");
            type.setItems(AttractionType.values());
            type.setItemLabelGenerator(AttractionType::getDescription);

            form.add(name, type);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(name)
                  .asRequired()
                  .bind(Tag::getName, Tag::setName);

            binder.forField(type)
                  .asRequired()
                  .bind(t -> AttractionType.fromDb(t.getType()).orElse(null),
                        (t, v) -> t.setType(v != null ? v.toDb() : null));

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
            delete.addThemeVariants(LUMO_ERROR);

            var cancel = new Button("Abbrechen");
            cancel.addClickListener(_ -> close());

            getFooter().add(delete, cancel, save);
        }

        public void open(Tag tag) {
            binder.setBean(tag);
            super.open();
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var tag = binder.getBean();
                if (tag.getId() == null) {
                    tagDao.insert(tag);
                } else {
                    tagDao.update(tag);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var tag = binder.getBean();
            if (tag != null && tag.getId() != null) {
                tagDao.delete(tag);
                close();
            }
        }
    }
}
