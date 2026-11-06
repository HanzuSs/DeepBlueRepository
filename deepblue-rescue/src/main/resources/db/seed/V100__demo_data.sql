-- ============================================================
-- Datos de prueba (solo para Docker / demo)
-- Se carga únicamente si se activa la ubicación classpath:db/seed
-- (ver docker-compose.yml). Los tests y el perfil normal NO lo usan.
-- ============================================================

-- Centros de rescate
INSERT INTO rescue_centers (code, name, city) VALUES
    ('DB-CAR', 'DeepBlue Cartagena',   'Cartagena'),
    ('DB-SMR', 'DeepBlue Santa Marta', 'Santa Marta');

-- Especialistas (SPEC-003 está inactivo para probar el 409)
INSERT INTO specialists (professional_code, first_name, last_name, email, active) VALUES
    ('SPEC-001', 'Laura',  'Gómez', 'laura.gomez@deepblue.org',  TRUE),
    ('SPEC-002', 'Andrés', 'Rojas', 'andres.rojas@deepblue.org', TRUE),
    ('SPEC-003', 'Marta',  'Díaz',  'marta.diaz@deepblue.org',   FALSE);

-- Especialidades de cada especialista (el catálogo viene de V2)
INSERT INTO specialist_expertise (specialist_id, expertise_id)
SELECT s.id, e.id
FROM specialists s
JOIN expertise e ON e.name IN ('Marine Reptiles', 'Trauma')
WHERE s.professional_code = 'SPEC-001';

INSERT INTO specialist_expertise (specialist_id, expertise_id)
SELECT s.id, e.id
FROM specialists s
JOIN expertise e ON e.name IN ('Marine Mammals', 'Rehabilitation')
WHERE s.professional_code = 'SPEC-002';

INSERT INTO specialist_expertise (specialist_id, expertise_id)
SELECT s.id, e.id
FROM specialists s
JOIN expertise e ON e.name = 'Toxicology'
WHERE s.professional_code = 'SPEC-003';

-- Casos de rescate: uno por cada estado
INSERT INTO rescue_cases (case_code, rescue_date, rescue_location, status, rescue_center_id) VALUES
    ('RES-2026-001', DATE '2026-08-20', 'Bahía Concha',        'IN_REHABILITATION', (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-002', DATE '2026-09-02', 'Playa Salguero',      'ADMITTED',          (SELECT id FROM rescue_centers WHERE code = 'DB-SMR')),
    ('RES-2026-003', DATE '2026-09-15', 'Isla Tierra Bomba',   'UNDER_EVALUATION',  (SELECT id FROM rescue_centers WHERE code = 'DB-CAR')),
    ('RES-2026-004', DATE '2026-07-10', 'Taganga',             'READY_FOR_RELEASE', (SELECT id FROM rescue_centers WHERE code = 'DB-SMR')),
    ('RES-2026-005', DATE '2026-06-01', 'Playa Grande',        'RELEASED',          (SELECT id FROM rescue_centers WHERE code = 'DB-SMR')),
    ('RES-2026-006', DATE '2026-05-12', 'Bocagrande',          'CLOSED',            (SELECT id FROM rescue_centers WHERE code = 'DB-CAR'));

-- Un animal por caso
INSERT INTO animals (animal_code, common_name, scientific_name, sex, tracking_device_code, rescue_case_id) VALUES
    ('AN-2026-001', 'Green sea turtle',       'Chelonia mydas',          'FEMALE',  'TRK-001', (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-001')),
    ('AN-2026-002', 'Loggerhead sea turtle',  'Caretta caretta',         'FEMALE',  NULL,      (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-002')),
    ('AN-2026-003', 'Brown pelican',          'Pelecanus occidentalis',  'UNKNOWN', NULL,      (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-003')),
    ('AN-2026-004', 'Hawksbill sea turtle',   'Eretmochelys imbricata',  'MALE',    'TRK-004', (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-004')),
    ('AN-2026-005', 'West Indian manatee',    'Trichechus manatus',      'FEMALE',  'TRK-005', (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-005')),
    ('AN-2026-006', 'Common bottlenose dolphin', 'Tursiops truncatus',   'MALE',    NULL,      (SELECT id FROM rescue_cases WHERE case_code = 'RES-2026-006'));

-- Registro médico inicial de cada animal
INSERT INTO medical_records (animal_id, initial_weight, initial_condition, injuries, observations) VALUES
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-001'),  48.50, 'Debilitada',  'Herida en aleta delantera izquierda', 'Enredada en red de pesca'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-002'),  62.00, 'Estable',     NULL,                                  'Sin lesiones visibles'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-003'),   2.80, 'Deshidratado', 'Ala derecha con fractura leve',       'Encontrado en la playa'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-004'),  35.20, 'Recuperado',  'Cicatriz en caparazón',               'Listo para liberación'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-005'), 410.00, 'Estable',     NULL,                                  'Liberado en junio'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-006'), 180.00, 'Estable',     NULL,                                  'Caso cerrado');

-- Tratamientos (todos posteriores a la fecha de rescate de su caso)
INSERT INTO treatments (animal_id, specialist_id, performed_at, type, description) VALUES
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-001'), (SELECT id FROM specialists WHERE professional_code = 'SPEC-001'), TIMESTAMP '2026-08-21 09:30:00', 'WOUND_CARE',    'Cleaning and evaluation of left front flipper injury.'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-001'), (SELECT id FROM specialists WHERE professional_code = 'SPEC-002'), TIMESTAMP '2026-08-25 14:00:00', 'MEDICATION',    'Antibiotic course started after wound culture.'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-003'), (SELECT id FROM specialists WHERE professional_code = 'SPEC-002'), TIMESTAMP '2026-09-16 10:00:00', 'OBSERVATION',   'Daily observation of the injured right wing.'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-004'), (SELECT id FROM specialists WHERE professional_code = 'SPEC-001'), TIMESTAMP '2026-07-12 11:00:00', 'NUTRITION',     'Supervised feeding plan to recover body weight.'),
    ((SELECT id FROM animals WHERE animal_code = 'AN-2026-004'), (SELECT id FROM specialists WHERE professional_code = 'SPEC-002'), TIMESTAMP '2026-07-20 16:30:00', 'PHYSIOTHERAPY', 'Swimming exercises to restore flipper mobility.');
