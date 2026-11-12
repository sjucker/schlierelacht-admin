package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.AbstractIntegrationTest;
import ch.schlierelacht.admin.dto.FaqEntryDTO;
import ch.schlierelacht.admin.dto.FaqGroupDTO;
import ch.schlierelacht.admin.jooq.tables.pojos.Faq;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static ch.schlierelacht.admin.jooq.Tables.FAQ;
import static org.assertj.core.api.Assertions.assertThat;

class FaqServiceTest extends AbstractIntegrationTest {

    private final FaqService faqService;
    private final DSLContext dslContext;

    @Autowired
    FaqServiceTest(FaqService faqService, DSLContext dslContext) {
        this.faqService = faqService;
        this.dslContext = dslContext;
    }

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(FAQ).execute();
    }

    @Test
    void findAllActive_groupedAndOrdered() {
        insert("Verpflegung", "Gibt es vegane Angebote?", 20, true);
        insert("Anreise", "Wo kann ich parkieren?", 10, true);
        insert("Anreise ", "Wie komme ich mit dem ÖV?", 5, true); // trailing space: same group
        insert("Verpflegung", "Kann ich mit Karte zahlen?", 1, true);
        insert("Anreise", "Bahnhof oder Bus?", 10, true); // same number: by title

        var groups = faqService.findAllActive();

        // Verpflegung's lowest number (1) is smaller than Anreise's (5)
        assertThat(groups).extracting(FaqGroupDTO::name).containsExactly("Verpflegung", "Anreise");
        assertThat(groups.get(0).entries()).extracting(FaqEntryDTO::title)
                                          .containsExactly("Kann ich mit Karte zahlen?", "Gibt es vegane Angebote?");
        assertThat(groups.get(1).entries()).extracting(FaqEntryDTO::title)
                                          .containsExactly("Wie komme ich mit dem ÖV?", "Bahnhof oder Bus?", "Wo kann ich parkieren?");
        assertThat(groups.get(1).entries().getFirst().content()).isEqualTo("Antwort auf **Wie komme ich mit dem ÖV?**");
    }

    @Test
    void findAllActive_skipsInactive() {
        insert("Anreise", "Entwurf", 1, false);
        insert("Hunde", "Sind Hunde erlaubt?", 1, true);
        insert("Hunde", "Noch nicht fertig", 2, false);

        assertThat(faqService.findAllActive())
                .singleElement()
                .satisfies(g -> {
                    assertThat(g.name()).isEqualTo("Hunde");
                    assertThat(g.entries()).extracting(FaqEntryDTO::title).containsExactly("Sind Hunde erlaubt?");
                });
    }

    @Test
    void findAllActive_none() {
        insert("Anreise", "Entwurf", 1, false);

        assertThat(faqService.findAllActive()).isEmpty();
    }

    @Test
    void groupOrder_ignoresInactiveEntries() {
        insert("Hunde", "Entwurf", 0, false);
        insert("Hunde", "Sind Hunde erlaubt?", 50, true);
        insert("Anreise", "Wo kann ich parkieren?", 10, true);
        insert("Abfall", "Nur ein Entwurf", 1, false); // no active entry: last in the grid

        assertThat(faqService.findAllActive()).extracting(FaqGroupDTO::name).containsExactly("Anreise", "Hunde");
        assertThat(faqService.findAllInAppOrder()).extracting(Faq::getTitle)
                                                  .containsExactly("Wo kann ich parkieren?", "Entwurf", "Sind Hunde erlaubt?", "Nur ein Entwurf");
    }

    @Test
    void groupsDifferingInCase_stayApart() {
        insert("anreise", "Klein 1", 10, true);
        insert("Anreise", "Gross 1", 10, true);
        insert("anreise", "Klein 2", 20, true);

        assertThat(faqService.findAllActive()).extracting(FaqGroupDTO::name).containsExactly("Anreise", "anreise");
        assertThat(faqService.findAllInAppOrder()).extracting(Faq::getTitle).containsExactly("Gross 1", "Klein 1", "Klein 2");
        assertThat(faqService.findGroupNames()).containsExactly("Anreise", "anreise");
    }

    private void insert(String group, String title, int sortOrder, boolean active) {
        dslContext.insertInto(FAQ)
                  .set(FAQ.GROUP_NAME, group)
                  .set(FAQ.TITLE, title)
                  .set(FAQ.CONTENT, "Antwort auf **" + title + "**")
                  .set(FAQ.SORT_ORDER, sortOrder)
                  .set(FAQ.ACTIVE, active)
                  .execute();
    }
}
