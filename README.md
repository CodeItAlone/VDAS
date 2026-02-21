# VDAS — Voice-Driven Developer Automation System

**Offline, deterministic voice automation for developers.** Execute system commands using natural voice input or keyboard — no cloud, no AI, no NLP libraries.

---

## Architecture Overview

```
Raw Input (voice / keyboard)
  → IntentNormalizer (lowercase, strip punctuation, remove articles)
      → IntentResolver (exact → alias → verb-prefix → fuzzy)
          → Intent { rawInput, normalizedInput, resolvedCommand, confidence, band, candidates }
              → ExecutionGate (checks Danger / Ambiguity / prompts Confirmation or Clarification)
                  → SkillRegistry
                      → Skill.execute(Intent)
```

| Layer | Responsibility |
|-------|---------------|
| **Speech** | Offline voice capture via Whisper STT (Python microservice) |
| **Intent** | Normalize input → resolve command → compute confidence & band → detect ambiguity |
| **Safety** | Execution gate, danger classification, confirmation, clarification |
| **Skill** | Domain-scoped command execution (stateless) |
| **Executor** | OS-level process execution via `ProcessBuilder` |
| **Config** | JSON-driven command definitions with aliases |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.10+ (for Whisper STT microservice)

### Whisper STT Setup

VDAS uses a local Python microservice for offline speech-to-text:

```bash
cd whisper-stt
pip install -r requirements.txt
python server.py
```

The Whisper STT server runs at `http://localhost:8000`. VDAS auto-detects it on startup.

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

> **Note:** CLI mode only executes HIGH-confidence, non-dangerous commands. Commands requiring confirmation or clarification are rejected in CLI mode.

### Run Tests

```bash
mvn test
```

---

## Usage

On startup, VDAS prompts for input mode:

```
Select input mode:
  [W] Whisper voice (offline, microphone)
  [K] Keyboard (default)
>
```

- **Voice mode** — Speak a command (e.g., "java version"). VDAS captures audio via Whisper STT, resolves intent, and executes.
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
- **`command`** — OS command string (empty for skill-handled commands like `open-app`)
- **`workingDirectory`** — optional execution directory
- **`aliases`** — optional list of voice/text synonyms for matching

---

## Intent Resolution Pipeline

All user input flows through `IntentResolver`, which produces an immutable `Intent` object:

| Stage | Match Type | Confidence |
|-------|-----------|------------|
| 1. Exact match | Normalized input = normalized command name | `1.0` (HIGH) |
| 2. Alias match | Normalized input = normalized alias | `1.0` (HIGH) |
| 3. Verb-prefix match | `open/launch/start/run <app>` → `open-app` | `1.0` (HIGH) |
| 4. Fuzzy match | Levenshtein similarity ≥ `0.75` threshold | `score` (MEDIUM) |
| 5. Ambiguous | Top-2 candidates within `0.05` margin | `score` (MEDIUM) + candidates |
| 6. Rejection | Below threshold | `0.0` (LOW) |

### Input Normalization

Lowercase → strip punctuation → collapse whitespace → strip leading ASR articles (`the`, `a`, `an`).

### Confidence Bands

| Band | Condition | Source |
|------|-----------|--------|
| **HIGH** | `confidence == 1.0` | Exact, alias, or verb-prefix match |
| **MEDIUM** | `0.75 ≤ confidence < 1.0` | Fuzzy match above threshold |
| **LOW** | `confidence < 0.75` | Below threshold |

`ConfidenceBand` is assigned once during `Intent` construction — never recomputed downstream.

---

## Safety & Execution Gate

After intent resolution, an **ExecutionGate** evaluates the intent based on its confidence band, danger classification, and ambiguity:

| ConfidenceBand | Dangerous | Ambiguous | Decision |
|----------------|-----------|-----------|----------|
| HIGH           | NO        | —         | EXECUTE  |
| HIGH           | YES       | —         | CONFIRM  |
| MEDIUM         | *         | YES       | CLARIFY  |
| MEDIUM         | *         | NO        | CONFIRM  |
| LOW            | *         | *         | REJECT   |
| (unresolved)   | —         | —         | REJECT   |

### Dangerous Commands

Hardcoded set: `quit`, `shutdown`, `restart`, `delete`, `remove`, `format`.

When confirmation is required, VDAS prompts:
```
Are you sure you want to <action>? (yes / no)
```
Accepted: `yes`, `yeah`, `confirm`. Everything else → rejection.

### Ambiguity Detection

An intent is ambiguous when:
- Confidence band is **MEDIUM**
- At least **2 candidate commands** exist
- Top-two score gap **≤ 0.10**

When clarification is required, VDAS prompts:
```
Did you mean:
1. system-info
2. java-version
```
Accepts a number (e.g., `1`) or exact command name. One shot only — no retries, no memory.

**Re-gating:** Clarified intents are passed back through `ExecutionGate`, so dangerous clarified commands still require confirmation.

### Keyboard Shortcut

Typing `q` or `quit` in the interactive loop exits immediately — this bypasses the safety gate by design. Voice/intent-based "quit" always passes through `ExecutionGate`.

---

## Skills

| Skill | Handles | Description |
|-------|---------|-------------|
| **AppLauncherSkill** | `open-app` | Launches whitelisted desktop apps (chrome, vscode, explorer, notepad, calculator) |
| **SystemInfoSkill** | `system-info`, `java-version` | Runs system commands via `CommandExecutor` |
| **FileSystemSkill** | `list-files` | Runs file system commands via `CommandExecutor` |

Skills are stateless, registered explicitly (no reflection), and matched via `SkillRegistry` (first-match).

---

## Project Structure

```
src/main/java/vdas/
├── Main.java                        — Entry point, input mode selection, main loop
├── intent/
│   ├── Intent.java                  — Immutable intent model (raw, normalized, command, confidence, band, candidates)
│   ├── IntentResolver.java          — Deterministic resolution engine (exact → alias → verb-prefix → fuzzy)
│   ├── IntentNormalizer.java        — Input normalization (voice + keyboard)
│   ├── ConfidenceBand.java          — HIGH/MEDIUM/LOW confidence bands
│   ├── AmbiguityDetector.java       — Ambiguity detection interface
│   ├── DefaultAmbiguityDetector.java — Score-based ambiguity detection (gap ≤ 0.10)
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
│   ├── AppLauncherSkill.java        — Whitelisted app launcher (chrome, vscode, etc.)
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
    └── WhisperSpeechInput.java      — Whisper STT microservice client

whisper-stt/
├── server.py                        — Python Whisper STT HTTP server
└── requirements.txt                 — Python dependencies

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
│   ├── AppLauncherSkillTest.java    — Whitelist & canHandle tests
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
- **Single source of truth** — `ConfidenceBand` computed once during Intent construction
- **Single responsibility** — Only `IntentResolver` creates `Intent` objects
- **Stateless skills** — Skills have no memory between executions
- **Safety by default** — Dangerous/ambiguous commands never auto-execute
- **Explicit registration** — No reflection, no DI framework

---

## License

Private project.
