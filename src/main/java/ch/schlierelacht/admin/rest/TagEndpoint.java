package ch.schlierelacht.admin.rest;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.schlierelacht.admin.dto.AttractionType;
import ch.schlierelacht.admin.dto.TagDTO;
import ch.schlierelacht.admin.service.TagService;

@Slf4j
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagEndpoint {

    private final TagService tagService;

    @GetMapping("/{attractionType}")
    public List<TagDTO> findByAttractionType(@PathVariable AttractionType attractionType) {
        log.info("GET /api/tag/{}", attractionType);

        return tagService.findByAttractionType(attractionType);
    }
}
