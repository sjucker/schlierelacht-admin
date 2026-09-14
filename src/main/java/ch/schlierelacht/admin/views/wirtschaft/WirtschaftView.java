package ch.schlierelacht.admin.views.wirtschaft;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.daos.LocationDao;
import ch.schlierelacht.admin.jooq.tables.daos.ProgrammDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Attraction;
import ch.schlierelacht.admin.jooq.tables.pojos.Location;
import ch.schlierelacht.admin.jooq.tables.pojos.Programm;
import ch.schlierelacht.admin.service.AttractionFileService;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.AbstractAttractionView;
import ch.schlierelacht.admin.views.MainLayout;
import ch.schlierelacht.admin.views.util.DatePickerUtil;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static ch.schlierelacht.admin.views.util.DatePickerUtil.SWISS_LOCALE;
import static ch.schlierelacht.admin.views.util.NotificationUtil.showNotification;
import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

/**
 * Manages the Wirtschaft/Gewerbe events ({@link AttractionType#EVENT}). Reuses the attraction machinery
 * (name, description, images, files) from {@link AbstractAttractionView} and, via its extension hooks,
 * edits each event together with its single programm entry — the location (from the {@code location}
 * table, no free text) plus date/time. That single dialog is all that is needed to "aufschalten" an event.
 * <p>
 * The location and schedule live in the {@code programm} table (an event = an attraction joined with one
 * programm entry), exactly like the general programm; this view just surfaces the two together.
 */
@PageTitle("Wirtschaft/Gewerbe")
@Route(value = "wirtschaft", layout = MainLayout.class)
@PermitAll
public class WirtschaftView extends AbstractAttractionView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ProgrammDao programmDao;
    private final LocationDao locationDao;

    // Extra dialog fields: the single programm entry (location + date/time) belonging to the event.
    private final ComboBox<Location> location = new ComboBox<>("Ort");
    private final DatePicker fromDate = new DatePicker("Datum");
    private final TimePicker fromTime = new TimePicker("Von Zeit");
    private final DatePicker toDate = new DatePicker("Bis Datum");
    private final TimePicker toTime = new TimePicker("Bis Zeit");

    // The programm entry currently being edited (null while creating a brand-new event).
    private Programm currentEntry;

    // Lookups feeding the extra grid columns (Ort / Datum / Zeit), rebuilt on every grid refresh.
    private Map<Long, String> locationNames = new HashMap<>();
    private Map<Long, Programm> entryByAttraction = new HashMap<>();

    public WirtschaftView(AttractionDao attractionDao, CloudflareService cloudflareService,
                          DSLContext dslContext, AttractionFileService attractionFileService,
                          ProgrammDao programmDao, LocationDao locationDao) {
        super(attractionDao, cloudflareService, dslContext, attractionFileService);
        this.programmDao = programmDao;
        this.locationDao = locationDao;
    }

    @Override
    protected @NonNull Set<AttractionType> getAttractionTypes() {
        return Set.of(AttractionType.EVENT);
    }

    @Override
    protected @NonNull String getViewLabel() {
        return "Anlass";
    }

    // --- Grid: Ort / Tag / Datum / Zeit --------------------------------------------------------------

    @Override
    protected void prepareExtraColumnData(java.util.List<Attraction> attractions) {
        locationNames = new HashMap<>();
        locationDao.findAll().forEach(l -> locationNames.put(l.getId(), l.getName()));

        entryByAttraction = new HashMap<>();
        for (Attraction attraction : attractions) {
            programmDao.fetchByAttractionId(attraction.getId()).stream()
                       .min(entryOrder())
                       .ifPresent(entry -> entryByAttraction.put(attraction.getId(), entry));
        }
    }

    @Override
    protected void addExtraColumns(Grid<Attraction> grid) {
        grid.addColumn(a -> weekday(a)).setHeader("Tag").setSortable(true);
        grid.addColumn(a -> format(a, p -> p.getFromDate() != null ? p.getFromDate().format(DATE_FORMAT) : ""))
            .setHeader("Datum").setSortable(true);
        grid.addColumn(a -> format(a, p -> p.getFromTime() != null ? p.getFromTime().format(TIME_FORMAT) : ""))
            .setHeader("Zeit").setSortable(true);
        grid.addColumn(a -> format(a, p -> locationNames.getOrDefault(p.getLocationId(), "")))
            .setHeader("Ort").setSortable(true);
    }

    private String weekday(Attraction attraction) {
        var entry = entryByAttraction.get(attraction.getId());
        if (entry == null || entry.getFromDate() == null) {
            return "";
        }
        return entry.getFromDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);
    }

    private String format(Attraction attraction, java.util.function.Function<Programm, String> mapper) {
        var entry = entryByAttraction.get(attraction.getId());
        return entry != null ? mapper.apply(entry) : "";
    }

    // --- Dialog: location + date/time integrated into the attraction dialog --------------------------

    @Override
    protected void addExtraFormFields(FormLayout form) {
        location.setItems(locationDao.findAll());
        location.setItemLabelGenerator(Location::getName);
        location.setRequired(true);

        fromDate.setLocale(SWISS_LOCALE);
        toDate.setLocale(SWISS_LOCALE);
        fromTime.setLocale(SWISS_LOCALE);
        toTime.setLocale(SWISS_LOCALE);

        var datePickerI18n = DatePickerUtil.getGermanI18n();
        fromDate.setI18n(datePickerI18n);
        toDate.setI18n(datePickerI18n);

        form.add(location, fromDate, fromTime, toDate, toTime);
        form.setColspan(location, 2);
    }

    @Override
    protected void loadExtraFields(Attraction attraction) {
        currentEntry = attraction.getId() != null
                ? programmDao.fetchByAttractionId(attraction.getId()).stream().min(entryOrder()).orElse(null)
                : null;

        if (currentEntry != null) {
            location.setValue(currentEntry.getLocationId() != null ? locationDao.fetchOneById(currentEntry.getLocationId()) : null);
            fromDate.setValue(currentEntry.getFromDate());
            fromTime.setValue(currentEntry.getFromTime());
            toDate.setValue(currentEntry.getToDate());
            toTime.setValue(currentEntry.getToTime());
        } else {
            location.clear();
            fromDate.clear();
            fromTime.clear();
            toDate.clear();
            toTime.clear();
        }
    }

    @Override
    protected boolean validateExtraFields() {
        if (location.isEmpty() || fromDate.isEmpty()) {
            showNotification("Ort und Datum sind erforderlich", LUMO_WARNING);
            return false;
        }
        return true;
    }

    @Override
    protected void saveExtraFields(Long attractionId) {
        var entry = currentEntry != null ? currentEntry : new Programm();
        entry.setAttractionId(attractionId);
        entry.setLocationId(location.getValue().getId());
        entry.setFromDate(fromDate.getValue());
        entry.setFromTime(fromTime.getValue());
        entry.setToDate(toDate.getValue());
        entry.setToTime(toTime.getValue());

        if (entry.getId() == null) {
            programmDao.insert(entry);
        } else {
            programmDao.update(entry);
        }
        currentEntry = null;
    }

    @Override
    protected void deleteExtraFields(Long attractionId) {
        // Remove the event's programm entries first so the attraction row can be deleted.
        var entries = programmDao.fetchByAttractionId(attractionId);
        if (!entries.isEmpty()) {
            programmDao.delete(entries);
        }
    }

    private static Comparator<Programm> entryOrder() {
        return Comparator.comparing(Programm::getFromDate, nullsLast(naturalOrder()))
                         .thenComparing(Programm::getFromTime, nullsLast(naturalOrder()));
    }
}
