# Decision Log

## Purpose
Maintain an authoritative record of architectural, design, and engineering decisions. Distinguishes between historical decisions (discovered in repository evidence) and ownership decisions (made during this ownership process).

## Scope
Cross-cutting architectural patterns, libraries, infrastructure choices, and data model decisions.

## Current Status
- **Historical Decisions Documented**: 6
- **Ownership Decisions Documented**: 1

---

## Historical Decisions [OBSERVED]

| Decision ID | Area | Decision | Historical Context / Reason | Status / Assessment |
| :--- | :--- | :--- | :--- | :--- |
| **HD-001** | Architecture | Feature-based modular packaging (`com.Pranav.finance_tracker.<feature>`) | Reorganized early in repo history to keep domain controllers, services, repositories, and entities co-located. | Clean domain boundaries. |
| **HD-002** | Persistence | Hibernate `ddl-auto: update` without Flyway/Liquibase migrations | Fast local iteration during initial development. | Risky for production schema evolution. |
| **HD-003** | Authentication | Stateless JWT with JJWT (0.11.5), HS256 algorithm | Standard REST API security pattern; 24-hour token expiry. | In-code fallback secret needs hardening. |
| **HD-004** | Payments | Razorpay Java SDK for settlement payments | Integration for Indian payment ecosystem (UPI, cards). | Keys currently in `application.yml`. |
| **HD-005** | Media Storage | Dual storage: Local `uploads/` + Cloudinary | Initial local disk storage complemented by Cloudinary CDN integration. | Fallback paths present. |
| **HD-006** | Financial Intelligence | Rule-based engines for risks/scores + template-based AI assistant | Deterministic, offline calculation preventing AI number hallucination. | Modular and swappable via `ai.provider`. |

---

## Ownership Decisions

| Decision ID | Area | Decision | Rationale | Date |
| :--- | :--- | :--- | :--- | :--- |
| **OD-001** | Documentation | Established structured `/docs/ownership/` knowledge base | Maintain evidence-based documentation, scorecard, journal, decision log, and knowledge graph. | 2026-09-19 |
