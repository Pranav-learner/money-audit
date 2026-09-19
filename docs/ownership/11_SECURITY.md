# 11 — Security Architecture & Boundaries

## Purpose
Document authentication mechanisms, authorization boundaries, token lifecycles, cryptographic keys, input validation, and data privacy safeguards.

## Scope
`com/Pranav/finance_tracker/auth/`, Spring Security filters, CORS configs, and resource ownership checks.

## Current Status
- **Status**: Initialized (Pre-Level 0)
- **Deep Inspection**: Scheduled for Level 11

---

## 1. Security Architecture Summary [OBSERVED]
- **Authentication**: Stateless Bearer JWT tokens passed in HTTP `Authorization` header.
- **Token Signing**: HMAC-SHA256 (`Keys.hmacShaKeyFor`) via JJWT.
- **Password Hashing**: BCryptPasswordEncoder (Spring Security standard).
- **Public vs Protected Endpoints**: Public access for `/api/auth/**`, Swagger UI; authenticated access for finance domains.

---

## 2. Identified Vulnerabilities & Hardening Targets [OBSERVED / RECOMMENDED]
- Default fallback secret in `@Value("${jwt.secret:...}")` in `JwtService.java`.
- `@Configuration` annotation on `JwtService` instead of `@Service`.
- Secret key stored in plain text inside `application.yml`.
- Need to audit multi-tenant authorization (ensuring User A cannot query or modify User B's expenses, budgets, or groups).

---

## 3. Placeholders for Level 11
- [ ] *Security filter chain breakdown (`SecurityFilterChain`).*
- [ ] *Token refresh strategy audit (currently access-token only, 24h expiration).*
- [ ] *CORS configuration and allowed origins audit.*
- [ ] *Method-level security (`@PreAuthorize`) and multi-tenant resource access verification.*
