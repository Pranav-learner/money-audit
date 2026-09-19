# 10 — Infrastructure & External Services

## Purpose
Document containerization, host system dependencies, external SaaS APIs, networking, configuration management, and deployment topology.

## Scope
`docker-compose.yml`, `application.yml`, third-party client configurations, and file storage.

## Current Status
- **Status**: Initialized (Pre-Level 0)
- **Deep Inspection**: Scheduled for Level 10

---

## 1. Infrastructure Topology [OBSERVED]

| Component | Technology | Configuration Location | Role |
| :--- | :--- | :--- | :--- |
| **Local Database** | PostgreSQL 15 | `docker-compose.yml` | Primary relational datastore on port 5432 |
| **OCR Native Engine** | Tesseract C++ / Tessdata | System path `/usr/share/tessdata` | Native character recognition binaries |
| **Media Storage** | Cloudinary / Local Disk | `cloudinary.url` / `uploads/receipts` | Receipt and avatar image hosting |
| **Payment Gateway** | Razorpay REST API | `razorpay.key-id`, `razorpay.key-secret` | Settlement orders and transaction verification |
| **Email Gateway** | Gmail SMTP | `spring.mail.*` | Scheduled reports and transactional notifications |

---

## 2. Placeholders for Level 10
- [ ] *Docker Compose full service stack evaluation.*
- [ ] *Environment variable injection strategy for production secrets.*
- [ ] *Failure mode analysis when external SaaS endpoints are unavailable.*
- [ ] *Production deployment topology (CI/CD, container registry, reverse proxy).*
