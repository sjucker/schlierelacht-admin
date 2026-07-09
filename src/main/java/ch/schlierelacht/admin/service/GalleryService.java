package ch.schlierelacht.admin.service;

import ch.schlierelacht.admin.dto.GalleryCategoryDTO;
import ch.schlierelacht.admin.dto.GalleryImageDTO;
import ch.schlierelacht.admin.jooq.tables.daos.GalleryCategoryDao;
import ch.schlierelacht.admin.jooq.tables.pojos.GalleryCategory;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.schlierelacht.admin.jooq.Tables.GALLERY_CATEGORY;
import static ch.schlierelacht.admin.jooq.Tables.GALLERY_IMAGE;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.max;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final DSLContext dslContext;
    private final GalleryCategoryDao galleryCategoryDao;

    /**
     * Returns gallery images grouped by category. Category order is controlled by the admin via
     * {@code gallery_category.sort_order}; categories without a configured order sort last (alphabetically).
     * Within each category the newest image comes first.
     */
    public List<GalleryCategoryDTO> findGroupedByCategory() {
        // Fetch in final display order. A LinkedHashMap then preserves it: rows of one category are contiguous
        // (same sort_order + name), so first-seen order == category order, and each list stays newest-first.
        Map<String, List<GalleryImageDTO>> grouped = new LinkedHashMap<>();
        dslContext.select(GALLERY_IMAGE.CATEGORY, GALLERY_IMAGE.CLOUDFLARE_ID)
                  .from(GALLERY_IMAGE)
                  .leftJoin(GALLERY_CATEGORY).on(GALLERY_CATEGORY.NAME.eq(GALLERY_IMAGE.CATEGORY))
                  .orderBy(GALLERY_CATEGORY.SORT_ORDER.asc().nullsLast(),
                           GALLERY_IMAGE.CATEGORY.asc(),
                           GALLERY_IMAGE.UPLOADED_AT.desc())
                  .fetch()
                  .forEach(r -> grouped.computeIfAbsent(r.get(GALLERY_IMAGE.CATEGORY), _ -> new ArrayList<>())
                                       .add(new GalleryImageDTO(r.get(GALLERY_IMAGE.CLOUDFLARE_ID))));

        return grouped.entrySet().stream()
                      .map(e -> new GalleryCategoryDTO(e.getKey(), e.getValue()))
                      .toList();
    }

    /** Categories in admin-defined order, for the ordering grid in the admin view. */
    public List<GalleryCategory> listCategoriesOrdered() {
        return dslContext.selectFrom(GALLERY_CATEGORY)
                         .orderBy(GALLERY_CATEGORY.SORT_ORDER.asc(), GALLERY_CATEGORY.NAME.asc())
                         .fetchInto(GalleryCategory.class);
    }

    /** Registers a (free-text) category for ordering if it does not exist yet, appending it at the end. */
    public void ensureCategory(String name) {
        dslContext.insertInto(GALLERY_CATEGORY)
                  .set(GALLERY_CATEGORY.NAME, name)
                  .set(GALLERY_CATEGORY.SORT_ORDER, nextSortOrder())
                  .onConflict(GALLERY_CATEGORY.NAME).doNothing()
                  .execute();
    }

    @Transactional
    public void moveUp(long categoryId) {
        swapWithNeighbour(categoryId, -1);
    }

    @Transactional
    public void moveDown(long categoryId) {
        swapWithNeighbour(categoryId, 1);
    }

    private void swapWithNeighbour(long categoryId, int direction) {
        var ordered = listCategoriesOrdered();
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId() == categoryId) {
                idx = i;
                break;
            }
        }
        int swapIdx = idx + direction;
        if (idx < 0 || swapIdx < 0 || swapIdx >= ordered.size()) {
            return;
        }
        var current = ordered.get(idx);
        var neighbour = ordered.get(swapIdx);
        int tmp = current.getSortOrder();
        current.setSortOrder(neighbour.getSortOrder());
        neighbour.setSortOrder(tmp);
        galleryCategoryDao.update(current, neighbour);
    }

    private int nextSortOrder() {
        return dslContext.select(coalesce(max(GALLERY_CATEGORY.SORT_ORDER), -1).add(1))
                         .from(GALLERY_CATEGORY)
                         .fetchOneInto(int.class);
    }
}
