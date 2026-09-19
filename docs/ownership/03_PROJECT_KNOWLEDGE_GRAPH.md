# 03 — Project Knowledge Graph

## Purpose
A living relational model and topological map capturing the components, data flows, trust boundaries, and interdependencies within `money-audit`.

## Scope
Cross-layer system topology: Controller -> Service -> Repository -> Database / External APIs.

## Current Status
- **Baseline Topology**: Initialized from high-level package analysis.
- **Deep Relationship Traces**: Pending Level 2 & Level 3.

---

## 1. High-Level System Graph [OBSERVED]

```mermaid
graph TD
    Client[Web/Mobile Client]
    
    subgraph SpringBootBackend [Spring Boot Application :8080]
        SecurityFilter[JwtAuthFilter & SecurityFilterChain]
        
        subgraph CoreDomain [Core Financial Domains]
            Auth[auth]
            User[user]
            Expense[expense]
            Category[category]
            Budget[budget]
            Savings[savings]
        end
        
        subgraph SocialDomain [Social & Splitting Domains]
            Friend[friend]
            Group[group]
            Payment[payment]
        end
        
        subgraph IntelligenceDomain [Intelligence & Advisory]
            Analytics[analytics]
            FI[financialintelligence]
            AIAssistant[aiassistant]
        end
        
        subgraph IngestionMedia [Ingestion & Media]
            Receipt[receipt]
            Email[email]
        end
    end
    
    subgraph ExternalSystems [External Systems & Storage]
        Postgres[(PostgreSQL 15)]
        Tesseract[Tesseract OCR Engine]
        RazorpayAPI[Razorpay Payment Gateway]
        CloudinaryAPI[Cloudinary CDN]
        SMTP[Gmail SMTP Server]
    end

    Client -->|HTTP / JWT| SecurityFilter
    SecurityFilter --> Auth
    SecurityFilter --> CoreDomain
    SecurityFilter --> SocialDomain
    SecurityFilter --> IntelligenceDomain
    SecurityFilter --> IngestionMedia
    
    CoreDomain --> Postgres
    SocialDomain --> Postgres
    IntelligenceDomain --> Postgres
    
    SocialDomain --> RazorpayAPI
    IngestionMedia --> Tesseract
    IngestionMedia --> CloudinaryAPI
    IngestionMedia --> SMTP
```

---

## 2. Interaction Matrix [Placeholder]

| Source Component | Target Component | Protocol / Mechanism | Data Exchanged | Verified? |
| :--- | :--- | :--- | :--- | :--- |
| `aiassistant` | `FinancialToolRegistry` | In-memory Java Call | Tool request/response | ⏳ Pending Level 4 |
| `group` | `payment` | Spring Service Call | Settlement status | ⏳ Pending Level 5 |
| `receipt` | `ocr` | JNI / Local File | Image file / Raw text | ⏳ Pending Level 5 |

---

## 3. Trust Boundaries [Placeholder]
- [ ] *Boundary 1: External Client to Public Endpoints (`/api/auth/**`).*
- [ ] *Boundary 2: Authenticated Client to Protected Endpoints (JWT Claims verification).*
- [ ] *Boundary 3: Backend to External APIs (Razorpay webhooks, Cloudinary).*
