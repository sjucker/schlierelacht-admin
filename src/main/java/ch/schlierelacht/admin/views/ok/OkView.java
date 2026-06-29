package ch.schlierelacht.admin.views.ok;

import ch.schlierelacht.admin.dto.OkTeam;
import ch.schlierelacht.admin.jooq.tables.daos.OkMemberDao;
import ch.schlierelacht.admin.jooq.tables.daos.OkTeamMemberDao;
import ch.schlierelacht.admin.jooq.tables.pojos.OkMember;
import ch.schlierelacht.admin.jooq.tables.pojos.OkTeamMember;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.CloudflareImage;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;

import java.util.Comparator;

import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.ModalityMode.STRICT;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR;
import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;
import static com.vaadin.flow.component.grid.ColumnTextAlign.CENTER;
import static com.vaadin.flow.component.icon.VaadinIcon.EDIT;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_SUCCESS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@PageTitle("OK Komitee")
@Route(value = "ok", layout = MainLayout.class)
@PermitAll
public class OkView extends VerticalLayout {

    private final OkMemberDao okMemberDao;
    private final OkTeamMemberDao okTeamMemberDao;
    private final CloudflareService cloudflareService;
    private final Grid<OkMember> membersGrid;
    private final Grid<OkTeamMember> teamMembersGrid;
    private final OkMemberDialog memberDialog;
    private final OkTeamMemberDialog teamMemberDialog;

    public OkView(OkMemberDao okMemberDao, OkTeamMemberDao okTeamMemberDao, CloudflareService cloudflareService) {
        this.okMemberDao = okMemberDao;
        this.okTeamMemberDao = okTeamMemberDao;
        this.cloudflareService = cloudflareService;

        this.memberDialog = new OkMemberDialog(() -> {
            refreshMembersGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });
        this.teamMemberDialog = new OkTeamMemberDialog(() -> {
            refreshTeamMembersGrid();
            showNotification("Speichern erfolgreich", LUMO_SUCCESS);
        });

        setSizeFull();
        add(new H2("OK Komitee verwalten"));

        add(new H3("Kern-Mitglieder (mit Bild)"));
        membersGrid = createMembersGrid();
        add(membersGrid, createMembersToolbar());

        add(new H3("Team-Mitglieder"));
        teamMembersGrid = createTeamMembersGrid();
        add(teamMembersGrid, createTeamMembersToolbar());

        refreshMembersGrid();
        refreshTeamMembersGrid();
    }

