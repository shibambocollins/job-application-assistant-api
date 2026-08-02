# AI Job Assistant — Full Project Context

**Purpose of this document:** This is a complete context brief for Claude (or any collaborator) picking up work on this project. It covers the product vision, actual current architecture, what's been built, what changed from the original plan and why, real challenges hit during development, and what's left to build.

**Developer:** Collins (Ntsobokwane Collins Shibambo) — final-year Diploma in ICT Application Development student at CPUT (Cape Peninsula University of Technology), Cape Town. Holds Azure AZ-900, OCI Foundations Associate, Google Foundations of Cybersecurity certs.

---

## 1. Product Overview

AI Job Assistant is a web application that helps students, recent graduates, and junior developers take control of their job search. Instead of sending CVs blindly and waiting for responses that never come, users get a personal AI-powered assistant that reads their CV, discovers matching jobs automatically, analyzes how well they fit each role, and tells them exactly what they need to improve.

**Core value proposition:** personal (uses your actual CV), practical (track, analyze, improve), AI used to solve a real problem rather than for novelty.

**Target users:** final-year students, recent graduates, junior developers with 0–2 years experience — people actively applying, getting rejected or ignored, and unsure why.

**Problems it solves:**
- No feedback on rejections
- Poor application tracking across scattered platforms
- Guessing which skills actually matter for a role
- Generic online advice that isn't personalized to the user's actual CV

---

## 2. Core Features

**Job Discovery** — user uploads CV → AI extracts skills → app automatically queries The Muse API → real job listings returned, ranked by fit, without the user typing a single search term.

**CV Analysis** — for any job (discovered or manually added), AI compares CV against the job description and returns: match score, missing skills, improvement suggestions, and relevant CV strengths.

**Application Tracker** — dashboard of all applications with status (Saved, Applied, Interview, Offer, Rejected), sortable by match score/date/company.

**AI Chat** — conversational interface with context of the user's CV and all stored jobs, so it can answer things like "Which job am I most qualified for?" or "What one skill would open the most roles?"

---

## 3. User Flow (End to End)

```
Open App
   ↓
Sign up / Login (JWT Auth)
   ↓
Upload CV (PDF → text extraction → AI structured extraction)
   ↓
AI extracts skills, education, certifications, projects, experience
   ↓
   ├─→ Discover Jobs (Muse API, ranked matches)
   └─→ Enter Job Manually (company, role, description)
   ↓
AI Analyzes CV vs Job (match %, missing skills, suggestions)
   ↓
AI Chat Insights ("Which job fits me most?")
   ↓
Track Application Status (Applied, Interview, Offer, Rejected)
   ↓
Improve & Re-apply
```

---

## 4. Tech Stack — Original Plan vs Actual (Important — Read This)

The stack changed meaningfully during development due to real-world constraints. This section is the single source of truth for what's actually being used.

| Layer | Originally Planned | Actually Used | Why It Changed |
|---|---|---|---|
| Backend framework | Spring Boot | Spring Boot | No change |
| Backend hosting | Azure App Service F1 (free) | Azure App Service F1 (free) | No change — not yet deployed |
| Database | Azure SQL DB | **H2 in-memory (dev)** → Azure SQL DB planned | Azure SQL still the target; H2 used for local dev/testing so far |
| File storage | Azure Blob Storage | **Local disk (`uploads/` folder)** | Blob Storage not yet integrated — deferred, working locally first |
| AI provider | **Azure OpenAI (GPT-4)** | **Google Gemini API** (`gemini-flash-latest`) | Azure OpenAI structurally blocked for Azure for Students subscriptions — see Challenges section |
| Job data | The Muse API | The Muse API | Confirmed working, free tier, no auth needed for testing (500 req/hour cap) |
| Auth | Spring Security + JWT | Spring Security + JWT (JJWT 0.12.6) | No change |
| Frontend | React | React (not yet started) | Planned next after backend AI/analysis layers complete |
| Frontend hosting | Vercel | Vercel (not yet deployed) | Confirmed plan — free tier + GitHub Student Pack Pro upgrade |
| Domain | Namecheap `.me` via GitHub Student Pack | Same, not yet purchased | No change |
| CI/CD | GitHub Actions | GitHub Actions (not yet configured) | No change |

