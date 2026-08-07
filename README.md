# Job Assistant AI — Backend

AI-powered job search assistant. Upload your CV, get real tech job listings ranked by fit, see exactly which skills are missing for each role, and track every application in one place.

**Status:** Deployed and live. Core product (CV parsing, discovery, fit analysis, chat, application tracking, auth) is complete and tested.

**Live API:** https://job-assistant-api.azurewebsites.net
**Live app:** https://job-assistant-web.vercel.app ([frontend repo](https://github.com/shibambocollins/job-assistant))

> Runs on Azure's free App Service tier, which sleeps after inactivity — the first request after a while can take 20–30s to wake up. Not a bug, just the cost of a genuinely free-tier deployment.

---

## What It Does

1. **Sign up** — email/password, or Google Sign-In
2. **Upload CV** — PDF or DOCX, stored in Azure Blob Storage
3. **AI reads it** — Gemini extracts skills, education, certifications, projects, and experience
4. **Discover jobs** — auto-pulls real tech listings from The Muse API (Software Engineering, Data & Analytics, Science & Engineering, Design & UX — entry level), deduplicated against jobs already fetched
5. **Fit analysis** — for any job (discovered or added manually), AI scores the match, lists missing skills, strengths, and concrete suggestions — looking at skills, education, experience, and CV structure, not just a keyword match
6. **Track applications** — dashboard with status (Saved, Applied, Interview, Offer, Rejected), paginated, with a link back to the original posting
7. **Ask AI** — chat with context of your CV and tracked jobs ("Which job am I most qualified for?")
8. **Manage your account** — change password, forgot-password email flow, or delete your account and everything tied to it

---

## Tech Stack

| Layer | Choice |
|---|---|
| Backend | Spring Boot 3.3.5, Java 21 |
| Auth | Spring Security + JWT (JJWT 0.12.6), BCrypt, Google Sign-In (OAuth ID token) |
| Database | H2 in-memory (local dev) / Azure SQL Database, serverless tier (production) |
| File storage | Azure Blob Storage (CVs) |
| AI | Google Gemini API (`gemini-flash-latest`) — CV extraction, fit analysis, chat |
| Job data | The Muse API (free, no auth required) |
| PDF/DOCX parsing | Apache PDFBox / Apache POI |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, react-router v7 — [separate repo](https://github.com/shibambocollins/job-assistant) |
| Backend hosting | Azure App Service (Linux, free tier) |
| Frontend hosting | Vercel |

**Why Gemini instead of Azure OpenAI:** Azure for Students subscriptions are structurally excluded from Azure OpenAI access — confirmed by testing every allowed region and via Microsoft support. Gemini's free tier needs no card and is good enough for structured CV extraction and fit analysis.

---

## Running Locally

### Prerequisites
- JDK 21+
- A Google Gemini API key (free tier is fine)

No local database setup needed — the `dev` profile runs on an in-memory H2 database by default.

### Setup

```bash
git clone https://github.com/shibambocollins/job-application-assistant-api.git
cd job-application-assistant-api

# Create src/main/resources/application-secret.properties (gitignored)
cat > src/main/resources/application-secret.properties << 'EOF'
jwt.secret=your-local-dev-secret-change-this
gemini.api.key=your-gemini-api-key
azure.storage.connection-string=your-azure-blob-connection-string
google.oauth.client-id=your-google-oauth-client-id
EOF

./mvnw spring-boot:run
```

Server runs on `http://localhost:8080`. Swagger UI is available at `/swagger-ui.html`. The H2 console is enabled in dev at `/h2-console`.

---

## API Endpoints

```
POST   /auth/register              # Sign up
POST   /auth/login                 # Login (returns JWT)
POST   /auth/google                # Google Sign-In
POST   /auth/forgot-password       # Request password reset email
POST   /auth/reset-password        # Complete password reset
POST   /auth/change-password       # Change password (authenticated)
GET    /auth/me                    # Current user profile
DELETE /auth/me                    # Delete account and all associated data

POST   /cv/upload                  # Upload CV (multipart)
PUT    /cv/upload                  # Replace CV
GET    /cv/my-cv                   # Get your CV + extracted data

GET    /jobs                       # List your tracked applications
POST   /jobs                       # Add a job manually
PUT    /jobs/{id}                  # Update job details
PATCH  /jobs/{id}/status            # Update application status
DELETE /jobs/{id}                  # Remove a tracked application
POST   /jobs/discover              # Auto-discover jobs via The Muse API

POST   /jobs/{id}/analysis         # Run AI fit analysis for a job
GET    /jobs/{id}/analysis         # Get the latest analysis for a job

POST   /chat                       # Send a chat message
GET    /chat/history                # Chat history
```

All routes except `/auth/register`, `/auth/login`, `/auth/google`, `/auth/forgot-password`, and `/auth/reset-password` require `Authorization: Bearer <token>`.

Registration and password-reset requests are rate-limited (5/hour per IP) to prevent abuse.

---

## Testing

125+ tests: Mockito unit tests for every service, plus full-stack `MockMvc` integration tests (real HTTP + Spring Security + JPA) for auth and job flows.

```bash
./mvnw test
```

---

## Deployment

- **Backend:** Azure App Service, deployed via GitHub Actions on push to `main` (falls back to `az webapp deploy` directly when Actions is unavailable)
- **Frontend:** Vercel, auto-deployed from the [frontend repo](https://github.com/shibambocollins/job-assistant)
- **Database:** Azure SQL Database (serverless, auto-pauses when idle — same cold-start tradeoff as the App Service tier)

---

## What's Planned

- South Africa-specific job filtering (needs a job data source with real SA coverage — The Muse API doesn't have meaningful SA listings)
- Cover letter generator
- Email notifications for status changes

---

## Data Model

- **User** — account info
- **CV** — one per user, extracted text + structured AI data (skills, education, certifications, projects, experience)
- **Job** — a listing (from Muse or entered manually), deduplicated by external ID; shared/cached across users, never deleted on account removal
- **JobApplication** — a user's tracking record for a job (status, dates) — this is what gets deleted when a user removes an application or their account
- **Analysis** — AI fit analysis for a CV/Job pair (match score, missing skills, strengths, suggestions)
- **ChatMessage** — chat history with AI context
- **PasswordResetToken** — short-lived tokens for the forgot-password flow

---

Built by Collins (Ntsobokwane Collins Shibambo) — final-year Diploma in ICT Application Development student at CPUT.
