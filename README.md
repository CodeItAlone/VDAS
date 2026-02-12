# VDAS — Voice-Driven Developer Automation System

**Step 1: Foundation** — Execute predefined system commands from a JSON config file.

## Prerequisites

- Java 17+
- Maven 3.8+

## Build & Run

```bash
mvn clean package -q
java -jar target/vdas-1.0-SNAPSHOT.jar
```

## Configuration

Edit `src/main/resources/commands.json` to add/modify commands:

```json
{
  "name": "my-command",
  "command": "echo Hello",
  "workingDirectory": "C:\\some\\path"   // optional
}
```

## Project Structure

```
vdas/
 ├── src/main/java/vdas/
 │   ├── Main.java              — Entry point
 │   ├── executor/
 │   │   └── CommandExecutor.java — ProcessBuilder wrapper
 │   ├── config/
 │   │   └── CommandLoader.java   — JSON config reader
 │   └── model/
 │       └── SystemCommand.java   — Command POJO
 ├── src/main/resources/
 │   └── commands.json            — Command definitions
 └── pom.xml
```
