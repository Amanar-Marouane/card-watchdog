# Card Watchdog

## Setup and Running Instructions

### 1. Configure Database
First, copy the database configuration file:
```bash
cp src/main/resources/database.properties.example src/main/resources/database.properties
```

Edit `src/main/resources/database.properties` with your actual database credentials.

### 2. Compile the Project
```bash
mvn compile
```

### 3. Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.cardwatchdog.Main"
```

## Requirements
- Java 17
- Maven
- MariaDB/MySQL database
