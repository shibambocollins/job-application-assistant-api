# Architecture

This document covers the data model, system architecture, and a representative request flow for the Job Assistant AI backend. The ERD and sequence diagram are Mermaid, rendered natively by GitHub — no image export needed. The system architecture diagram uses real service icons ([Azure](https://icons.terrastruct.com), [React](https://icons.terrastruct.com), [Java](https://icons.terrastruct.com)) via [D2](https://d2lang.com), so it's a pre-rendered SVG checked into this repo instead.

All three diagrams were built by reading the actual entity, service, and controller classes rather than the original design notes, so a couple of details below differ slightly from earlier planning docs. Those are called out where relevant.

---

## 1. Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o| CVS : "has one"
    USERS ||--o{ JOB_APPLICATIONS : "tracks many"
    USERS ||--o{ CHAT_MESSAGES : "sends many"
    JOBS ||--o{ JOB_APPLICATIONS : "applied via"
    JOB_APPLICATIONS ||--o{ ANALYSES : "analyzed by"
    CVS ||--o{ ANALYSES : "analyzed against"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        datetime created_at
    }
    CVS {
        bigint id PK
        bigint user_id FK "one CV per user"
        varchar blob_url
        varchar original_filename
        text extracted_text
        text skills_json
        datetime uploaded_at
    }
    JOBS {
        bigint id PK
        varchar external_id UK "Muse dedup key; null for manually-added jobs"
        varchar title
        varchar company
        text description
        varchar location
        varchar posting_url
        varchar source "MUSE or MANUAL"
        datetime fetched_at
    }
    JOB_APPLICATIONS {
        bigint id PK
        bigint user_id FK
        bigint job_id FK
        varchar status "SAVED, APPLIED, INTERVIEW, OFFER, REJECTED"
        date applied_date
        datetime created_at
    }
    ANALYSES {
        bigint id PK
        bigint job_application_id FK "required"
        bigint cv_id FK "required"
        int match_score
        text missing_skills
        text ai_suggestions
        text strengths
        datetime created_at
    }
    CHAT_MESSAGES {
        bigint id PK
        bigint user_id FK
        text user_message
        text ai_response
        text context_snapshot
        datetime sent_at
    }
```

**Notes on how this maps to the actual JPA entities:**

- The `@Table` names in code are singular for two entities — `cv` and `analysis` — not `cvs`/`analyses`. The diagram uses plural labels for readability; the underlying tables are singular.
- `CVS.user_id` is not a database-level unique constraint (no `unique = true` on the column) — the one-CV-per-user rule is enforced in `CVServiceImpl.uploadCV()`, which throws `DuplicateResourceException` if a CV already exists for that user. It's a true 1:1 in practice, just enforced in the service layer rather than the schema.
- `ANALYSES` only has a foreign key to `job_applications` and `cv` — **not** a direct foreign key to `jobs`. Both `@ManyToOne` relationships on `Analysis` are `optional = false`, so every analysis requires an existing tracked application; there's no "analyze before saving" path in the current implementation, even though that was discussed early on.
- `JOBS.posting_url` stores the original listing URL (captured from Muse's `refs.landing_page`, or entered manually) so users can click through to the real posting.
- Every entity uses the Builder pattern with a protected no-arg constructor (for JPA) and a private all-args constructor via `Builder.build()` — not shown in the ERD since that's a Java-level pattern, not a schema detail.

---

## 2. System Architecture

![System architecture diagram: React frontend calling the Spring Boot REST API over HTTPS with a JWT bearer token, fanning out to the seven services in the service layer, which go through a shared repository layer down to Azure SQL Database, with CVService also reaching Azure Blob Storage, AIService reaching Google Gemini, and DiscoveryService reaching The Muse API.](architecture.svg)

Source: [`docs/architecture.d2`](architecture.d2), rendered with [D2](https://d2lang.com) (`d2 docs/architecture.d2 docs/architecture.svg`). Regenerate it the same way after any architectural change — both the source and the rendered SVG are committed so the diagram never depends on a build step to view.

`JobService` covers manual job CRUD and application status updates; `DiscoveryService` covers auto-discovery via Muse (it delegates the actual HTTP calls to a `MuseApiClient`, omitted here as an implementation detail of the same service). `CVService` similarly delegates the Blob Storage calls to a `BlobStorageService` — shown here as a direct edge for clarity, since architecturally it's still "the CV layer talks to Blob Storage."

No icon exists for Spring Boot or Gemini specifically in the [Terrastruct icon set](https://icons.terrastruct.com), so the diagram uses the Java icon for the Spring Boot node and Google Cloud's generic AI/ML icon for the Gemini node — both are the closest same-vendor-family fallback rather than an exact product icon.

---

## 3. Request Flow: CV Upload

The CV upload path is the most involved single request in the system — it touches file storage, text extraction, an external AI call, and persistence in one transaction. Sequence shown here is for the PDF path (`POST /cv/upload`); a DOCX upload is identical except `WordExtractionService` stands in for `PdfExtractionService`.

```mermaid
sequenceDiagram
    actor Client
    participant Controller as CVController
    participant CVService
    participant Blob as BlobStorageService
    participant PdfSvc as PdfExtractionService
    participant CleanSvc as TextCleaningService
    participant AISvc as AIService
    participant Gemini as Google Gemini API
    participant Repo as CVRepository
    participant DB as Database

    Client->>Controller: POST /cv/upload (multipart file, JWT)
    Controller->>CVService: uploadCV(file, email)
    CVService->>Repo: findByUserId(userId)
    Repo->>DB: SELECT
    DB-->>Repo: no existing row
    Repo-->>CVService: Optional.empty()

    CVService->>Blob: upload(file, generatedFileName)
    Blob-->>CVService: blobUrl

    CVService->>PdfSvc: extractText(file)
    PdfSvc-->>CVService: rawText

    CVService->>CleanSvc: clean(rawText)
    CleanSvc-->>CVService: extractedText

    CVService->>AISvc: extractCVData(extractedText)
    AISvc->>Gemini: POST /v1beta/models/{model}:generateContent
    Gemini-->>AISvc: structured JSON (skills, education, certifications, projects, experience)
    AISvc-->>CVService: CVDataResult

    CVService->>Repo: save(CV)
    Repo->>DB: INSERT
    DB-->>Repo: saved row
    Repo-->>CVService: CV

    CVService-->>Controller: CVResponse(id, "CV uploaded successfully")
    Controller-->>Client: 200 OK
```

If a CV already exists for that user, the flow short-circuits right after the `findByUserId` check with a `409 Conflict` (`DuplicateResourceException`) — re-uploading requires `PUT /cv/upload` instead, which follows the same pipeline but deletes the old blob before writing the new one.

---

## Related

- [README.md](../README.md) — setup, running locally, API endpoint list
- `AI_Job_Assistant_Project_Context.md` — product vision, decision history, and challenges hit during development (broader and more narrative than this doc; this file is the up-to-date structural reference)
