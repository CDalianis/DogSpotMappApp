# Dog Spot Map

A full-stack web application for discovering, saving, and organizing dog-friendly locations on an interactive map. The backend exposes a REST API backed by PostgreSQL; the browser UI uses Leaflet and OpenStreetMap tiles with a Spring Boot–served single-page experience.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
  - [Web Application](#web-application)
  - [REST API](#rest-api)
  - [Backend Services](#backend-services)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Data Model](#data-model)
- [Error Handling](#error-handling)
- [Logging](#logging)
- [Project Structure](#project-structure)

## Overview

Dog Spot Map helps users plan walks and keep track of places such as parks, veterinarians, grooming salons, and dog-friendly cafés. Locations are stored persistently in PostgreSQL. On first startup with an empty database, the application seeds a small set of sample spots in Athens, Greece.

The default map view is centered on Athens (`37.9838, 23.7275`). Users can add spots by clicking the map, edit details in a side drawer, filter the location list, build a simple multi-stop route, and import or export their data as JSON.

## Features

### Web Application

| Feature | Description |
|---------|-------------|
| Interactive map | OpenStreetMap tiles via [Leaflet](https://leafletjs.com/); click the map to create a new spot at the chosen coordinates. |
| Category markers | Visual markers use emoji by category (park, vet, grooming, café, water, bags, other). |
| Location list | Sidebar lists all saved spots with name, notes preview, coordinates, and category. |
| Search and filters | Filter by text (name or notes), category, minimum rating, favorites only, notes only, and proximity to the user. |
| Geolocation | “Use my location” centers the map and sorts the list by distance (Haversine formula). |
| Edit drawer | Update name, category, notes, favorite/visited flags, rating (1–5 paws), and photos. |
| Photo attachments | Upload up to six images per batch (stored as Base64 data URLs; up to nine per location in the UI). |
| Route planning | Select multiple spots from the list and draw a straight-line route with approximate total distance. |
| JSON export | Download all locations as `dog-spots.json` from the browser. |
| JSON import | Replace all server-side locations by uploading a JSON array (calls the bulk import API). |
| Popups | Map markers show name, rating, notes, and the first photo thumbnail when available. |

### REST API

| Method | Path | Success status | Description |
|--------|------|----------------|-------------|
| `GET` | `/api/locations` | `200` | List all locations with photos, sorted by id. |
| `POST` | `/api/locations` | `201` | Create a location. Server assigns `id`. Returns `Location` header pointing to the new resource. |
| `PUT` | `/api/locations/{id}` | `200` | Update an existing location. Path id must match body id when body id is present. |
| `DELETE` | `/api/locations/{id}` | `204` | Delete a location by id. |
| `GET` | `/api/locations/export` | `200` | Export all locations (same payload as list). |
| `POST` | `/api/locations/import` | `204` | Replace all locations with the provided JSON array. `null` body clears all locations. |

Interactive API documentation is available at:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Backend Services

| Capability | Description |
|------------|-------------|
| Persistence | JPA/Hibernate entities stored in PostgreSQL (`locations`, `location_photos`). |
| Validation | Jakarta Bean Validation on the `Location` entity (coordinates, name, category, rating). |
| Default values | Missing category defaults to `OTHER`; `visited` and `favorite` default to `false`; empty photo list. |
| Photo size limit | Each photo data URL is limited to 512 KB (enforced in the service layer). |
| Centralized errors | `@ControllerAdvice` maps domain exceptions to consistent JSON error responses. |
| Structured logging | SLF4J (via Lombok `@Slf4j`) on controllers, services, data loader, and the global error handler. |
| Sample data | `LocationDataLoader` seeds three Athens locations when the database is empty. |

## Technology Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Thymeleaf) |
| Database | PostgreSQL 16 |
| API docs | springdoc-openapi 2.8 |
| Frontend | Leaflet 1.9, vanilla JavaScript, Thymeleaf template |
| Build | Maven (wrapper included) |
| Utilities | Lombok |

## Prerequisites

- **JDK 21** or newer
- **Maven 3.9+** (or use `./mvnw` / `mvnw.cmd` from the project root)
- **Docker** (recommended) for running PostgreSQL locally, or an existing PostgreSQL 16 instance

## Getting Started

### 1. Start PostgreSQL

Using Docker Compose (recommended):

```bash
docker compose up -d
```

This starts PostgreSQL on port `5432` with database `mapstest`, user `mapstest`, and password `mapstest` (see `docker-compose.yml`).

### 2. Run the application

```bash
./mvnw spring-boot:run
```

On Windows:

```cmd
mvnw.cmd spring-boot:run
```

### 3. Open the application

| Resource | URL |
|----------|-----|
| Map UI | [http://localhost:8080/](http://localhost:8080/) |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |

### 4. Build without running

```bash
./mvnw clean package
```

The executable JAR is produced under `target/`.

## Configuration

Main settings are in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/mapstest` | JDBC connection URL |
| `spring.datasource.username` | `mapstest` | Database user |
| `spring.datasource.password` | `mapstest` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate schema strategy |
| `spring.jpa.open-in-view` | `false` | Disables OSIV for cleaner transactions |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |

Optional logging (see more detail requests in application logs):

```properties
logging.level.com.example.mapstest=DEBUG
```

## API Reference

### Location JSON shape

```json
{
  "id": 1,
  "lat": 37.9838,
  "lng": 23.7275,
  "name": "National Garden",
  "notes": "Shaded paths in summer",
  "category": "PARK",
  "visited": false,
  "favorite": true,
  "rating": 4,
  "photos": ["data:image/jpeg;base64,..."]
}
```

### Categories

`PARK`, `VET`, `GROOMING`, `CAFE`, `WATER`, `BAGS`, `OTHER`

### Example: create a location

```bash
curl -i -X POST http://localhost:8080/api/locations \
  -H "Content-Type: application/json" \
  -d '{"lat":37.98,"lng":23.73,"name":"Riverside walk","category":"PARK"}'
```

Expected response: `HTTP/1.1 201 Created` with a `Location` header and JSON body.

### Example: delete a location

```bash
curl -i -X DELETE http://localhost:8080/api/locations/1
```

Expected response: `HTTP/1.1 204 No Content`.

## Data Model

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | Long | Server-generated; read-only on create |
| `lat` | double | −90 to 90 |
| `lng` | double | −180 to 180 |
| `name` | String | Required, non-blank |
| `notes` | String | Optional |
| `category` | String | One of the allowed category values |
| `visited` | Boolean | Optional; defaults to `false` |
| `favorite` | Boolean | Optional; defaults to `false` |
| `rating` | Integer | Optional; 1–5 when set |
| `photos` | List of String | Base64 data URLs; max ~512 KB each |

Photos are stored in a separate collection table (`location_photos`) with preserved order.

## Error Handling

Errors are returned as JSON with a stable structure:

| HTTP status | Exception / case | Response type |
|-------------|------------------|---------------|
| `400` | Validation failure | `ValidationErrorResponseDTO` (`code`, `message`, `errors` map) |
| `400` | Invalid argument (e.g. path/body id mismatch) | `ErrorResponseDTO` |
| `404` | Location not found | `ErrorResponseDTO` |
| `409` | Duplicate id on create | `ErrorResponseDTO` |
| `500` | Photo too large, application error, unexpected failure | `ErrorResponseDTO` |

Unexpected server errors are logged at `ERROR` level with a stack trace; expected client errors are logged at `WARN`.

## Logging

Logging uses SLF4J with Logback (provided by Spring Boot). Lombok `@Slf4j` is used in:

- `LocationController` — request boundaries (`INFO` for mutations, `DEBUG` for reads)
- `LocationService` — business events (create, update, delete, import)
- `LocationDataLoader` — sample data seeding
- `ErrorHandler` — handled and unhandled exceptions

## Project Structure

```
src/main/java/com/example/mapstest/
├── MapstestApplication.java      # Spring Boot entry point
├── config/
│   ├── LocationDataLoader.java   # Sample data on empty DB
│   └── OpenApiConfig.java        # OpenAPI metadata
├── controller/
│   ├── LocationController.java   # REST API
│   └── MapPageController.java    # Serves map page at /
├── core/
│   ├── ErrorHandler.java         # Global exception handling
│   └── exceptions/               # Domain exceptions
├── dto/                          # API error response records
├── model/
│   └── Location.java             # JPA entity
├── repository/
│   └── LocationRepository.java
└── service/
    └── LocationService.java      # Business logic and validation

src/main/resources/
├── application.properties
├── static/map.js                 # Map UI logic
└── templates/map.html            # Map page template
```

## License

This project is provided as a demonstration application. No license file is included; add one before distribution if required.
