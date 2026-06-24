# Job Application Assistant 

AI-powered job search tool. Upload your CV, get job matches ranked by fit, see skill gaps.

**Status:** Active development. Core features working locally. Not yet deployed.

---

## What It Does

1. **Sign up** → Create account with email/password
2. **Upload CV** → PDF or DOCX file
3. **AI reads your skills** → Extracts text, identifies languages/frameworks/tools
4. **Get job matches** → Auto-discovers jobs via The Muse API, ranked by how well they fit your CV
5. **See skill gaps** → For any job, AI shows: match %, missing skills, what to learn
6. **Track applications** → Dashboard to save and track jobs (status: saved, applied, interview, rejected, accepted)
7. **Ask AI** → Chat about your job search. "Which job am I most qualified for?" etc.
<img width="564" height="1164" alt="image" src="https://github.com/user-attachments/assets/c117e857-a7e9-48f3-9b3b-cbef488f8038" />
<img width="1504" height="825" alt="image" src="https://github.com/user-attachments/assets/4a7cdfc5-8d64-47cd-be35-aa8e4e60c965" />

---

## Tech Stack

**Backend:** Spring Boot 3, Spring Security (JWT), JPA/Hibernate  
**Frontend:** React, TypeScript, Tailwind CSS (separate repo)  
**Database:** MySQL (local), Azure SQL (planned for production)  
**Storage:** Azure Blob Storage (for CV files)  
**AI:** Azure OpenAI (GPT-4 for skill extraction & chat)  
**Job data:** The Muse API (free, no auth)  
**PDF parsing:** Apache PDFBox

---

## Running Locally

### Prerequisites
- JDK 17+
- MySQL running locally
- Azure OpenAI API key 

### Setup

```bash
# Clone repo
git clone https://github.com/shibambocollins/job-application-assistant-api.git
cd job-application-assistant-api

# Create MySQL database
mysql -u root -p
> CREATE DATABASE jobassistant_dev;
> EXIT;

# Create application.yml
cat > src/main/resources/application.yml << 'EOF'
spring:
  application:
    name: job-assistant-api
  datasource:
    url: jdbc:mysql://localhost:3306/jobassistant_dev
    username: root
    password: your_mysql_password_here
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  security:
    jwt:
      secret: your-secret-key-change-this
      expiration: 86400000

azure:
  openai:
    api-key: your-azure-openai-api-key
    endpoint: https://your-instance.openai.azure.com/

muse:
  api:
    base-url: https://www.themuse.com/api/public
EOF

# Run
mvn spring-boot:run
```

Server runs on `http://localhost:8080`

---

## API Endpoints

```
POST   /api/auth/register         # Sign up
POST   /api/auth/login            # Login (returns JWT token)
POST   /api/cvs/upload            # Upload CV file
GET    /api/cvs/latest            # Get your CV data
POST   /api/jobs/discover         # Find jobs using your skills
GET    /api/jobs                  # List your saved jobs
POST   /api/jobs                  # Save a job
PUT    /api/jobs/{id}             # Update job status
POST   /api/analysis/{jobId}      # Analyze job fit
GET    /api/chat                  # Chat with AI
```

All requests need `Authorization: Bearer <token>` header (except register/login).

---

## What's Working

- User registration & login (JWT)  
- CV upload & text extraction (PDFBox)  
- Skill extraction from CV (GPT-4)  
- Job discovery (Muse API + skill ranking)  
- CV vs Job analysis (match %, missing skills)  
- Application tracker (save, status updates)  
- Basic chat (working, but needs optimization)

---

## What's In Progress

🚧 Chat improvements (context window, token cost)  
🚧 Frontend (React)  
🚧 Error handling (some edge cases still crashing)

---

## What's Planned

📋 Deploy to Azure  
📋 Email notifications  
📋 Cover letter generator  

---

## Database

4 tables:
- `users` — Account info
- `jobs` — Saved jobs
- `cvs` — Uploaded CV files
- `job_analysis` — Skill analysis results

---

Current coverage: ~80% of service layer

---

## Next Steps (For Me)

1. Fix chat token issues (long conversations losing context)
2. Polish mobile UI
3. Deploy backend to Azure App Service
4. Deploy frontend to Vercel
5. Add email notifications
6. Add cover letter generator

---
