package ch.schlierelacht.admin.service;

import static ch.schlierelacht.admin.dto.AttractionType.FOOD;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import ch.schlierelacht.admin.dto.AttractionDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class GastroService {
    private final AttractionService attractionService;

    public List<AttractionDTO> findAll() {
        return attractionService.findAll(FOOD);
    }

    public List<AttractionDTO> findByTagId(Long tagId) {
        return attractionService.findByTagId(FOOD, tagId);
    }

    public Optional<AttractionDTO> findByExternalId(String externalId) {
        return attractionService.findByExternalId(FOOD, externalId);
    }
}
