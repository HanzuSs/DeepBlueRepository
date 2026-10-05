package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.AnimalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;

    private final AnimalMapper mapper;

    public AnimalServiceImpl(AnimalRepository animalRepository,
                             AnimalMapper mapper) {
        this.animalRepository = animalRepository;
        this.mapper = mapper;
    }

    @Override
    public AnimalResponse findByCode(Long animalId) {
        return animalRepository
                .findById(animalId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + animalId));
    }

    @Override
    public List<AnimalResponse> findAnimalsInRehabilitation() {
        return animalRepository
                .findByRescueCaseStatus(
                        RescueStatus.IN_REHABILITATION)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public boolean canReceiveTreatment(Long animalId) {
        Animal animal = animalRepository
                .findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + animalId));

        RescueStatus status = animal.getRescueCase().getStatus();

        return status == RescueStatus.UNDER_EVALUATION
                || status == RescueStatus.IN_REHABILITATION;
    }
}