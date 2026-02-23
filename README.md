# VDAS — Voice-Driven Developer Automation System

**Offline, deterministic voice automation for developers.** Execute system commands using natural voice input or keyboard — no cloud, no AI, no NLP libraries.

---

## Architecture Overview

```
Raw Input (voice / keyboard)
  → CommandSplitter (parses "and", "then" → List<Step>)
      → FOR EACH Step:
          → IntentNormalizer (lowercase, strip punctuation, remove articles)
              → IntentResolver (exact → alias → verb-prefix → fuzzy)
                  → ContextualIntentResolver (enriches Intent using SessionContext)
                      → Intent { rawInput, normalizedInput, resolvedCommand, confidence, band, parameters, candidates }
                          → ExecutionGate (checks Danger / Ambiguity / prompts Confirmation or Clarification)
                              → SkillRegistry
                                  → Skill.execute(Intent)
                                  → SessionContext.update(Intent, Command, Skill)
```

| Layer | Responsibility |
|-------|---------------|
| **Parsing** | Split compound commands (e.g., "X and then Y") into sequential steps (Max 3) |
| **Speech** | Offline voice capture via Whisper STT (Python microservice) |
| **Intent** | Resolve command → compute confidence → Contextual follow-up enrichment |
| **Context** | Maintain session state (last app opened) to enable chained navigation |
| **Safety** | Execution gate, danger classification, confirmation, clarification |
| **Skill** | Domain-scoped command execution (AppLauncher, FileSystem, Browser, etc.) |
| **Config** | JSON-driven command definitions with whitelisted apps and websites |

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

## Multi-Step Commands

VDAS supports sequential execution of up to **3 steps** in a single input string. Use the following connectors:
- `and`
- `then`
- `and then`

**Example:** `"open chrome and then open youtube"`
1.  **Step 1:** Opens Chrome.
2.  **Step 2:** Navigates Chrome to YouTube (Context-aware).

> [!IMPORTANT]
> If any step fails (e.g., a dangerous command is rejected, or clarification is cancelled), all subsequent steps are **aborted** for safety.

---

## Session Context & Follow-ups

VDAS maintains a transient **SessionContext** to enable fluid, multi-step interactions without repeating targets.

### Contextual Resolution Strategies
1.  **Repeat:** "again", "repeat that" → Re-runs the last successful action.
2.  **Close-it:** "close it", "close that" → Closes the last opened application.
3.  **Contextual Navigation:** "open youtube" (after "open chrome") → Upgrades "youtube" from an application to a URL target for the active browser.

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
| **BrowserSkill** | `open-app` + `url` | Navigates whitelisted browsers to whitelisted websites |
| **AppLauncherSkill** | `open-app` | Launches whitelisted desktop apps (chrome, vscode, etc.) |
| **SystemInfoSkill** | `system-info`, `java-version` | Runs system commands via `CommandExecutor` |
| **FileSystemSkill** | `list-files` | Runs file system commands via `CommandExecutor` |

Skills are stateless, registered explicitly (no reflection), and matched via `SkillRegistry` (first-match). `BrowserSkill` is prioritized to intercept contextual URL navigation.

---

## Project Structure

```
src/main/java/vdas/
├── Main.java                        — Entry point, input execution loop
├── agent/
│   └── AgentState.java              — Lifecycle states (IDLE, RESOLVING, etc.)
├── intent/
│   ├── Intent.java                  — Immutable intent model
│   ├── IntentResolver.java          — Primary resolution engine
│   ├── ContextualIntentResolver.java — Strategy 0: Context enrichment
│   ├── CommandSplitter.java         — Multi-step command parser
│   ├── IntentNormalizer.java        — Input normalization
│   └── LevenshteinDistance.java     — Fuzzy matching engine
├── safety/
│   ├── ExecutionGate.java           — Safety evaluator (EXECUTE/CONFIRM/CLARIFY)
│   ├── ConfirmationManager.java     — Yes/no interaction
│   └── ClarificationPrompt.java     — Ambiguity resolution
├── skill/
│   ├── Skill.java                   — Skill interface
│   ├── BrowserSkill.java            — Context-aware navigation
│   ├── AppLauncherSkill.java        — Local app launcher
│   ├── SystemInfoSkill.java         — System metrics
└── session/
│   └── SessionContext.java          — In-memory session state
├── speech/
│   └── WhisperSpeechInput.java      — STT microservice client
└── executor/
    └── CommandExecutor.java         — ProcessBuilder wrapper

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
