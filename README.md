    # DeepBlue Rescue

    # Estudiantes : Hanzu Vives 2024214064; Samuel Almanza 2024214061


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


    ## Respuestas a las preguntas
    -1. 
 JPA: Especificación (interfaz estándar) de Java para persistencia.
 Hibernate: Proveedor/implementación concreta que ejecuta JPA.
 Spring Data JPA: Capa de abstracción de Spring que simplifica el uso de repositorios JPA.
 PostgreSQL: Sistema de gestión de bases de datos relacional físico.
2. ¿Qué componente crea las tablas?
El motor de base de datos (PostgreSQL), ejecutado mediante esquemas generados por Hibernate o scripts de migración.
3. ¿Qué componente ejecuta las migraciones?
Herramientas especializadas como Flyway o Liquibase.
4. ¿Qué hace ddl-auto=validate?
Comprueba que el esquema actual de la base de datos coincida con las entidades de Java, lanzando un error al iniciar si hay diferencias (no modifica la BD).
5. ¿Qué significa mappedBy?
Indica el atributo de la entidad inversa que posee la relación, señalando que el lado actual es el inverso (no propietario) y no crea la FK.
6. ¿Cómo identificas al propietario de una relación?
Es el lado que no tiene el atributo mappedBy (en relaciones @OneToMany/@ManyToOne, siempre es el lado @ManyToOne).
7. ¿Dónde está físicamente la FK de RescueCenter 1:N RescueCase?
En la tabla de la entidad del lado "N" (RescueCase).
8. ¿Qué permite que Animal 1:1 MedicalRecord sea realmente 1:1 en PostgreSQL?
Una restricción de unicidad (UNIQUE) en la columna de la llave foránea de la tabla dependiente.
9. ¿Por qué Specialist N:M Expertise requiere una tabla intermedia?
Porque las bases de datos relacionales no pueden almacenar múltiples valores en una sola columna; la tabla intermedia mapea las combinaciones de ambas entidades.
10. Diferencia entre findById() y findByCaseCode()
  findById(): Busca por la llave primaria (@Id) de la entidad.
  findByCaseCode(): Es un Query Method dinámico que busca por un atributo de negocio (caseCode).
11. ¿Qué es un Query Method?
Un método declarado en un repositorio cuya nomenclatura es interpretada automáticamente por Spring Data JPA para construir la consulta.
12. ¿Qué significa navegar asociaciones mediante findByRescueCaseRescueCaseCode(...)?
Permite hacer consultas cruzadas (joins implícitos) atravesando propiedades anidadas de entidades relacionadas.
13. ¿Qué es @Query?
Una anotación para definir consultas personalizadas (en JPQL o SQL nativo) de forma explícita en un repositorio.
14. ¿Qué es JPQL?
Java Persistence Query Language; un lenguaje de consultas orientado a objetos que opera sobre las entidades de Java en lugar de las tablas de la base de datos.
15. ¿Por qué JPQL utiliza Specialist en vez de specialists?
Porque JPQL trabaja con los nombres de las clases de entidad en Java, no con los nombres físicos de las tablas de la base de datos.
16. Diferencia entre save() y saveAndFlush()
  save(): Guarda la entidad en el contexto de persistencia (sincroniza al final de la transacción).
  saveAndFlush(): Fuerza la escritura y ejecución inmediata del SQL en la base de datos en ese preciso instante.
17. ¿Por qué probamos constraints con PostgreSQL y no con Java?
Porque PostgreSQL valida reglas a nivel de motor real (como integridad referencial y restricciones a nivel de BD) que una simulación en memoria no garantiza al 100%.
18. ¿Por qué Testcontainers es útil?
Porque permite levantar servicios reales (como una base de datos PostgreSQL en Docker) durante las pruebas automatizadas, asegurando un entorno idéntico al de producción.