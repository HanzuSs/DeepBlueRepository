    # DeepBlue Rescue

    ## Descripcion

    DeepBlue Rescue es una capa de persistencia para una plataforma de rescate y rehabilitacion de fauna marina. Utiliza Java 21, Spring Boot 4, Spring Data JPA, Hibernate, PostgreSQL, Flyway y Testcontainers.

    El alcance se limita a persistencia: no incluye controladores REST, servicios, DTOs, seguridad ni frontend.

    ## Modelo de datos

    Tablas principales:

    - `rescue_centers`
    - `rescue_cases`
    - `animals`
    - `medical_records`
    - `specialists`
    - `expertise`
    - `treatments`

    Tabla asociativa:

    - `specialist_expertise`

    Restricciones relevantes:

    - claves primarias autogeneradas;
    - codigos y correos unicos;
    - claves foraneas entre entidades relacionadas;
    - relaciones 1:1 garantizadas con restricciones `UNIQUE`;
    - estados de rescate protegidos por un `CHECK`;
    - `tracking_device_code` opcional y unico para animales.

    ## Relaciones

    ```text
    RescueCenter 1:N RescueCase
    RescueCase 1:1 Animal
    Animal 1:1 MedicalRecord
    Animal 1:N Treatment
    Specialist 1:N Treatment
    Specialist N:M Expertise
    ```

    La relacion N:M utiliza `specialist_expertise`. `Treatment` mantiene las claves foraneas hacia `Animal` y `Specialist`.

    ## Requisitos

    - Java 21
    - Docker Desktop en ejecucion para Testcontainers
    - Maven Wrapper incluido (`mvnw.cmd` en Windows y `mvnw` en Linux/macOS)

    ## Ejecucion

    Desde la carpeta `deepblue-rescue/`:

    Windows:

    ```powershell
    .\mvnw.cmd spring-boot:run
    ```

    Linux/macOS:

    ```bash
    ./mvnw spring-boot:run
    ```

    La aplicacion espera PostgreSQL en `localhost:5432` si se ejecuta fuera de los tests. La conexion puede configurarse mediante `DB_URL`, `DB_USER` y `DB_PASSWORD`.

    ## Tests

    Windows:

    ```powershell
    .\mvnw.cmd clean test
    ```

    Linux/macOS:

    ```bash
    ./mvnw clean test
    ```

    Los tests de integracion crean temporalmente PostgreSQL 18 con Testcontainers. Para ejecutarlos, `JAVA_HOME` debe apuntar a un JDK 21 y Docker debe estar disponible.

    ## Flyway

    Flyway crea y evoluciona el esquema mediante migraciones versionadas:

    - `V1__create_schema.sql`: crea tablas, claves, restricciones e indices.
    - `V2__insert_expertise_catalog.sql`: inserta el catalogo inicial de expertise.
    - `V3__add_tracking_device_to_animal.sql`: agrega el codigo opcional y unico de dispositivo GPS.

    Hibernate utiliza `ddl-auto: validate`, por lo que valida que el modelo JPA coincida con el esquema, pero no crea ni modifica las tablas.

    ## Testcontainers

    Los tests de persistencia usan `PostgreSQLContainer` con la imagen `postgres:18-alpine` y `@ServiceConnection`. Spring Boot obtiene automaticamente la URL, usuario y contrasena del contenedor. Asi, las pruebas ejecutan migraciones, consultas y constraints contra PostgreSQL real en lugar de H2.

    ## Query Methods

    Implementados en los repositories:

    - `RescueCenterRepository.findByCode`
    - `RescueCaseRepository.findByCaseCode`
    - `RescueCaseRepository.findByStatusOrderByRescueDateAsc`
    - `RescueCaseRepository.findByRescueCenterCode`
    - `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc`
    - `AnimalRepository.findByAnimalCode`
    - `AnimalRepository.findByCommonNameContainingIgnoreCase`
    - `AnimalRepository.findByRescueCaseStatus`
    - `AnimalRepository.findByRescueCaseRescueCenterCode`
    - `ExpertiseRepository.findByNameIgnoreCase`
    - `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc`

    ## Consultas JPQL

    - Especialistas activos por expertise, con `JOIN`, `LOWER`, parametro nombrado y orden por nombre.
    - Tratamientos realizados entre dos fechas.
    - Tratamientos de animales pertenecientes a un centro.
    - Tratamientos realizados por especialistas con una expertise determinada, usando `DISTINCT`.
    - Animales en un estado de rescate que recibieron tratamientos de especialistas con una expertise determinada, usando `DISTINCT`.

    Todas las consultas personalizadas utilizan JPQL; no se usa SQL nativo en los repositories.
