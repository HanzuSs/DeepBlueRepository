-- V3__add_tracking_device_to_animal.sql
-- Código opcional y único para dispositivos GPS de animales.
ALTER TABLE animals
    ADD COLUMN tracking_device_code VARCHAR(50),
    ADD CONSTRAINT uk_animal_tracking_device_code UNIQUE (tracking_device_code);
