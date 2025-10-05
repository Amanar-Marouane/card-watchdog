# Card Watchdog

## Setup and Running Instructions

### 1. Configure Environment
First, copy the environment configuration file:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `src/main/resources/application.properties` with your actual environment credentials.

### 2. Compile the Project
```bash
mvn compile
```

### 3. Run the Application
```bash
mvn exec:java -Dexec.mainClass="www.Main"
```

### For .jar Packaging
To package the application into a `.jar` file:
```bash
mvn clean package
```

## Requirements
- Java 17  
- Maven  
- MariaDB/MySQL database  

---

## Project Structure

```
src
├── config          # Configuration loaders and environment setup
├── controllers     # Input handling and business logic coordination
├── database        # Database schemas & SQL initialization
├── entities        # Data models representing tables
├── enums           # Enumerations (card types, statuses, alert levels, etc.)
├── repositories    # Data access layer (DAO pattern)
├── resources       # Application properties and configuration files
├── services        # Core business logic and service classes
├── ui              # Console-based user interface
├── utils           # Utilities (helpers, callbacks, console, hydrators)
└── www             # Main entry point
```

---

## Description

**Card Watchdog** is a Java-based fraud detection and card management system.  
It simulates the backend logic for:
- Monitoring and detecting fraudulent activity on payment cards.
- Managing users, cards, and transactions.
- Generating fraud alerts and managing card status.

Designed for educational and experimental purposes to demonstrate:
- Layered architecture (Controllers → Services → Repositories → DB)
- Clean separation of concerns
- JDBC and dynamic query handling
- Console-based UI with alert and fraud detection logic.