**Bottom line on AI provider:** Azure OpenAI was the original plan to keep everything in one cloud ecosystem, but Azure for Students subscriptions are **structurally excluded** from Azure OpenAI access — confirmed after testing 5 different "allowed regions" on the subscription (uaenorth, italynorth, southafricanorth, austriaeast, brazilsouth), none of which support Azure OpenAI, and confirming via Microsoft's own support channels that student subscriptions aren't a supported path regardless of region. Google Gemini's free tier was selected as the replacement — genuinely free (no card required), good enough quality for CV parsing and structured extraction.

---

## 5. Actual Data Model (Final Hybrid Design)

The entity design went through real iteration — initial Claude-drafted UML was cross-checked against a ChatGPT review, and the best parts of each were merged into a final hybrid.

**Entities:**
- `User` — id, email, passwordHash, fullName, createdAt
- `CV` — id, userId (**unique constraint — enforces 1:1 with User**), blobUrl, originalFilename, extractedText (`@Lob`), skillsJson (`@Lob`), uploadedAt
- `Job` — id, externalId (**unique — Muse deduplication guard**), title, company, description (`@Lob`), location, source (enum: MUSE/MANUAL), fetchedAt
- `JobApplication` — id, user (FK), job (FK), status (enum: SAVED/APPLIED/INTERVIEW/OFFER/REJECTED), appliedDate, createdAt
- `Analysis` — id, jobId, jobApplicationId (nullable — supports analyzing before saving/applying), cvId (snapshot reference), matchScore, missingSkills, aiSuggestions, strengths, createdAt
- `ChatMessage` — id, userId, userMessage, aiResponse, contextSnapshot (optional), sentAt

**Key design decisions and why:**

1. **CV is 1:1 with User** (not 1:many) — enforced via `@Column(unique = true)` on `CV.userId`. Keeps the system simple: no ambiguity about "which CV" during AI calls or discovery. If CV versioning is needed later, the correct approach is a separate `cv_history` table, not relaxing this constraint.

2. **Job and JobApplication are separate tables** — `Job` acts as a cache of listings (especially from Muse), `JobApplication` is lightweight tracking data (status, dates) tied to a user. This avoids duplicating large job description text across multiple users saving the same listing. `externalId` on `Job` is the deduplication key for Muse-sourced jobs.

3. **Skills stored as JSON blob, not a separate entity** — `CV.skillsJson` holds the full AI-extracted structured breakdown (skills, education, certifications, projects, experience) as a single JSON string. A dedicated `Skill` entity/table was considered and rejected as over-engineering for what's essentially a flat data blob fed to/from AI prompts.

4. **Analysis keeps both `jobId` and `jobApplicationId`** — this was a deliberate design debate. Having only `jobApplicationId` would force every analysis to happen after a job is saved/applied to. Keeping `jobId` as well allows analyzing a job's fit *before* deciding to save or apply — a better UX (see a Muse job → analyze first → decide whether to track it).

**Service layer** (business logic — separate from entities/domain):
- `AuthService` — register, login, JWT validation
- `CVService` — upload, extraction pipeline, retrieval
- `JobService` — manual job CRUD, status updates
- `DiscoveryService` — Muse API querying, job creation/deduplication
- `AIService` — Gemini API calls (CV extraction, job-fit analysis, chat)
- `AnalysisService` — orchestrates CV vs Job comparison (not yet built)
- `ChatService` — conversational AI with context (not yet built)

---

## 6. What's Actually Built and Tested (Current Progress)

```
✅ Model layer (all 6 entities, JPA-annotated, Builder pattern throughout)
✅ Repository layer (Spring Data JPA interfaces for all entities)
✅ Auth — Spring Security + JWT (register, login, protected routes, /auth/me)
✅ CV upload — file storage, PDFBox text extraction, text cleaning, Gemini AI 
   structured extraction (skills, education, certifications, projects, experience)
✅ Job layer — manual job CRUD, JobApplication tracking, status updates, 
   ownership checks (users can only modify their own applications)
✅ Muse API discovery — automatic job fetching based on CV, deduplication via 
   externalId, JobApplication auto-created with SAVED status
⬜ Analysis service — CV vs Job AI-powered matching (not yet built)
⬜ Chat service — conversational AI assistant (not yet built)
⬜ Frontend — React app (not yet started)
⬜ Deployment — Vercel + Azure App Service (not yet done)
```

