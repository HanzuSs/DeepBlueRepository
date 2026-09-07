package com.deepblue.rescue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

// TODO: definir <Entidad, TipoId> según el modelado (Parte II)
public interface TreatmentRepository extends JpaRepository<Object, Long> {
}
