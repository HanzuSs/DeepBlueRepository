package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;

    private final SpecialistRepository specialistRepository;

    private final TreatmentRepository treatmentRepository;

    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(AnimalRepository animalRepository,
                                SpecialistRepository specialistRepository,
                                TreatmentRepository treatmentRepository,
                                TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // 1. Regla 1: el animal debe existir
        Animal animal = animalRepository
                .findById(request.animalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal not found: " + request.animalId()));

        // 2. Regla 2: el especialista debe existir
        Specialist specialist = specialistRepository
                .findById(request.specialistId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist not found: " + request.specialistId()));

        // 3. Regla 3: el especialista debe estar activo
        if (!specialist.isActive()) {
            throw new BusinessRuleException(
                    "Cannot register treatment because specialist "
                            + request.specialistId() + " is not active.");
        }

        // 4. Caso de rescate del animal
        RescueCase rescueCase = animal.getRescueCase();

        // 5. Regla 4: el caso no puede estar RELEASED ni CLOSED
        RescueStatus caseStatus = rescueCase.getStatus();
        if (caseStatus == RescueStatus.RELEASED
                || caseStatus == RescueStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Cannot register treatment because the rescue case is "
                            + caseStatus + ".");
        }

        // 6. Regla 5: el tratamiento no puede ser anterior al rescate.
        // rescueDate es LocalDate: se compara por dia, de modo que un
        // tratamiento el mismo dia del rescate es valido.
        if (request.performedAt().toLocalDate()
                .isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Cannot register treatment before the rescue date ("
                            + rescueCase.getRescueDate() + ").");
        }

        // 7. Crear Treatment
        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description());

        // 8. Guardar
        Treatment saved = treatmentRepository.save(treatment);

        // 9. Mapear a DTO
        return mapper.toResponse(saved);
    }

    @Override
    public List<TreatmentResponse> findByAnimalId(Long animalid) {
        return treatmentRepository
                .findByAnimalIdOrderByPerformedAtAsc(animalid)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}