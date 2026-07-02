package ch.schlierelacht.admin.views.artist;

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

import java.util.Set;

@Slf4j
@PageTitle("Künstler")
@Route(value = "artists", layout = MainLayout.class)
@PermitAll
public class ArtistView extends AbstractAttractionView {

    public ArtistView(AttractionDao attractionDao, TagDao tagDao, CloudflareService cloudflareService,
                      DSLContext dslContext, AttractionFileService attractionFileService) {
        super(attractionDao, tagDao, cloudflareService, dslContext, attractionFileService);
    }

    @Override
    protected @NonNull Set<AttractionType> getAttractionTypes() {
        return Set.of(AttractionType.ARTIST);
    }
}
