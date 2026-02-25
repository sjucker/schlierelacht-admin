package ch.schlierelacht.admin.service;

import static java.util.Comparator.comparing;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.TagDTO;
import ch.schlierelacht.admin.jooq.tables.daos.TagDao;
import ch.schlierelacht.admin.jooq.tables.pojos.Tag;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagDao tagDao;

    public List<TagDTO> findByAttractionType(AttractionType attractionType) {
        return tagDao.fetchByType(attractionType.toDb()).stream()
                     .sorted(comparing(Tag::getName))
                     .map(this::toDto)
                     .toList();
    }

    private TagDTO toDto(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName());
    }
}
