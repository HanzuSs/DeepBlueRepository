-- ============================================================
-- V1__create_schema.sql
-- Deep Blue Rescue - creación de tablas
-- ============================================================

-- 1. rescue_centers (entidad, lado "1" de RescueCenter -> RescueCase)
CREATE TABLE rescue_centers (
    id     BIGSERIAL    PRIMARY KEY,
    code   VARCHAR(50)  NOT NULL,
    name   VARCHAR(150) NOT NULL,
    city   VARCHAR(100) NOT NULL,
    CONSTRAINT uk_rescue_center_code UNIQUE (code)
);

-- 2. rescue_cases (entidad, lado "N" respecto a rescue_centers)
CREATE TABLE rescue_cases (
    id                BIGSERIAL    PRIMARY KEY,
    case_code         VARCHAR(50)  NOT NULL,
    rescue_date       DATE         NOT NULL,
    rescue_location   VARCHAR(200) NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    rescue_center_id  BIGINT       NOT NULL,
    CONSTRAINT uk_rescue_case_code UNIQUE (case_code),
    CONSTRAINT fk_rescue_case_center
        FOREIGN KEY (rescue_center_id) REFERENCES rescue_centers (id),
    CONSTRAINT chk_rescue_case_status
        CHECK (status IN (
            'ADMITTED',
            'UNDER_EVALUATION',
            'IN_REHABILITATION',
            'READY_FOR_RELEASE',
            'RELEASED',
            'CLOSED'
        ))
);

-- 3. animals (entidad, 1:1 con rescue_cases -> FK + UNIQUE)
CREATE TABLE animals (
    id               BIGSERIAL PRIMARY KEY,
    animal_code      VARCHAR(50)  NOT NULL UNIQUE,
    common_name      VARCHAR(100) NOT NULL,
    scientific_name  VARCHAR(150),
    sex              VARCHAR(20)  NOT NULL,
    rescue_case_id   BIGINT       NOT NULL UNIQUE,
    CONSTRAINT fk_animal_rescue_case
        FOREIGN KEY (rescue_case_id) REFERENCES rescue_cases (id)
);

-- 4. medical_records (entidad, 1:1 con animals -> FK + UNIQUE)
CREATE TABLE medical_records (
    id                 BIGSERIAL PRIMARY KEY,
    animal_id          BIGINT      NOT NULL UNIQUE,
    initial_weight     DECIMAL(8,2),
    initial_condition  VARCHAR(100),
    injuries           TEXT,
    observations       TEXT,
    CONSTRAINT fk_medical_record_animal
        FOREIGN KEY (animal_id) REFERENCES animals (id)
);

-- 5. specialists (entidad)
CREATE TABLE specialists (
    id                  BIGSERIAL PRIMARY KEY,
    professional_code   VARCHAR(50)  NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(150) NOT NULL UNIQUE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 6. expertise (entidad, catálogo)
CREATE TABLE expertise (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

-- 7. specialist_expertise (tabla asociativa N:M)
CREATE TABLE specialist_expertise (
    specialist_id  BIGINT NOT NULL,
    expertise_id   BIGINT NOT NULL,
    PRIMARY KEY (specialist_id, expertise_id),
    CONSTRAINT fk_se_specialist
        FOREIGN KEY (specialist_id) REFERENCES specialists (id),
    CONSTRAINT fk_se_expertise
        FOREIGN KEY (expertise_id) REFERENCES expertise (id)
);

-- 8. treatments (entidad, "N" respecto a animals y a specialists)
CREATE TABLE treatments (
    id             BIGSERIAL PRIMARY KEY,
    animal_id      BIGINT       NOT NULL,
    specialist_id  BIGINT       NOT NULL,
    performed_at   TIMESTAMP    NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    description    TEXT,
    CONSTRAINT fk_treatment_animal
        FOREIGN KEY (animal_id) REFERENCES animals (id),
    CONSTRAINT fk_treatment_specialist
        FOREIGN KEY (specialist_id) REFERENCES specialists (id)
);

-- ============================================================
-- Índices
-- ============================================================
CREATE INDEX idx_rescue_case_center ON rescue_cases (rescue_center_id);
CREATE INDEX idx_rescue_case_status ON rescue_cases (status);
CREATE INDEX idx_rescue_case_date ON rescue_cases (rescue_date);

CREATE INDEX idx_treatment_animal ON treatments (animal_id);
CREATE INDEX idx_treatment_specialist ON treatments (specialist_id);
CREATE INDEX idx_treatment_performed_at ON treatments (performed_at);