package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.FaqEntryDTO;
import ch.schlierelacht.admin.dto.FaqGroupDTO;
import ch.schlierelacht.admin.jooq.tables.pojos.Faq;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.schlierelacht.admin.jooq.Tables.FAQ;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

/**
 * The FAQ order — the one place it lives: entries are grouped by their <b>trimmed</b> group name (case matters);
 * groups come in the order of the lowest sort number of their <b>active</b> entries, then by name; entries by sort
 * number, then title. The app gets the active entries in this order, the admin grid all of them — groups without an
 * active entry come last there, so the active ones show exactly as in the app.
 */
@Service
@RequiredArgsConstructor
public class FaqService {

    private static final Comparator<Faq> ENTRY_ORDER = Comparator.comparing(Faq::getSortOrder)
                                                                  .thenComparing(Faq::getTitle, String.CASE_INSENSITIVE_ORDER)
                                                                  .thenComparing(Faq::getId, nullsLast(naturalOrder()));

    private static final Comparator<Map.Entry<String, List<Faq>>> GROUP_ORDER =
            Comparator.<Map.Entry<String, List<Faq>>, Integer>comparing(e -> lowestActiveSortOrder(e.getValue()), nullsLast(naturalOrder()))
                      .thenComparing(e -> e.getValue().getFirst().getSortOrder())
                      .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
                      .thenComparing(Map.Entry::getKey);

    private final DSLContext dslContext;

    /**
     * The active entries, grouped and ordered as the app shows them.
     */
    public List<FaqGroupDTO> findAllActive() {
        var active = dslContext.selectFrom(FAQ).where(FAQ.ACTIVE.isTrue()).fetchInto(Faq.class);
        return grouped(active).entrySet().stream()
                              .map(e -> new FaqGroupDTO(e.getKey(),
                                                        e.getValue().stream()
                                                         .map(f -> new FaqEntryDTO(f.getId(), f.getTitle(), f.getContent()))
                                                         .toList()))
                              .toList();
    }

    /**
     * All entries, active or not, in the app's order (for the admin grid).
     */
    public List<Faq> findAllInAppOrder() {
        return grouped(dslContext.selectFrom(FAQ).fetchInto(Faq.class)).values().stream()
                                                                       .flatMap(List::stream)
                                                                       .toList();
    }

    /**
     * The distinct (trimmed) group names, alphabetically.
     */
    public List<String> findGroupNames() {
        return dslContext.selectFrom(FAQ).fetchInto(Faq.class).stream()
                         .map(f -> f.getGroupName().trim())
                         .distinct()
                         .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(naturalOrder()))
                         .toList();
    }

    private static Map<String, List<Faq>> grouped(List<Faq> entries) {
        // entries are sorted first, so every group keeps ENTRY_ORDER and starts with its lowest sort number
        return entries.stream()
                      .sorted(ENTRY_ORDER)
                      .collect(Collectors.groupingBy(f -> f.getGroupName().trim(), LinkedHashMap::new, Collectors.toList()))
                      .entrySet().stream()
                      .sorted(GROUP_ORDER)
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, _) -> a, LinkedHashMap::new));
    }

    private static Integer lowestActiveSortOrder(List<Faq> sortedEntries) {
        return sortedEntries.stream()
                            .filter(f -> Boolean.TRUE.equals(f.getActive()))
                            .findFirst()
                            .map(Faq::getSortOrder)
                            .orElse(null);
    }
}
