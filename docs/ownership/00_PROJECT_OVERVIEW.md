# 00 — Project Overview

## Purpose
Provide a high-level, evidence-based orientation to the `money-audit` repository: what it is, its core capabilities, and verified technology stack.

## Scope
Repository `/home/pranav/Documents/money-audit` (Backend service).

## Current Status
- **Initial High-Level Inspection Completed**: 2026-09-19
- **Deep Investigation**: Pending Level 0 / Level 1

---

## 1. Verified Identity [OBSERVED]
- **Project Name**: `finance-tracker` (Artifact ID) / `money-audit` (Repository & Application Name)
- **Runtime**: Java 21 (LTS)
- **Framework**: Spring Boot 3.5.10
- **Build System**: Maven (Maven Wrapper `mvnw` present)
- **Primary Persistence**: PostgreSQL 15 (via Docker container `finance_postgres`)
- **API Style**: RESTful JSON APIs documented via Springdoc OpenAPI (Swagger UI)

## 2. Core Functional Pillars [OBSERVED from packages]
1. **Personal Finance Management**: Individual expense tracking, category limits, budget enforcement, and savings goals.
2. **Social Expense Splitting**: Group management, multi-party expense splitting, peer-to-peer friend relationships, and balance tracking.
3. **Payment Settlements**: Razorpay integration for settling split balances.
4. **Receipt OCR**: Image upload (local disk / Cloudinary) with Tesseract OCR parsing.
5. **Financial Intelligence**: Rule-based risk detection, health score evaluation, financial forecasting, and tool-orchestrated AI assistant.

## 3. Technology Stack Summary

| Layer | Component / Tool | Configured Location | Status |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Spring Boot 3.5.10, Java 21 | `pom.xml` | Verified (Compiles) |
| **Database** | PostgreSQL 15, Spring Data JPA | `docker-compose.yml`, `application.yml` | Verified |
| **Security** | Spring Security, JJWT (0.11.5) | `com/Pranav/finance_tracker/auth` | Verified |
| **Document OCR** | Tess4J (5.11.0), Tesseract | `com/Pranav/finance_tracker/receipt` | Configured |
| **Payments** | Razorpay Java SDK (1.4.8) | `com/Pranav/finance_tracker/payment` | Configured |
| **Media Storage** | Cloudinary / Local Disk (`uploads/`) | `application.yml` | Configured |
| **Mail** | Spring Mail (JavaMailSender) | `application.yml` | Configured |

## 4. Open Questions / Placeholders
- [ ] *Detailed problem statement and user personas (To be completed in Level 1).*
- [ ] *End-to-end user journey mapping (To be completed in Level 1).*
