# Implementación

# Estructura del Repositorio - CBMM System

## Estructura General del Proyecto

```
cbmm-system/
├── .gitignore
├── LICENSE
├── Makefile
├── README.md
├── design.md
├── implementation.md
├── docker-compose.yml
├── resources/
│   ├── Challenge.pdf
│   └── diagrams/
│       ├── datamodel.png
│       ├── sequence.png
│       ├── initial.png
│       ├── intermediate.png
│       └── final.png
└── cbmm-processor/
    ├── Dockerfile
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/com/processor/
    │   │   │   ├── CbmmSystemApplication.java
    │   │   │   ├── application/
    │   │   │   │   └── service/
    │   │   │   │       └── CbmmTransactionApplicationService.java
    │   │   │   ├── core/
    │   │   │   │   ├── domain/
    │   │   │   │   │   ├── enums/
    │   │   │   │   │   │   ├── ProcessingStatus.java
    │   │   │   │   │   │   ├── TransactionStatus.java
    │   │   │   │   │   │   └── TransactionType.java
    │   │   │   │   │   ├── exception/
    │   │   │   │   │   │   ├── AccountNotFoundException.java
    │   │   │   │   │   │   ├── InsufficientFundsException.java
    │   │   │   │   │   │   ├── InvalidCurrencyException.java
    │   │   │   │   │   │   └── TransactionProcessingException.java
    │   │   │   │   │   ├── model/
    │   │   │   │   │   │   ├── Account.java
    │   │   │   │   │   │   └── Transaction.java
    │   │   │   │   │   └── value_object/
    │   │   │   │   │       ├── TransactionData.java
    │   │   │   │   │       ├── TransactionResult.java
    │   │   │   │   │       └── TransferAccount.java
    │   │   │   │   ├── ports/
    │   │   │   │   │   ├── in/
    │   │   │   │   │   │   └── ProcessCbmmTransactionUseCase.java
    │   │   │   │   │   └── out/
    │   │   │   │   │       ├── AccountRepository.java
    │   │   │   │   │       ├── IdempotencyChecker.java
    │   │   │   │   │       └── TransactionRepository.java
    │   │   │   │   └── use_case/
    │   │   │   │       └── ProcessCbmmTransactionUseCaseImpl.java
    │   │   │   └── infrastructure/
    │   │   │       ├── adapters/
    │   │   │       │   ├── in/
    │   │   │       │   │   └── http/
    │   │   │       │   │       ├── CbmmController.java
    │   │   │       │   │       ├── HealthController.java
    │   │   │       │   │       └── dto/
    │   │   │       │   │           ├── AccountDTO.java
    │   │   │       │   │           ├── BatchProcessingResponse.java
    │   │   │       │   │           └── EventDTO.java
    │   │   │       │   └── out/
    │   │   │       │       ├── postgresql/
    │   │   │       │       │   ├── AccountRepositoryImpl.java
    │   │   │       │       │   ├── PostgresAccountRepository.java
    │   │   │       │       │   ├── PostgresTransactionRepository.java
    │   │   │       │       │   └── TransactionRepositoryImpl.java
    │   │   │       │       └── reddis/
    │   │   │       │           └── IdempotencyCheckerImpl.java
    │   │   │       └── config/
    │   │   │           ├── AsyncConfiguration.java
    │   │   │           ├── RedisConfiguration.java
    │   │   │           └── TransactionConfig.java
    │   │   └── resources/
    │   │       ├── application.yml
    │   │       ├── schema.sql
    │   │       └── data.sql
    │   └── test/
    │       └── java/com/processor/
    │           ├── CbmmSystemApplicationTests.java
    │           ├── MockFactoryTest.java
    │           ├── application/
    │           │   └── CbmmTransactionApplicationServiceTest.java
    │           ├── core/
    │           │   └── use_case/
    │           │       └── ProcessCbmmTransactionUseCaseImplTest.java
    │           └── infrastructure/
    │               └── out/
    │                   └── postgresql/
    │                       └── TransactionRepositoryImplTest.java
```

## Descripción de Componentes

### Archivos Raíz

- **`.gitignore`**: Configuración de archivos ignorados por Git (targets Maven, IDE files, etc.)
- **`LICENSE`**: Licencia MIT del proyecto
- **`Makefile`**: Comandos automatizados para Docker Compose (build, up, down, logs, clean, restart, infra, services)
- **`README.md`**: Documentación principal del proyecto con objetivos, instalación y ejecución
- **`design.md`**: Especificación técnica detallada con requisitos, arquitectura, modelo de datos y escalabilidad
- **`implementation.md`**: Documentación de la implementación técnica y estructura del código
- **`docker-compose.yml`**: Orquestación de servicios (PostgreSQL, Redis, CBMM-Processor)