    private Grid<OkMember> createMembersGrid() {
        var g = new Grid<OkMember>();
        g.addComponentColumn(m -> new Button(EDIT.create(), _ -> memberDialog.open(m))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(OkMember::getSortOrder).setHeader("Reihenfolge").setWidth("120px").setFlexGrow(0).setSortable(true);
        g.addColumn(OkMember::getName).setHeader("Name").setSortable(true);
        g.addColumn(OkMember::getRole).setHeader("Funktion").setSortable(true);
        g.addColumn(OkMember::getEmail).setHeader("E-Mail");
        g.addColumn(OkMember::getCloudflareId).setHeader("Cloudflare ID");
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                memberDialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createMembersToolbar() {
        var addButton = new Button("Kern-Mitglied hinzufügen", _ -> {
            var member = new OkMember();
            member.setSortOrder(0);
            memberDialog.open(member);
        });
        return new HorizontalLayout(addButton);
    }

    private void refreshMembersGrid() {
        membersGrid.setItems(okMemberDao.findAll().stream()
                .sorted(Comparator.comparing(OkMember::getSortOrder).thenComparing(OkMember::getName))
                .toList());
    }

    private Grid<OkTeamMember> createTeamMembersGrid() {
        var g = new Grid<OkTeamMember>();
        g.addComponentColumn(m -> new Button(EDIT.create(), _ -> teamMemberDialog.open(m))).setWidth("80px").setTextAlign(CENTER).setFlexGrow(0);
        g.addColumn(OkTeamMember::getName).setHeader("Name").setSortable(true);
        g.addColumn(m -> OkTeam.fromDb(m.getTeam()).map(OkTeam::getDescription).orElse("")).setHeader("Team").setSortable(true);
        g.addColumn(OkTeamMember::getEmail).setHeader("E-Mail");
        g.addItemDoubleClickListener(event -> {
            if (event.getItem() != null) {
                teamMemberDialog.open(event.getItem());
            }
        });
        return g;
    }

    private HorizontalLayout createTeamMembersToolbar() {
        var addButton = new Button("Team-Mitglied hinzufügen", _ -> teamMemberDialog.open(new OkTeamMember()));
        return new HorizontalLayout(addButton);
    }

    private void refreshTeamMembersGrid() {
        teamMembersGrid.setItems(okTeamMemberDao.findAll().stream()
                .sorted(Comparator.comparing((OkTeamMember m) -> m.getTeam() != null ? m.getTeam().getLiteral() : "")
                        .thenComparing(OkTeamMember::getName))
                .toList());
    }

    private class OkMemberDialog extends Dialog {

        private final Binder<OkMember> binder = new Binder<>(OkMember.class);

        public OkMemberDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Kern-Mitglied bearbeiten");

            var form = new FormLayout();

            var name = new TextField("Name");
            var role = new TextField("Funktion");
            var email = new TextField("E-Mail");
            var sortOrder = new IntegerField("Reihenfolge");
            var cloudflareId = new TextField("Cloudflare ID");
            cloudflareId.setReadOnly(true);

            var upload = createUpload(cloudflareId);

            var imagePreview = new VerticalLayout();
            imagePreview.setPadding(false);
            imagePreview.setSpacing(false);

            form.add(name, role, email, sortOrder, cloudflareId, upload, imagePreview);
            form.setColspan(upload, 2);
            form.setColspan(imagePreview, 2);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(name).asRequired().bind(OkMember::getName, OkMember::setName);
            binder.forField(role).asRequired().bind(OkMember::getRole, OkMember::setRole);
            binder.forField(email).bind(OkMember::getEmail, OkMember::setEmail);
            binder.forField(sortOrder).asRequired().bind(OkMember::getSortOrder, OkMember::setSortOrder);
            binder.forField(cloudflareId).bind(OkMember::getCloudflareId, OkMember::setCloudflareId);

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
            delete.addThemeVariants(LUMO_ERROR);

            var cancel = new Button("Abbrechen", _ -> close());

            getFooter().add(delete, cancel, save);
        }

        private Upload createUpload(TextField cloudflareId) {
            var uploadHandler = UploadHandler.inMemory((metadata, data) -> {
                var member = binder.getBean();
                var id = member.getId() == null ? "new" : member.getId().toString();
                var resultId = cloudflareService.upload(data, metadata.fileName(), metadata.contentType(), metadata.contentLength(), id, "OkView");
                resultId.ifPresent(cloudflareId::setValue);
            });
            uploadHandler.whenComplete(success -> {
                if (!success) {
                    showNotification("Upload fehlgeschlagen", com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
                }
            });
            var upload = new Upload(uploadHandler);
            upload.setAcceptedMimeTypes("image/*");
            upload.setMaxFiles(1);
            return upload;
        }

        public void open(OkMember member) {
            binder.setBean(member);
            super.open();
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var member = binder.getBean();
                if (member.getId() == null) {
                    okMemberDao.insert(member);
                } else {
                    okMemberDao.update(member);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var member = binder.getBean();
            if (member != null && member.getId() != null) {
                okMemberDao.delete(member);
                close();
            }
        }
    }

    private class OkTeamMemberDialog extends Dialog {

        private final Binder<OkTeamMember> binder = new Binder<>(OkTeamMember.class);

        public OkTeamMemberDialog(Runnable onSuccessCallback) {
            setModality(STRICT);
            setCloseOnOutsideClick(false);
            setCloseOnEsc(false);
            setHeaderTitle("Team-Mitglied bearbeiten");

            var form = new FormLayout();

            var name = new TextField("Name");

            var team = new ComboBox<OkTeam>("Team");
            team.setItems(OkTeam.values());
            team.setItemLabelGenerator(OkTeam::getDescription);

            var email = new TextField("E-Mail");

            form.add(name, team, email);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                    new FormLayout.ResponsiveStep("500px", 2));

            add(form);

            binder.forField(name).asRequired().bind(OkTeamMember::getName, OkTeamMember::setName);
            binder.forField(team)
                  .asRequired()
                  .bind(m -> OkTeam.fromDb(m.getTeam()).orElse(null),
                        (m, v) -> m.setTeam(v != null ? v.toDb() : null));
            binder.forField(email).bind(OkTeamMember::getEmail, OkTeamMember::setEmail);

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

            var cancel = new Button("Abbrechen", _ -> close());

            getFooter().add(delete, cancel, save);
        }

        public void open(OkTeamMember member) {
            binder.setBean(member);
            super.open();
        }

        private boolean save() {
            if (binder.validate().isOk()) {
                var member = binder.getBean();
                if (member.getId() == null) {
                    okTeamMemberDao.insert(member);
                } else {
                    okTeamMemberDao.update(member);
                }
                close();
                return true;
            }
            return false;
        }

        private void delete() {
            var member = binder.getBean();
            if (member != null && member.getId() != null) {
                okTeamMemberDao.delete(member);
                close();
            }
        }
    }
}
