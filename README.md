# Money Audit (Finance Tracker Backend)

## Project Identity
**Money Audit** is a Spring Boot 3.5 / Java 21 financial platform combining personal finance management, Splitwise-style collaborative group/peer-to-peer expense splitting with Razorpay settlements, and an automated Financial Intelligence & AI Assistant engine.

---

## Engineering Ownership Progress

This repository is actively undergoing the **21-Level Project Ownership Protocol** to achieve genuine engineering mastery, operational independence, and production readiness.

- **Current Status**: Initialized (Pre-Level 0)
- **Scorecard**: [Ownership Scorecard](file:///home/pranav/Documents/money-audit/docs/ownership/OWNERSHIP_SCORECARD.md)
- **Knowledge Base Root**: [`docs/ownership/`](file:///home/pranav/Documents/money-audit/docs/ownership/)

---

## Ownership Documentation Navigation

| Document | Purpose |
| :--- | :--- |
| [00_PROJECT_OVERVIEW.md](file:///home/pranav/Documents/money-audit/docs/ownership/00_PROJECT_OVERVIEW.md) | High-level project identity, capabilities, and technology stack |
| [03_PROJECT_KNOWLEDGE_GRAPH.md](file:///home/pranav/Documents/money-audit/docs/ownership/03_PROJECT_KNOWLEDGE_GRAPH.md) | Relational topology and component interaction graph |
| [08_DATABASE.md](file:///home/pranav/Documents/money-audit/docs/ownership/08_DATABASE.md) | Database schema, JPA entities, and persistence strategy |
| [09_AI_ML.md](file:///home/pranav/Documents/money-audit/docs/ownership/09_AI_ML.md) | OCR pipeline, rule engines, and conversational AI assistant |
| [10_INFRASTRUCTURE.md](file:///home/pranav/Documents/money-audit/docs/ownership/10_INFRASTRUCTURE.md) | Docker, PostgreSQL, external APIs, and deployment topology |
| [11_SECURITY.md](file:///home/pranav/Documents/money-audit/docs/ownership/11_SECURITY.md) | Authentication, authorization boundaries, and vulnerability backlog |
| [DECISION_LOG.md](file:///home/pranav/Documents/money-audit/docs/ownership/DECISION_LOG.md) | Historical vs. Ownership architectural decision register |
| [PROJECT_JOURNAL.md](file:///home/pranav/Documents/money-audit/docs/ownership/PROJECT_JOURNAL.md) | Engineering journal, learning iterations, and mental model shifts |
| [KNOWN_UNKNOWNS.md](file:///home/pranav/Documents/money-audit/docs/ownership/KNOWN_UNKNOWNS.md) | Living inventory of technical uncertainties and investigation plans |
| [EXPERIMENT_LOG.md](file:///home/pranav/Documents/money-audit/docs/ownership/EXPERIMENT_LOG.md) | Proofs-of-concept, failure injections, and verification experiments |
| [OWNERSHIP_SCORECARD.md](file:///home/pranav/Documents/money-audit/docs/ownership/OWNERSHIP_SCORECARD.md) | Master progress tracking across all 21 ownership levels |

---

## Local Development Quickstart

1. **Database**: Start PostgreSQL container:
   ```bash
   docker compose up -d
   ```
2. **Compile**:
   ```bash
   ./mvnw test-compile -DskipTests
   ```
3. **Run Application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **API Documentation**:
   Access Swagger UI at `http://localhost:8080/swagger-ui.html` once running.
