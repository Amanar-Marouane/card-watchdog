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

## Requirements
- Java 17
- Maven
- MariaDB/MySQL database
