package ch.schlierelacht.admin.views.gastro;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.jooq.tables.daos.AttractionDao;
import ch.schlierelacht.admin.jooq.tables.daos.TagDao;
import ch.schlierelacht.admin.service.CloudflareService;
import ch.schlierelacht.admin.views.AbstractAttractionView;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;

@PageTitle("Gastronomie")
@Route("food")
@PermitAll
public class GastroView extends AbstractAttractionView {

    public GastroView(AttractionDao attractionDao, TagDao tagDao, CloudflareService cloudflareService, DSLContext dslContext) {
        super(attractionDao, tagDao, cloudflareService, dslContext);
    }

    @Override
    protected @NonNull AttractionType getAttractionType() {
        return AttractionType.FOOD;
    }
}
