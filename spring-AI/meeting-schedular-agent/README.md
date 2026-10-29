# AI Meeting Scheduler

Spring Boot application that schedules meetings from natural-language prompts using **Spring AI**, with deterministic validation, conflict detection, and MySQL persistence.

## Version choices

| Component | Version | Why |
|-----------|---------|-----|
| Java | 21 | Required by the brief; records and virtual threads ready. |
| Spring Boot | 3.4.5 | Stable Boot 3.4 line compatible with Spring AI 1.0.x. |
| Spring AI | 1.1.0 (BOM) | Gemini Developer API (`spring-ai-starter-model-google-genai`). Pairs with Boot 3.4.x. |
| MySQL | 8.4 | JPA/Hibernate schema (`ddl-auto: update`), UTC timestamps. |

## Architecture

```text
controller/     REST API (structured + /ai)
service/          MeetingService, MeetingConflictService, MeetingAiService
ai/               MeetingIntentParser, MeetingIntentValidator, MeetingToolService (@Tool)
entity/           Meeting, MeetingParticipant
repository/       JPA + pessimistic lock query for scheduling
dto/              API and AI response models
validation/       Deterministic time rules (Clock-injected)
exception/        Global exception handling
config/           Clock bean
```

**Design principle:** the LLM only **interprets** text into `MeetingIntent`. Java services validate times, normalize emails, detect conflicts, and persist data. Tool methods (`MeetingToolService`) delegate to the same services and require `confirmed=true` before writes.

### Concurrency

A naive `check conflicts → insert` flow can double-book under load: two requests can both pass the check before either commits.

This app uses a single `@Transactional` create flow that:

1. Runs `findOverlappingScheduledForParticipantsForUpdate` with **`PESSIMISTIC_WRITE`** locks on overlapping scheduled meetings for the requested participants.
2. Re-checks conflicts in the same transaction.
3. Inserts only if still clear.

**Scope:** protects against races on overlapping rows that MySQL locks. It does not replace business validation; extreme edge cases (new participant rows with no overlapping meeting yet) still rely on transactional serialization within the same participant/time overlap set.

## Project structure

```text
src/main/java/com/example/meetingscheduler/
  MeetingSchedulerApplication.java
  ai/
  config/
  controller/
  dto/
  entity/
  exception/
  mapper/
  repository/
  service/
  validation/
src/main/resources/
  application.yml
src/test/java/...
docker-compose.yml
.env.example
```

## Run with IntelliJ IDEA

### 1. Open the project

1. **File → Open** and select the `spring-AI` folder (the one containing `pom.xml`).
2. Trust the project if prompted.
3. Wait for **Maven import** to finish (status bar). If needed: right-click `pom.xml` → **Maven → Reload project**.
4. Set **Project SDK** to **Java 21**: **File → Project Structure → Project** → SDK **21**, language level **21**.

### 2. Start MySQL

From a terminal in the project root (or IntelliJ **Terminal**):

```bash
docker compose up -d
```

### 3. Environment variables

The repo includes shared run configs under **`.run/`**:

| Configuration | Use when |
|---------------|----------|
| **MeetingSchedulerApplication** | Normal run/debug (Spring Boot, fastest) |
| **MeetingSchedulerApplication (Maven)** | Fallback if the IDE module name does not match |
| **All Tests** | Run unit + integration tests from the IDE |

