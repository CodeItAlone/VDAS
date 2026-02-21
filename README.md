# VDAS — Voice-Driven Developer Automation System

**Offline, deterministic voice automation for developers.** Execute system commands using natural voice input or keyboard — no cloud, no AI, no NLP libraries.

---

## Architecture Overview

```
Raw Input (voice / keyboard)
  → IntentResolver
      → Intent { rawInput, normalizedInput, resolvedCommand, confidence, confidenceBand }
          → ExecutionGate (checks Danger / prompts Confirmation)
              → SkillRegistry
                  → Skill.execute(Intent)
```

| Layer | Responsibility |
|-------|---------------|
| **Speech** | Offline voice capture via Vosk (ASR) |
| **Intent** | Normalize input → resolve command → compute confidence & band |
| **Safety** | Execution gate, danger classification, user confirmation |
| **Skill** | Domain-scoped command execution (stateless) |
| **Executor** | OS-level process execution via `ProcessBuilder` |
| **Config** | JSON-driven command definitions with aliases |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Vosk speech model (English, ~1.8 GB)

### Vosk Model Setup

1. Download from [Vosk Models](https://alphacephei.com/vosk/models): **`vosk-model-en-us-0.22`**
2. Extract to `C:\vosk-models\vosk-model-en-us-0.22`
3. Update `VOSK_MODEL_PATH` in `Main.java` if using a different location.

---

## Build & Run

```bash
mvn clean package -q
java -jar target/vdas-1.0-SNAPSHOT.jar
```

### CLI Mode (single command)

```bash
java -jar target/vdas-1.0-SNAPSHOT.jar "java version"
```

### Run Tests

```bash
mvn test
```

---

## Usage

On startup, VDAS prompts for input mode:

```
Select input mode:
  [V] Voice (offline, microphone)
  [K] Keyboard (default)
>
```

- **Voice mode** — Speak a command (e.g., "java version"). VDAS listens up to 10s, recognizes offline, resolves intent, and executes.
- **Keyboard mode** — Type the command name or number.
- Switch modes: `k` (keyboard) / `q` (quit) at any time.

---

## Configuration

Edit `src/main/resources/commands.json` to add or modify commands:

```json
{
  "name": "my-command",
  "command": "echo Hello",
  "workingDirectory": "C:\\some\\path",
  "aliases": ["run my command", "execute my cmd"]
}
```

Each command supports:
- **`name`** — canonical identifier (kebab-case)
- **`command`** — OS command string
- **`workingDirectory`** — optional execution directory
- **`aliases`** — optional list of voice/text synonyms for matching

---

## Intent Resolution Pipeline

All user input flows through `IntentResolver`, which produces an immutable `Intent` object:

| Stage | Match Type | Confidence |
|-------|-----------|------------|
| 1. Exact match | Normalized input = normalized command name | `1.0` |
| 2. Alias match | Normalized input = normalized alias | `1.0` |
| 3. Fuzzy match | Levenshtein similarity ≥ `0.75` threshold | `score` |
| 4. Rejection | Below threshold or ambiguous (top-2 margin < `0.05`) | `0.0` |

Input normalization: lowercase → strip punctuation → collapse whitespace → strip leading ASR articles (`the`, `a`, `an`).

---

## Safety & Confirmation Gate

After intent resolution, an **ExecutionGate** evaluates the intent based on its `ConfidenceBand`, danger level, and ambiguity:

| ConfidenceBand | Dangerous | Ambiguous | Decision |
|----------------|-----------|-----------|----------|
| HIGH           | NO        | —         | EXECUTE  |
| HIGH           | YES       | —         | CONFIRM  |
| MEDIUM         | *         | YES       | CLARIFY  |
| MEDIUM         | *         | NO        | CONFIRM  |
| LOW            | *         | *         | REJECT   |
| (unresolved)   | —         | —         | REJECT   |

* **Dangerous Commands**: Hardcoded set (`quit`, `shutdown`, `restart`, `delete`, `remove`, `format`).
* **Ambiguity**: Detected when MEDIUM band + 2+ candidates + top-two score gap ≤ 0.10.
* **Confirmation**: Prompts `Are you sure you want to <action>? (yes / no)`.
* **Clarification**: Prints numbered candidate list, accepts index or exact name. One shot only.
* Clarified intents are re-gated through ExecutionGate (dangerous clarified commands still require confirmation).
* Keyboard shortcut (`q` / `quit`) bypasses this gate for convenience.

---

## Project Structure

```
src/main/java/vdas/
├── Main.java                        — Entry point, input mode selection, main loop
├── intent/
│   ├── Intent.java                  — Immutable intent model (raw, normalized, command, confidence, band, candidates)
│   ├── IntentResolver.java          — Deterministic resolution engine (exact → alias → fuzzy)
│   ├── IntentNormalizer.java        — Input normalization (voice + keyboard)
│   ├── ConfidenceBand.java          — HIGH/MEDIUM/LOW confidence bands
│   ├── AmbiguityDetector.java       — Ambiguity detection interface
│   ├── DefaultAmbiguityDetector.java — Score-based ambiguity detection
│   └── LevenshteinDistance.java     — Edit distance & similarity scoring
├── safety/
│   ├── ExecutionGate.java           — Evaluates confidence×danger×ambiguity (EXECUTE/CONFIRM/CLARIFY/REJECT)
│   ├── DangerClassifier.java        — Danger classification interface
│   ├── DefaultDangerClassifier.java — Hardcoded list of dangerous commands
│   ├── ConfirmationManager.java     — Yes/no user confirmation prompt
│   └── ClarificationPrompt.java     — Ambiguous command clarification prompt
├── skill/
│   ├── Skill.java                   — Skill interface (canHandle + execute)
│   ├── SkillRegistry.java           — First-match skill dispatcher
│   ├── SystemInfoSkill.java         — system-info, java-version
│   └── FileSystemSkill.java         — list-files
├── executor/
│   └── CommandExecutor.java         — ProcessBuilder wrapper
├── config/
│   └── CommandLoader.java           — JSON config reader (Gson)
├── model/
│   └── SystemCommand.java           — Command model with aliases
└── speech/
    ├── SpeechInput.java             — Speech input interface
    └── VoskSpeechInput.java         — Vosk offline ASR implementation

src/main/resources/
└── commands.json                    — Command definitions

src/test/java/vdas/
├── intent/
│   ├── IntentTest.java              — Immutability & structure tests
│   ├── IntentResolverTest.java      — Resolution pipeline tests
│   ├── IntentNormalizerTest.java    — Normalization tests
│   ├── AmbiguityDetectorTest.java   — Score-based ambiguity tests
│   └── LevenshteinDistanceTest.java — Edit distance tests
├── safety/
│   ├── ExecutionGateTest.java       — PRD validation matrix + CLARIFY tests
│   ├── DangerClassifierTest.java    — Classification rules tests
│   ├── ConfirmationManagerTest.java — Confirmation input tests
│   └── ClarificationPromptTest.java — Clarification input tests
├── skill/
│   ├── SkillRegistryTest.java       — Dispatch & ordering tests
│   ├── FileSystemSkillTest.java     — canHandle tests
│   └── SystemInfoSkillTest.java     — canHandle tests
└── config/
    └── CommandLoaderTest.java       — JSON loading tests
```

---

## Design Principles

- **Offline-first** — No network, no cloud, no external APIs
- **Deterministic** — Same input always produces same output
- **No AI/ML/NLP** — Pure algorithmic resolution (Levenshtein + exact matching)
- **Immutable Intent** — `Intent` is a value object, no mutation after creation
- **Single responsibility** — Only `IntentResolver` creates `Intent` objects
- **Stateless skills** — Skills have no memory between executions
- **Explicit registration** — No reflection, no DI framework

---

## License

Private project.
