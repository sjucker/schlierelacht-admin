package ch.schlierelacht.admin.views.message;

import ch.schlierelacht.admin.dto.MessagePriority;
import ch.schlierelacht.admin.jooq.tables.daos.LoginDao;
import ch.schlierelacht.admin.jooq.tables.daos.MessageDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Login;
import ch.schlierelacht.admin.jooq.tables.pojos.Message;
import ch.schlierelacht.admin.service.MessageService;
import ch.schlierelacht.admin.util.DateUtil;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.schlierelacht.admin.util.DateUtil.formatDateTime;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Meldungen for the app: emergency alerts, warnings and infos. The app shows the most important active one on its
 * home screen (highest priority, then the newest) and all active ones in a list.
 */
@PageTitle("Meldungen")
@Route(value = "meldungen", layout = MainLayout.class)
@PermitAll
public class MessageView extends VerticalLayout {

    /**
     * Like the app: priority, then most recently published; never published (prepared) ones last, newest first.
     */
    private static final Comparator<Message> ORDER = Comparator.comparing(Message::getPriority)
                                                               .thenComparing(Message::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                                               .thenComparing(Message::getCreatedAt, Comparator.reverseOrder());

    private final MessageDao messageDao;
    private final LoginDao loginDao;
    private final Grid<Message> grid;
    private final MessageDialog dialog;
    private Map<String, String> loginNames = Map.of();

    public MessageView(MessageDao messageDao, LoginDao loginDao) {
        this.messageDao = messageDao;
        this.loginDao = loginDao;
        this.dialog = new MessageDialog(() -> {
            refreshGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        setSizeFull();

        add(new H2("Meldungen verwalten"),
            new Paragraph("Die App zeigt auf der Startseite die wichtigste aktive Meldung (Notfall vor Warnung vor Info, " +
                          "bei gleicher Priorität die zuletzt veröffentlichte) und alle aktiven Meldungen in einer Liste. " +
                          "Meldungen lassen sich inaktiv vorbereiten: beim Aktivieren erhalten sie ihren Veröffentlichungszeitpunkt, " +
                          "den die App anzeigt."));

        grid = createGrid();
        add(grid, createToolbar());

        refreshGrid();
    }

    private Grid<Message> createGrid() {
        var g = new Grid<Message>();
        g.addComponentColumn(m -> new Button(EDIT.create(), _ -> dialog.open(m))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addComponentColumn(m -> priorityBadge(m.getPriority())).setHeader("Priorität").setComparator(Message::getPriority);
        g.addColumn(Message::getTitle).setHeader("Titel").setSortable(true).setFlexGrow(3);
        g.addColumn(m -> Boolean.TRUE.equals(m.getActive()) ? "Ja" : "Nein").setHeader("Aktiv").setSortable(true);
        g.addColumn(m -> formatDateTime(m.getPublishedAt())).setHeader("Veröffentlicht am")
         .setComparator(Comparator.comparing(Message::getPublishedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        g.addColumn(m -> formatDateTime(m.getCreatedAt())).setHeader("Erstellt am").setComparator(Message::getCreatedAt);
        g.addColumn(m -> creatorName(m.getCreatedBy())).setHeader("Erstellt von").setSortable(true);
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                dialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createToolbar() {
        var addButton = new Button("Meldung hinzufügen", _ -> {
            var message = new Message();
            message.setPriority(MessagePriority.LOW.toDb());
            message.setActive(false);
            dialog.open(message);
        });
        return new HorizontalLayout(addButton);
    }

    private void refreshGrid() {
        loginNames = loginDao.findAll().stream().collect(Collectors.toMap(Login::getEmail, Login::getName));
        grid.setItems(messageDao.findAll().stream().sorted(ORDER).toList());
    }

    private String creatorName(String email) {
        return email == null ? "" : loginNames.getOrDefault(email, email);
    }

    private static Span priorityBadge(ch.schlierelacht.admin.jooq.enums.MessagePriority dbPriority) {
        var priority = MessagePriority.fromDb(dbPriority).orElse(MessagePriority.LOW);
        var badge = new Span(priority.getDescription());
        var style = badge.getStyle();
        switch (priority) {
            case HIGH -> style.setBackgroundColor("#b3261e").setColor("#ffffff");
            case MEDIUM -> style.setBackgroundColor("#f9a825").setColor("#000000");
            case LOW -> style.setBackgroundColor("#7ca6d8").setColor("#000000");
        }
        style.setPadding("2px 8px").setBorderRadius("999px").setFontWeight("600");
        return badge;
    }

    private class MessageDialog extends Dialog {

        private final Binder<Message> binder = new Binder<>(Message.class);
        private final Div contentPreview = new Div();
        private final Span createdInfo = new Span();
        /**
         * Whether the message was active when the dialog opened: saving it active then doesn't publish it anew.
         */
        private boolean wasActive;

        public MessageDialog(@NonNull Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Meldung bearbeiten");
            setWidth("700px");

            var form = new FormLayout();

            var priority = new Select<MessagePriority>();
            priority.setLabel("Priorität");
            priority.setItems(MessagePriority.values());
            priority.setItemLabelGenerator(MessagePriority::getDescription);

            var active = new Checkbox("Aktiv (in der App sichtbar)");
            var title = new TextField("Titel");
            var content = new TextArea("Inhalt (Markdown)");
            content.setMinHeight("200px");
            content.setWidthFull();
            content.setValueChangeMode(ValueChangeMode.TIMEOUT);
            content.addValueChangeListener(e -> updatePreview(e.getValue()));

            contentPreview.setWidthFull();
            var previewLayout = new VerticalLayout(new Span("Markdown Vorschau:"), contentPreview);
            previewLayout.setPadding(false);
            previewLayout.setSpacing(false);

            createdInfo.getStyle().setColor("var(--lumo-secondary-text-color)");

            form.add(priority, active, title, content, previewLayout, createdInfo);
            form.setColspan(title, 2);
            form.setColspan(content, 2);
            form.setColspan(previewLayout, 2);
            form.setColspan(createdInfo, 2);
            form.setResponsiveSteps(new ResponsiveStep("0", 1),
                                    new ResponsiveStep("500px", 2));
            add(form);

            binder.forField(priority)
                  .asRequired()
                  .bind(m -> MessagePriority.fromDb(m.getPriority()).orElse(null),
                        (m, value) -> m.setPriority(value == null ? null : value.toDb()));

            binder.forField(active)
                  .bind(Message::getActive, Message::setActive);

            binder.forField(title)
                  .asRequired()
                  .bind(Message::getTitle, Message::setTitle);

            binder.forField(content)
                  .asRequired()
                  .bind(Message::getContent, Message::setContent);

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

        public void open(Message message) {
            wasActive = Boolean.TRUE.equals(message.getActive());
            binder.setBean(message);
            updatePreview(message.getContent());
            var info = message.getId() == null
                       ? ""
                       : "Erstellt am %s von %s".formatted(formatDateTime(message.getCreatedAt()), creatorName(message.getCreatedBy()));
            if (message.getPublishedAt() != null) {
                info += ", zuletzt veröffentlicht am %s".formatted(formatDateTime(message.getPublishedAt()));
            }
            createdInfo.setText(info);
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
                var message = binder.getBean();
                var now = DateUtil.now();
                MessageService.markPublishedIfActivated(message, wasActive, now);
                if (message.getId() == null) {
                    // the login's email, like the uploader of a download or gallery image
                    message.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
                    message.setCreatedAt(now);
                    messageDao.insert(message);
                } else {
                    messageDao.update(message);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var message = binder.getBean();
            if (message != null && message.getId() != null) {
                messageDao.delete(message);
                close();
            }
        }
    }
}
