package ch.schlierelacht.admin.views.util;

import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;

import java.util.Arrays;

public class DatePickerUtil {

    public static DatePickerI18n getGermanI18n() {
        var datePickerI18n = new DatePickerI18n();
        datePickerI18n.setMonthNames(Arrays.asList("Januar", "Februar", "März", "April", "Mai", "Juni",
                                                   "Juli", "August", "September", "Oktober", "November", "Dezember"));
        datePickerI18n.setWeekdays(Arrays.asList("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"));
        datePickerI18n.setWeekdaysShort(Arrays.asList("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"));
        datePickerI18n.setToday("Heute");
        datePickerI18n.setCancel("Abbrechen");
        datePickerI18n.setFirstDayOfWeek(1); // Monday
        return datePickerI18n;
    }
}