1. Open **Run → Edit Configurations…** and pick **MeetingSchedulerApplication**.
2. Set **`GEMINI_API_KEY`** (from [Google AI Studio](https://aistudio.google.com/apikey)) when you want `/api/meetings/ai`. The app **starts without** a key; REST APIs work without Gemini.
3. Optional: copy `.env.example` to `.env`, fill in secrets, and use the **EnvFile** plugin to load `$PROJECT_DIR$/.env` on run (`.env` is gitignored).

**Quick run without a saved config:** open `MeetingSchedulerApplication.java` and click the green **Run** gutter icon next to `main`. Add the same environment variables in the run configuration IntelliJ creates.

### 4. Run or debug

- Choose **MeetingSchedulerApplication** in the run configuration dropdown.
- Click **Run** (Shift+F10) or **Debug** (Shift+F9).
- App URL: `http://localhost:8080`

If you see **Error: Module not specified** or unknown module `ai-meeting-scheduler`:

- **Maven → Reload project**, then try again, **or**
- Use **MeetingSchedulerApplication (Maven)** instead.

### 5. Run tests in IntelliJ

- Use **All Tests**, or right-click `src/test/java` → **Run 'Tests in …'**.
- Integration tests need **Docker** running (Testcontainers); they are skipped if Docker is unavailable.

## Run locally (CLI)

### 1. Start MySQL

```bash
docker compose up -d
```

### 2. Configure environment

Copy `.env.example` to `.env` and set `GEMINI_API_KEY`.

PowerShell:

```powershell
$env:GEMINI_API_KEY="AIza..."
$env:DB_HOST="localhost"
$env:DB_USER="meeting_user"
$env:DB_PASSWORD="meeting_pass"
$env:MEETING_DEFAULT_TIME_ZONE="Asia/Kolkata"
```

### 3. Run the app

```bash
mvn spring-boot:run
```

### 4. Run tests

Integration tests use Testcontainers MySQL (Docker required). Unit tests run without Docker; integration tests are **skipped** when Docker is unavailable.

Use **Maven 3.6.3+** (Spring Boot 3.4 defaults need it). This repo pins older compiler/surefire plugins for Maven 3.3.x, but upgrading Maven is recommended.

```bash
mvn test
```

## REST examples

### Create meeting (structured)

```bash
curl -s -X POST http://localhost:8080/api/meetings ^
  -H "Content-Type: application/json" ^
  -d "{\"topic\":\"Project Review\",\"description\":\"Q4\",\"startTime\":\"2026-10-29T08:30:00Z\",\"endTime\":\"2026-10-29T09:30:00Z\",\"participants\":[{\"email\":\"alice@example.com\"},{\"email\":\"bob@example.com\"}],\"createdBy\":\"organizer\"}"
```

### Natural language (interpret + execute)

```bash
curl -s -X POST http://localhost:8080/api/meetings/ai ^
  -H "Content-Type: application/json" ^
  -d "{\"prompt\":\"Schedule a project review tomorrow from 2 PM to 3 PM with alice@example.com and bob@example.com.\",\"timeZone\":\"Asia/Kolkata\",\"confirmSchedule\":true}"
```

**Success (201):** `outcome=SCHEDULE`, `meeting` populated with id, topic, UTC instants, participants.

**Conflict (409):** `conflicts[]` with `meetingId`, `topic`, times, `conflictingParticipantEmails`.

**Clarification (422):** `outcome=CLARIFICATION_REQUIRED`, `missingFields`, `clarificationQuestion` (e.g. missing time or email-only names like "Alice").

### Check availability

```bash
curl -s -X POST http://localhost:8080/api/meetings/availability ^
  -H "Content-Type: application/json" ^
  -d "{\"participantEmails\":[\"alice@example.com\"],\"startTime\":\"2026-10-29T08:30:00Z\",\"endTime\":\"2026-10-29T09:30:00Z\"}"
```

### Tool-calling agent (optional)

```bash
curl -s -X POST http://localhost:8080/api/meetings/ai/tools ^
  -H "Content-Type: application/json" ^
  -d "{\"prompt\":\"Check whether alice@example.com is free tomorrow 2pm-3pm Asia/Kolkata\",\"timeZone\":\"Asia/Kolkata\"}"
```

## Configure LLM provider

Uses **Google Gemini** via `spring-ai-starter-model-google-genai`:

- `GEMINI_API_KEY` or `GOOGLE_API_KEY` — Gemini Developer API key for `/api/meetings/ai`
- `GEMINI_MODEL` — default `gemini-2.0-flash`
- `spring.ai.google.genai.*` in `application.yml`

## Troubleshooting

| Issue | Fix |
|-------|-----|
| DB connection refused | `docker compose ps`, verify port 3306. |
| Hibernate schema errors | Ensure MySQL 8.4+ is running and the DB user can create/alter tables. |
| 404 on `/api/meetings/ai` | No `GEMINI_API_KEY` configured — set the key and restart. |
| 502 on `/api/meetings/ai` | See `details` in JSON — invalid API key, quota, or SSL (below). |
| `PKIX path building failed` / `certificate_unknown` | On **Windows**, the app merges **JDK cacerts + Windows ROOT** at startup (`SSL_MERGE_WINDOWS_ROOT=true` by default). If it still fails, use a custom truststore (below). |
| Times wrong for "tomorrow" | Set `timeZone` on the request or `MEETING_DEFAULT_TIME_ZONE`. |

### SSL: `unable to find valid certification path to requested target`

Your JVM does not trust HTTPS to Google APIs (common on corporate networks with SSL inspection).

**Option A — Import your company root CA into the JDK used by IntelliJ**

1. Export your organization root CA (`.cer`) from Windows cert manager or IT.
2. Find the JDK IntelliJ uses: **File → Project Structure → SDK** (prefer **64-bit** JDK 21, not `Program Files (x86)`).
3. Import (run **cmd as Administrator**, adjust paths):

```text
keytool -importcert -alias corp-root -file C:\path\to\corp-root.cer ^
  -keystore "C:\Program Files\Java\jdk-21\lib\security\cacerts" -storepass changeit
```

4. Restart IntelliJ and the application.

**Option B — Custom truststore (recommended if you cannot modify `cacerts`)**

```text
keytool -importcert -alias corp-root -file C:\path\to\corp-root.cer ^
  -keystore C:\Users\YOU\corp-truststore.jks -storepass changeit
```

Set in **Run → Edit Configurations → Environment variables**:

```text
SSL_TRUST_STORE=C:\Users\YOU\corp-truststore.jks
SSL_TRUST_STORE_PASSWORD=changeit
```

Or in `.env` (gitignored). Restart the app.

**Option C — Test outside corporate network** (mobile hotspot) to confirm the app and API key work when TLS is not intercepted.

## Example conversation

**User:** Schedule a project review tomorrow from 2 PM to 3 PM with alice@example.com and bob@example.com.

**App:** Parses intent in `Asia/Kolkata` (or default zone), validates emails and interval, checks overlaps (`existing.start < requested.end AND existing.end > requested.start`), creates the meeting if clear, returns meeting id and participants.

If Alice overlaps 2:30–3:30 PM, response is **409** with the conflicting meeting and Alice’s email; nothing is scheduled.
