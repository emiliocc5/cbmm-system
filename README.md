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
make build          # Construir servicios
make up             # Iniciar servicios en background
make down           # Detener servicios
make logs           # Ver logs en tiempo real
make clean          # Limpieza completa (volumes + system prune)
make restart        # Reiniciar servicios
make infra          # Solo infraestructura (postgres + redis)
make services       # Solo servicios aplicación
```

### Acceso a Servicios

- **CBMM-Processor**: http://localhost:8080

### Consideraciones
- A los efectos de poder validar el comportamiento de la aplicación de manera aislada se crearon endpoints de prueba y se adjunta una collection de postman
útil para realizar pruebas en ambientes bajos. [postman-collection](./resources/postman_collection.json)
- El endpoint declarado en el apartado de [diseño](design.md) no refleja la realidad actual de la aplicación sino un deseable.
- La data inicial cargada tiene varias cuentas extra que matchean con las pruebas realizadas.

---
**Autor**: [Emilio Nicolas Caccia Campaner]  
**Versión**: 1.0.0  
**Fecha**: Octubre 2025






