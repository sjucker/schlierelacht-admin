package ch.schlierelacht.admin.views.attraction;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.daos.TagDao;
import ch.schlierelacht.admin.service.AttractionFileService;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.AbstractAttractionView;
import ch.schlierelacht.admin.views.MainLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;

/**
 * Manages every attraction type except {@link AttractionType#ARTIST} and {@link AttractionType#FOOD}, which have their
 * own dedicated views. The type is picked per attraction via the dialog's type selector. Deriving the managed set from
 * the enum means any future type shows up here automatically unless it gets its own view.
 */
@Slf4j
@PageTitle("Attraktionen")
@Route(value = "attractions", layout = MainLayout.class)
@PermitAll
public class AttractionView extends AbstractAttractionView {

    private static final Set<AttractionType> EXCLUDED = Set.of(AttractionType.ARTIST, AttractionType.FOOD);

    public AttractionView(AttractionDao attractionDao, TagDao tagDao, CloudflareService cloudflareService,
                          DSLContext dslContext, AttractionFileService attractionFileService) {
        super(attractionDao, tagDao, cloudflareService, dslContext, attractionFileService);
    }

    @Override
    protected @NonNull Set<AttractionType> getAttractionTypes() {
        return Arrays.stream(AttractionType.values())
                     .filter(type -> !EXCLUDED.contains(type))
                     .collect(toCollection(LinkedHashSet::new));
    }

    @Override
    protected @NonNull String getViewLabel() {
        return "Attraktionen";
    }
}
