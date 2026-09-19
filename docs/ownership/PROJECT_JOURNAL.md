# Project Ownership Journal

## Purpose
Chronicle the engineering journey, recording investigations, previous beliefs, discoveries, mental model shifts, corrected misconceptions, and unresolved questions as ownership progresses.

## Scope
All notes, deductions, reflections, and learning iterations from Level 0 through Level 20.

## Current Status
- **Phase**: Pre-Level 0 Initialization
- **Date**: 2026-09-19

---

## Log Entries

### [2026-09-19] — Workspace Initialization & Pre-Level 0 Inspection

#### What Was Investigated:
- High-level directory inspection (`pom.xml`, `mvnw`, `docker-compose.yml`, `architecture.md`, `src/`).
- Build verification via `./mvnw test-compile -DskipTests` (Confirmed compiles cleanly on Java 21).
- Package discovery in `com/Pranav/finance_tracker/`.
- Git commit tree traversal from initial commit to HEAD.

#### Previous Belief / Default Assumption:
- A generic, monolithic personal finance CRUD app.

#### Verified Discoveries [OBSERVED]:
- The repo has 18 distinct packages reflecting an evolutionary architecture:
  1. Personal Finance (`auth`, `user`, `expense`, `category`, `budget`, `savings`)
  2. Social Finance (`friend`, `group`, `payment`)
  3. Processing & Integrations (`receipt`, `email`, `config`, `exception`)
  4. Financial Intelligence (`analytics`, `dashboard`, `financialintelligence`, `aiassistant`)
- AI is implemented as a deterministic agentic tool architecture (`TemplateLlmProvider` + 11 internal tool implementations), preventing hallucinated numbers.
- Database runs on PostgreSQL 15 with Hibernate `ddl-auto: update`.

#### Mental Model Shift:
- Moving from viewing the repo as an unstructured codebase to understanding it as four coordinated subsystems: Ledger, Social Splitting, Financial Intelligence, and External Ingestion.

#### Open Questions to Tackle:
- How does the OCR pipeline extract line items and numbers?
- How does group debt balance resolution handle cyclical debts?
- What are the security protections on multi-tenant group expense access?