### Recursos

#### `resources/`
- **`Challenge.pdf`**: Documento original del desafío técnico
- **`diagrams/`**: Diagramas de arquitectura y diseño del sistema
    - **`datamodel.png`**: Modelo de datos relacional
    - **`sequence.png`**: Diagrama de secuencia del flujo CBMM
    - **`initial.png`**: Diseño arquitectónico inicial
    - **`intermediate.png`**: Diseño arquitectónico intermedio con escalabilidad
    - **`final.png`**: Diseño arquitectónico final con microservicios

### CBMM Processor Service

#### Directorio Raíz del Servicio
- **`Dockerfile`**: Imagen Docker multi-stage (Maven build + OpenJDK runtime)
- **`pom.xml`**: Configuración Maven con dependencias (Spring Boot 3.5.6, PostgreSQL, Redis, Lombok, Testing)

#### `src/main/java/com/processor/`

##### Punto de Entrada
- **`CbmmSystemApplication.java`**: Clase principal de Spring Boot

##### `application/service/`
- **`CbmmTransactionApplicationService.java`**: Servicio de aplicación que orquesta el procesamiento de transacciones
    - Procesamiento asíncrono con CompletableFuture
    - Manejo de idempotencia
    - Retry mechanism con exponential backoff y jitter
    - Manejo de OptimisticLockException

##### `core/domain/`

###### `enums/`
- **`ProcessingStatus.java`**: Estados de procesamiento (PROCESSING, SUCCESS, FAILED)
- **`TransactionStatus.java`**: Estados de transacción (APPLIED)
- **`TransactionType.java`**: Tipos de transacción (DEBIT, CREDIT)

###### `exception/`
- **`AccountNotFoundException.java`**: Excepción cuando no se encuentra una cuenta
- **`InsufficientFundsException.java`**: Excepción por fondos insuficientes
- **`InvalidCurrencyException.java`**: Excepción por mismatch de moneda
- **`TransactionProcessingException.java`**: Excepción general de procesamiento

###### `model/`
- **`Account.java`**: Entidad JPA de cuenta con optimistic locking (@Version)
    - Métodos de dominio: debit() y credit()
    - Balance management con BigDecimal
- **`Transaction.java`**: Entidad JPA de transacción con ledger completo
    - Registro auditable de movimientos

###### `value_object/`
- **`TransactionData.java`**: DTO para datos de transacción CBMM
- **`TransactionResult.java`**: DTO para resultado de procesamiento con estados
- **`TransferAccount.java`**: DTO para información de cuenta en transferencia

##### `core/ports/`

###### `in/`
- **`ProcessCbmmTransactionUseCase.java`**: Puerto de entrada para procesamiento de transacciones

###### `out/`
- **`AccountRepository.java`**: Puerto de salida para repositorio de cuentas
- **`IdempotencyChecker.java`**: Puerto de salida para verificación de idempotencia
- **`TransactionRepository.java`**: Puerto de salida para repositorio de transacciones

##### `core/use_case/`
- **`ProcessCbmmTransactionUseCaseImpl.java`**: Implementación del caso de uso principal
    - Validación de cuentas y monedas
    - Prevención de deadlocks con ordenamiento de cuentas
    - Transacciones ACID con aislamiento READ_COMMITTED
    - Generación de transacciones de débito y crédito
    - Entity flushing para garantizar persistencia

##### `infrastructure/adapters/`

###### `in/http/`
- **`CbmmController.java`**: REST Controller para procesamiento de transacciones
    - `POST /api/cbmm/process-batch`: Procesa múltiples transacciones concurrentemente
    - `POST /api/cbmm/process-batch-file`: Procesa transacciones desde archivo JSON
    - `POST /api/cbmm/process-single`: Procesa una transacción sincrónica
- **`HealthController.java`**: Endpoint de health check
- **`dto/`**: DTOs para capa HTTP
    - **`AccountDTO.java`**: DTO de cuenta inmutable (@Value)
    - **`BatchProcessingResponse.java`**: DTO de respuesta con estadísticas
    - **`EventDTO.java`**: DTO de evento CBMM

