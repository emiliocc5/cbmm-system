# CBMM System
Software for Cross Border Money Movements

## Objetivo
Crear una plataforma que permita realizar transferencias de dinero transfronterizas

- [Challenge](./resources/Challenge.pdf)

## Diseño e implementación

- [Diseño](design.md)
- [Implementación](implementation.md)

## Instalación y Ejecución

### Prerrequisitos
- Docker
- Docker Compose

### Comandos Principales

```bash
# Construir sin cache
docker compose build --no-cache

# Ejecutar aplicación
docker compose up -d

# Detener servicios
docker compose down
```

### Acceso a Servicios

- **CBMM-Processor**: http://localhost:8080


---
**Autor**: [Emilio Nicolas Caccia Campaner]  
**Versión**: 1.0.0  
**Fecha**: Octubre 2025






