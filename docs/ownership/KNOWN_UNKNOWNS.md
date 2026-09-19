# Known Unknowns

## Purpose
Maintain an explicit, prioritized inventory of uncertainties, untested assumptions, and gaps in technical understanding.

## Scope
Architecture, runtime behavior, data integrity, external integrations, and edge-case handling across `money-audit`.

## Current Status
- **Total Unknowns Tracked**: 6
- **Active / Open**: 6
- **Resolved**: 0

---

## Active Unknowns Register

| ID | Unknown Description | Why It Matters | Planned Investigation Level | Status |
| :--- | :--- | :--- | :--- | :--- |
| **KU-001** | OCR parsing heuristics and noise resilience | Tess4J extraction is noisy; parsing failure breaks automated receipt entry. | Level 5 (Feature Flow) / Level 9 (AI/ML) | 🔍 Open |
| **KU-002** | Debt settlement algorithm (Pairwise vs Graph Simplification) | Determines whether group debts are resolved directly or simplified to minimize transactions. | Level 5 (Feature Flow) / Level 7 (Backend) | 🔍 Open |
| **KU-003** | Razorpay webhook signature validation and payment lifecycle | Financial integrity against forged payment callbacks or replay attacks. | Level 5 (Feature Flow) / Level 11 (Security) | 🔍 Open |
| **KU-004** | Concurrency control during simultaneous expense logging or split updates | Potential race conditions or dirty reads on group balance calculations. | Level 7 (Backend) / Level 8 (Database) | 🔍 Open |
| **KU-005** | Fallback behavior when external services (Cloudinary, SMTP, Razorpay) are unreachable | Determines whether user operations fail gracefully or cause HTTP 500 crashes. | Level 10 (Infrastructure) / Level 13 (Debugging) | 🔍 Open |
| **KU-006** | Extent of test coverage across core financial and auth paths | Untested business logic poses regression risks during refactoring. | Level 12 (Testing) | 🔍 Open |
