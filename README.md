# VDAS — Voice-Driven Developer Automation System

**Step 2: Offline Voice Input** — Execute system commands using voice or keyboard input.

## Prerequisites

- Java 17+
- Maven 3.8+
- Vosk speech model (English, ~40 MB)

### Vosk Model Setup

1. Download the model from [Vosk Models](https://alphacephei.com/vosk/models):
   - Recommended: `vosk-model-small-en-us-0.15`
2. Extract to a known location (e.g., `C:\vosk-models\vosk-model-small-en-us-0.15`)
3. Update `DEFAULT_MODEL_PATH` in `VoskSpeechInput.java` if using a different path.

## Build & Run

```bash
mvn clean package -q
java -jar target/vdas-1.0-SNAPSHOT.jar
```

## Usage

On startup, VDAS asks you to choose an input mode:

```
Select input mode:
  [V] Voice (offline, microphone)
  [K] Keyboard (default)
>
```

- **Voice mode**: Speak a command name (e.g., "java version"). VDAS listens for up to 10 seconds, recognizes your speech offline, and executes the matching command.
- **Keyboard mode**: Type the command name or number, same as Step 1.
- In voice mode, type `k` to switch to keyboard, or `q` to quit.

## Configuration

Edit `src/main/resources/commands.json` to add/modify commands:

```json
{
  "name": "my-command",
  "command": "echo Hello",
  "workingDirectory": "C:\\some\\path"
}
```

## Project Structure

```
vdas/
 ├── src/main/java/vdas/
 │   ├── Main.java              — Entry point (voice/keyboard mode selection)
 │   ├── speech/
 │   │   ├── SpeechInput.java   — Speech input interface
 │   │   └── VoskSpeechInput.java — Vosk offline implementation
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