All completed layers have been tested end-to-end via Postman with dedicated test collections (auth flow — 13 tests; jobs/discovery — 11 tests) covering success paths, auth rejection, ownership violations, and edge cases.

---

## 7. Real Challenges Faced During Development

This section exists so future work (or a new Claude session) doesn't repeat solved problems.

**Azure OpenAI blocked entirely on Azure for Students** — deployment repeatedly failed with `RequestDisallowedByAzure` policy errors across every region tried. Root cause: Azure for Students subscriptions get assigned a restricted "allowed regions" policy that has zero overlap with Azure OpenAI's actual supported regions, and Microsoft support confirms student subscriptions are not a supported path for Azure OpenAI regardless of region. **Resolution:** switched to Google Gemini API (free tier, no card required).

**Spring Security circular dependency** — `JwtAuthenticationFilter` depended on `UserDetailsService`, which was defined as a `@Bean` inside `SecurityConfig`, which itself depended on `JwtAuthenticationFilter`. Classic bean cycle. **Resolution:** extracted `UserDetailsService` into its own standalone `CustomUserDetailsService` class annotated `@Service`, breaking the cycle.

**Duplicate `PasswordEncoder` bean** — defined once in a standalone `PasswordConfig` class and again inside `SecurityConfig`. **Resolution:** deleted `PasswordConfig`, kept the bean only in `SecurityConfig`.

**Spring Security blocking `/error` (the "double bounce")** — all exception-based edge case tests (duplicate email, wrong password, etc.) returned blank `403 Forbidden` instead of proper error JSON. Root cause: Spring's internal forward to `/error` to render exception payloads was itself being intercepted and blocked by Spring Security since `/error` wasn't explicitly permitted. **Resolution:** added `.requestMatchers("/error").permitAll()` to the security filter chain.

**Gemini API model churn** — `gemini-2.0-flash` and `gemini-1.5-flash` were retired mid-project (404 errors), then `gemini-2.5-flash` returned a 404 with an explicit message that it's "no longer available to new users." **Resolution:** switched to the `gemini-flash-latest` alias, which Google guarantees always points to their current recommended Flash model — avoids this exact problem recurring.

**Gemini API key format transition** — Google is mid-rollout of a new "Auth key" format (`AQ.Ab...`) replacing the legacy "Standard key" format (`AIzaSy...`). The new format requires the key to be sent via the `x-goog-api-key` HTTP header, not the old `?key=` query parameter — using the old method against a new-format key silently produced 404s. **Resolution:** switched to header-based auth in `AIServiceImpl`.

**Spring WebClient URI template bug** — using `.uri("/v1beta/models/{model}:generateContent", model)` caused subtle 404s, likely due to the colon immediately after the template variable confusing Spring's URI parser. **Resolution:** built the path as a plain concatenated string instead of relying on template variable substitution for that segment.

**H2 in-memory database resets on every restart** — caused confusing "Upload a CV first" errors during discovery testing, when in fact the CV had simply been wiped by an app restart. Not a bug — expected behavior of `jdbc:h2:mem:testdb`. Worth remembering when testing multi-step flows after a restart.

**Postman multipart form-data mistakes** — file upload tests initially failed with `Required part 'file' is not present` due to leaving the form-data key name blank while still selecting a file as the value. Resolved by ensuring the key name matches the `@RequestPart("file")` parameter name exactly.

**The Muse API doesn't support free-text keyword search** — original assumption was that CV skills could be mapped into an arbitrary search query. In reality, Muse only filters by fixed `category`, `level`, `location`, and `company` parameters. **Resolution:** hardcoded `category=Software Engineering` and `level=Entry Level` for discovery calls, since this matches the target user base (junior developers) exactly — a reasonable simplification for MVP rather than building a complex skill-to-category mapping table.

