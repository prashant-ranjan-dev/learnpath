# 🎓 LearnPath – AI‑Assisted Learning Platform

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Google AI](https://img.shields.io/badge/AI-Gemini-4285F4?logo=google&logoColor=white)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **An intelligent learning platform that generates personalised syllabi and in‑depth topic explanations using Google Gemini AI – built with Spring Boot, React, and PostgreSQL.**

---

## 🧠 Overview

**LearnPath** allows learners to input any topic, their current understanding, desired depth, and goal. The backend, powered by **Spring AI** and **Google Gemini**, creates a structured syllabus with modules and topics. When a user clicks a topic, the AI generates a detailed, markdown‑formatted explanation, which is then cached for instant retrieval.

This project was developed as a **full‑stack learning exercise** – mastering Spring Boot, JPA, REST API design, React hooks, and AI integration – while producing a genuinely useful tool.

---

## ✨ Features

- 🤖 **AI‑Driven Syllabus Generation** – Google Gemini builds a multi‑module curriculum tailored to the user’s profile.
- 📚 **On‑Demand Content** – Click any topic to get an in‑depth, formatted explanation.
- ⚡ **Smart Caching** – Generated content is stored in PostgreSQL; subsequent requests are served instantly.
- 📱 **Intuitive UI** – React frontend with accordion modules, radio buttons, and Markdown rendering.
- 🔒 **Input Validation** – Radio buttons & dropdowns eliminate invalid data; backend enforces constraints.
- 📦 **Clean Architecture** – Service layer, DTOs, global exception handling, and repository pattern.
- 🔐 **Externalised Configuration** – API keys and DB credentials via environment variables.

---

## 🏗️ Tech Stack

| Layer          | Technology                          |
|----------------|-------------------------------------|
| **Backend**    | Java 21, Spring Boot 3.4.1          |
| **AI**         | Spring AI, Google Gemini (Flash)    |
| **Database**   | PostgreSQL 18, Spring Data JPA      |
| **Frontend**    | React 18, Vite, React‑Markdown      |
| **Build**      | Maven Wrapper (mvnd)                |
| **Tooling**    | Lombok, Postman, Git                |

---

## 📐 Architecture

```
┌───────────────────┐      REST API      ┌────────────────────────┐
│   React (Vite)    │ ◄────────────────► │   Spring Boot 3.4.1    │
│                   │  /api/syllabus     │                        │
│  • SyllabusForm   │  /api/topics/{id}  │  • SyllabusService     │
│  • SyllabusDisplay│                    │  • ContentService      │
│  • TopicContent   │                    │  • Spring AI ChatClient│
└───────────────────┘                    └───────────┬────────────┘
                                                     │
                                                     ▼
                                          ┌─────────────────────┐
                                          │   Google Gemini AI  │
                                          └─────────────────────┘
                                                     │
                                                     ▼
                                          ┌─────────────────────┐
                                          │    PostgreSQL 18    │
                                          │   (Syllabus,        │
                                          │    Module, Topic)   │
                                          └─────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven** (or use the included Maven wrapper `mvnd`)
- **PostgreSQL 18** (or any recent version)
- **Node.js 18+** and npm
- **Google Gemini API Key** ([get one free](https://aistudio.google.com/app/apikey))

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/learnpath.git
cd learnpath
```

### 2. Backend Setup

1. Create a PostgreSQL database named `LearnPath` (or adjust the URL in `application.yml`).
2. Set the required environment variables:

   ```powershell
   # Windows PowerShell
   $env:GOOGLE_API_KEY = "AIza..."
   $env:DB_PASSWORD = "your_postgres_password"
   ```

   *On Linux/macOS: `export GOOGLE_API_KEY=...`*

3. (Optional) Adjust `src/main/resources/application.yml` – the default configuration uses `${GOOGLE_API_KEY}` and `${DB_PASSWORD}`.
4. Run the backend:

   ```bash
   cd backend   # if you have a separate folder, or just run from root
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8080`.

### 3. Frontend Setup

1. Navigate to the frontend directory:

   ```bash
   cd frontend   # or the directory containing package.json
   ```

2. Install dependencies and start the dev server:

   ```bash
   npm install
   npm run dev
   ```

   The React app will open at `http://localhost:5173` (and proxy API requests to port 8080).

---

## 🧪 API Endpoints

| Method | Endpoint                       | Description                                   |
|--------|--------------------------------|-----------------------------------------------|
| `POST` | `/api/syllabus`                | Generate a syllabus from user input           |
| `GET`  | `/api/topics/{topicId}/content`| Get or generate content for a specific topic  |

### Example Request (`POST /api/syllabus`)

```json
{
  "topic": "Machine Learning",
  "currentUnderstanding": "BEGINNER",
  "depthLevel": "LEVEL2",
  "goal": "To build a simple predictive model"
}
```

### Example Response (`POST /api/syllabus`)

```json
{
  "syllabusId": 1,
  "modules": [
    {
      "id": 10,
      "title": "Foundations of Machine Learning",
      "estimatedHours": 4.5,
      "topics": [
        { "id": 25, "name": "What is ML?" },
        { "id": 26, "name": "Supervised vs Unsupervised Learning" }
      ]
    }
  ]
}
```

### Example Request (`GET /api/topics/25/content`)

```json
{
  "topicId": 25,
  "topicName": "What is ML?",
  "content": "## What is Machine Learning?\nMachine learning is a subset of artificial intelligence..."
}
```

---

## 📁 Project Structure (Backend)

```
src/main/java/com/learnpath/version1/
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects (records)
├── entity/              # JPA entities (Syllabus, SyllabusModule, Topic)
├── exception/           # Custom exceptions & global handler
├── repository/          # Spring Data JPA repositories
└── service/             # Business logic & AI integration
```

---

## 🔮 Future Enhancements

- [ ] User authentication & history of past syllabi
- [ ] Quiz generation for each module
- [ ] Streaming AI responses (real‑time content display)
- [ ] Dark mode & improved UI/UX
- [ ] Export syllabus as PDF
- [ ] Docker compose for one‑click setup

---

## 🎯 Learning Outcomes (Why This Project Stands Out)

- **Full‑stack integration** – React <-> Spring Boot REST API
- **AI/LLM integration** – Practical use of Google Gemini with Spring AI, including prompt engineering, JSON parsing, and caching
- **Database design** – JPA entity relationships (OneToMany, ManyToOne), cascading, and schema generation
- **Production‑like practices** – environment variables, DTOs vs entities, global exception handling
- **Clean code** – builder pattern, Java records, Lombok, and feature‑based packaging

---

## 📄 License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Prashant Ranjan**  
[GitHub](https://github.com/prashant-ranjan-dev) • [LinkedIn](https://www.linkedin.com/in/prashant-ranjan-2b80a622a/)

---

*Built with ❤️ as a learning journey into full‑stack AI development. Contributions, issues, and stars are welcome!*