###### `out/postgresql/`
- **`AccountRepositoryImpl.java`**: Implementación del repositorio de cuentas
- **`PostgresAccountRepository.java`**: JpaRepository para Account
- **`PostgresTransactionRepository.java`**: JpaRepository para Transaction
- **`TransactionRepositoryImpl.java`**: Implementación del repositorio de transacciones

###### `out/reddis/` (nota: typo en el nombre del paquete)
- **`IdempotencyCheckerImpl.java`**: Implementación de idempotencia con Redis
    - Set-if-not-exists pattern
    - TTL configurables por estado
    - Manejo de locks de procesamiento
    - Prevención de procesamiento duplicado

##### `infrastructure/config/`
- **`AsyncConfiguration.java`**: Configuración de ThreadPoolTaskExecutor
    - Core pool: 10 threads
    - Max pool: 20 threads
    - Queue capacity: 100
    - Rejection policy: CallerRunsPolicy
- **`RedisConfiguration.java`**: Configuración de Redis y cache
    - StringRedisTemplate y RedisTemplate
    - Serialización JSON con Jackson
- **`TransactionConfig.java`**: Configuración de reintentos
    - Propiedades externalizadas con @ConfigurationProperties

#### `src/main/resources/`
- **`application.yml`**: Configuración de la aplicación
    - Datasource PostgreSQL con Hikari pool
    - JPA/Hibernate con dialect PostgreSQL
    - Redis con Lettuce pool
    - Transaction retry configuration
    - Logging levels
- **`schema.sql`**: DDL para creación de tablas
    - accounts: id, balance, currency, version, timestamps
    - transactions: ledger completo con foreign key
    - Índices para optimización de queries
- **`data.sql`**: Datos de prueba iniciales
    - Cuentas con diferentes monedas (MXN, USD, EUR, BRL)
    - Balances iniciales variados

#### `src/test/java/com/processor/`

##### Test Utilities
- **`MockFactoryTest.java`**: Clase base con factory methods para crear mocks y test data
- **`CbmmSystemApplicationTests.java`**: Test de contexto de Spring Boot

##### Test Suites

###### `application/`
- **`CbmmTransactionApplicationServiceTest.java`**: Suite completa de tests del servicio de aplicación
    - Tests de procesamiento asíncrono exitoso
    - Tests de idempotencia (already processed, already processing)
    - Tests de manejo de excepciones
    - Tests de retry con OptimisticLockException
    - Tests de procesamiento concurrente múltiple
    - Tests de procesamiento sincrónico
    - Tests de espera de futures

###### `core/use_case/`
- **`ProcessCbmmTransactionUseCaseImplTest.java`**: Suite completa de tests del caso de uso
    - Tests de procesamiento exitoso con validaciones
    - Tests de validación de fondos insuficientes
    - Tests de validación de moneda (origen y destino)
    - Tests de edge cases (balance exacto, montos decimales)
    - Tests de cuentas no encontradas
    - Tests de metadata de transacciones

###### `infrastructure/out/postgresql/`
- **`TransactionRepositoryImplTest.java`**: Tests del repositorio de transacciones

## Arquitectura del Código

### Patrón de Arquitectura
El proyecto sigue **Hexagonal Architecture (Ports & Adapters)** con las siguientes capas:

1. **Infrastructure Layer** (`infrastructure/adapters/`):
    - Input adapters: REST Controllers
    - Output adapters: PostgreSQL repositories, Redis idempotency checker
2. **Application Layer** (`application/service/`):
    - Servicios de orquestación
    - Manejo de concurrencia y reintentos
3. **Core Layer** (`core/`):
    - **Domain**: Entidades, value objects, excepciones de negocio
    - **Ports**: Interfaces que definen contratos
    - **Use Cases**: Lógica de negocio pura

### Características Técnicas Implementadas

#### Idempotencia
- Verificación distribuida con Redis
- Pattern: Set-if-not-exists con TTL
- Estados: PROCESSING → SUCCESS/FAILED
- Prevención de procesamiento duplicado en sistemas concurrentes

#### Consistencia Transaccional
- **Optimistic Locking**: @Version en entidades para detectar modificaciones concurrentes
- **Transaction Isolation**: READ_COMMITTED para balance entre consistencia y performance
- **Atomic Operations**: Propagation.REQUIRES_NEW para transacciones independientes
- **Deadlock Prevention**: Ordenamiento determinístico de cuentas por ID

#### Concurrencia
- **Async Processing**: ThreadPoolTaskExecutor con CompletableFuture
- **Retry Mechanism**: Exponential backoff con jitter para dispersión temporal
- **Optimistic Lock Handling**: Reintentos automáticos con backoff configurables
- **Thread Pool Configuration**: Core/max threads y queue capacity ajustables