**Gemini free tier transient errors (429, 503)** — rate limiting and temporary service unavailability are expected occasionally on the free tier; these are not code bugs and typically resolve on retry within a minute.

---

## 8. Alternative Tools Considered (Free-Tier Focused)

Since this is a student project with cost constraints, several alternatives were evaluated at each layer:

**AI Provider:**
- Azure OpenAI (GPT-4) — originally planned, **blocked** by Azure for Students subscription policy
- OpenAI API (platform.openai.com) — works immediately, no region restrictions, but requires billing setup (pay-per-token from first request, no free tier)
- **Google Gemini API — chosen.** Genuinely free tier, no card required, sufficient quality for structured CV extraction
- Anthropic Claude API — viable alternative, also requires billing setup, no ongoing free tier

**Job Listings API:**
- **The Muse API — chosen.** Free, no authentication required for basic use (500 requests/hour without registering an app; 3600/hour if registered), tech-focused listings, but limited to category/level/location/company filters (no free-text search)

**Frontend Hosting:**
- **Vercel — chosen.** Free tier, custom domain support, GitHub Student Pack upgrades it to Pro for free
- Netlify — comparable alternative, also has Student Pack credits
- Azure Static Web Apps — considered since already in the Azure ecosystem, but more CORS/routing complexity for a React + separate backend setup than Vercel

**Backend Hosting:**
- **Azure App Service F1 (free tier) — chosen.** Simplest for a student project despite sleep-on-inactivity limitation
- Azure Container Apps — more modern, scales to zero, but more setup complexity
- Azure Spring Apps — purpose-built for Spring Boot, but free tier too limited for practical use

**Database:**
- H2 in-memory — currently used for local development/testing
- **Azure SQL DB — planned for production**, covered by Azure for Students credit
- MongoDB Atlas — considered as an alternative if Azure SQL costs become an issue; GitHub Student Pack includes $200 credit

**Domain:**
- **Namecheap — chosen.** Free `.me` domain for 1 year via GitHub Student Pack

---

## 9. Deferred / Not Yet Implemented

Deliberately postponed until core functionality (CV → Job → Analysis → Chat) is fully working end-to-end:

- GlobalExceptionHandler + custom exception hierarchy (currently using generic `RuntimeException` messages)
- Validation annotations on DTOs (`@NotBlank`, `@Email`, etc.)
- File type validation on CV upload (currently accepts any file passed as "file")
- More precise HTTP status codes (currently many error paths return 500 rather than proper 400/404/409)
- Azure Blob Storage integration (currently storing files to local disk)
- OCR support for scanned/image-based PDFs
- More advanced skill-to-job-category mapping for Muse discovery (currently hardcoded to "Software Engineering" / "Entry Level")
- Logging improvements (currently relying on default Spring Boot console logging)
- CV versioning/history (currently strictly 1:1, one active CV per user)

---

## 10. Planned Next Modules (Priority Order)

1. **Analysis Service** — AI-powered CV vs Job comparison producing match score, missing skills, and suggestions (builds on the same Gemini integration pattern used in CV extraction)
2. **Chat Service** — conversational AI assistant with context built from the user's CV and stored jobs
3. **Frontend (React)** — dashboard, CV analyzer, job discovery view, chat interface
4. **Deployment** — Vercel (frontend) + Azure App Service (backend) + Azure SQL DB migration from H2
5. **Deferred improvements** (see Section 9) — tackled after the above are functionally complete

---

## 11. Development Philosophy

The approach throughout has been to prioritize complete, working end-to-end features over premature optimization or refactoring. Each layer (Model → Repository → Auth → CV → Job → Discovery) has been built, tested via Postman, and confirmed working before moving to the next. Architectural decisions (like the entity hybrid design) were made deliberately through comparing multiple AI-assisted design reviews rather than accepting the first draft. The plan is to reach a stable, fully working Version 1 (including Analysis, Chat, and a basic frontend) before circling back to production-grade concerns like proper exception handling, validation, and cloud storage migration.
