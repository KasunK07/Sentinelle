# Sentinelle - Transaction Processor & MDR Calculator

Sentinelle is a Spring Boot-based backend service developed for the PAYable Backend Developer Practical Assessment. It processes transaction CSV data, determines BIN locality (Local vs. Foreign) using an Excel reference, and calculates MDR fees and net amounts.

Tech Stack

- **Java:** 17
- **Framework:** Spring Boot 3.2.5
- **Build Tool:** Maven
- **Core Libraries:**
  - **Apache POI 5.2.5**: Excel (.xlsx) parsing for BIN master data.
  - **OpenCSV 5.9**: CSV reading and generation.
  - **Lombok**: Boilerplate reduction.
  - **MapStruct**: DTO mapping.
  - **SpringDoc OpenAPI 2.x**: Swagger UI documentation.

Getting Started

### Prerequisites
- JDK 17
- Maven 3.8+

### Build and Run
1. **Build the project:**
   ```bash
   mvn clean install

2. Run
   mvn spring-boot:run

3.Swagger Link
   http://localhost:8080/swagger-ui/index.html