#### Event-Driven Design (Preparado)
- Estructura de eventos CBMM con event_id, operation_date
- Value objects preparados para mensajería
- Diseño compatible con Event Bridge y message queues

#### Observabilidad (Diseñada)
- Logging estructurado con SLF4J
- Métricas de negocio en respuestas (success/failed counts)
- Preparado para integración con Prometheus/Grafana

#### Testing
- **Unit Tests**: Mockito para aislamiento de dependencias
- **Test Coverage**: Application service, use cases, repositories
- **Test Utilities**: Factory methods y builders para test data
- **Assertions**: JUnit 5 + AssertJ para validaciones expresivas

### Stack Tecnológico

#### Backend
- **Framework**: Spring Boot 3.5.6
- **Language**: Java 25
- **Build Tool**: Maven 3.9.11

#### Persistencia
- **Database**: PostgreSQL 18.0 con Hikari connection pool
- **ORM**: Spring Data JPA + Hibernate
- **Migrations**: SQL scripts con schema.sql/data.sql

#### Cache & Idempotencia
- **Cache**: Redis 8.2.2 con Lettuce driver
- **Connection Pool**: Apache Commons Pool2

#### Testing
- **Framework**: JUnit 5
- **Mocking**: Mockito 5.20.0
- **In-Memory DB**: H2 2.3.232 para tests

#### Containerización
- **Docker**: Multi-stage builds con Maven + OpenJDK
- **Orchestration**: Docker Compose con health checks
- **Networking**: Bridge network para comunicación entre servicios

### Infraestructura como Código

#### Docker Compose Services
- **cbmm-processor**: Servicio principal en puerto 8080
    - Depends on: postgres, redis (con health checks)
- **postgres**: Base de datos con volume persistente
    - Health check con pg_isready
- **redis**: Cache con AOF persistence
    - Health check con redis-cli ping

#### Makefile Commands
```bash
make build          # Construir servicios
make up             # Iniciar servicios en background
make down           # Detener servicios
make logs           # Ver logs en tiempo real
make clean          # Limpieza completa (volumes + system prune)
make restart        # Reiniciar servicios
make infra          # Solo infraestructura (postgres + redis)
make services       # Solo servicios aplicación
```
### Patrones de Diseño Implementados

1. **Repository Pattern**: Abstracción de persistencia
2. **Factory Pattern**: MockFactoryTest para creación de test objects
3. **Builder Pattern**: Lombok @Builder en DTOs y entidades
4. **Strategy Pattern**: Retry strategies con backoff configurable
5. **Template Method**: Procesamiento transaccional reutilizable
6. **Dependency Injection**: Spring IoC container

## Principios SOLID Aplicados

- SRP: Separación clara entre use cases, servicios y repositories
- OCP: Extensible vía interfaces (ports)
- LSP: Implementaciones intercambiables de repositorios
- ISP: Interfaces específicas por responsabilidad
- DIP: Dependencia de abstracciones (ports) no de implementaciones

### Decisiones de Diseño Técnico
#### Manejo de Concurrencia

- Choice: Optimistic Locking + Retry con Jitter
- Rationale: Balance entre throughput y consistencia en escenarios de alta concurrencia
- Trade-off: Mayor complejidad vs consistencia eventual garantizada

#### Ordenamiento de Cuentas

- Choice: Ordenamiento alfabético por ID antes de bloqueo
- Rationale: Prevención determinística de deadlocks en transacciones bidireccionales
- Trade-off: Overhead de sorting vs eliminación de deadlocks

#### Idempotencia con Redis

- Choice: Redis con TTL por estado (5min processing, 24h success/failed)
- Rationale: Performance de cache distribuido + expiración automática
- Trade-off: Consistencia eventual vs latencia baja

#### Procesamiento Asíncrono

- Choice: ThreadPoolTaskExecutor con CompletableFuture
- Rationale: Maximizar throughput en batch processing
- Trade-off: Complejidad de manejo de errores vs performance

### Notas de Implementación

- Timezone Handling: Todos los timestamps en UTC (ISO 8601)
- Decimal Precision: BigDecimal con scale 4 para manejo de monedas
- Transaction Boundaries: Propagation.REQUIRES_NEW para independencia transaccional
- Error Handling: Excepciones de dominio específicas vs genéricas
- Immutability: Value objects inmutables con Lombok @Value
- Null Safety: Optional para manejo explícito de ausencia de valores