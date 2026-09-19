# Experiment Log

## Purpose
Document all deliberate technical experiments, proof-of-concepts, failure injections, benchmarking runs, and code modifications conducted during ownership.

## Scope
Controlled experiments and exploratory tests on `money-audit`.

## Current Status
- **Total Experiments**: 1 (Initial compilation check)
- **Active / Pending**: 0

---

## Experiment Register

### EXP-001: Baseline Maven Compilation Check
- **Date**: 2026-09-19
- **Hypothesis**: Project compiles successfully on Java 21 without active code modifications.
- **Method**: Executed `./mvnw test-compile -DskipTests`.
- **Observed Result**: Build SUCCESS in 2.9s. 114+ classes compiled into `target/classes`.
- **Conclusion**: Core code syntax and dependencies are aligned with JDK 21.

---

## Planned Experiments [Placeholders]
- [ ] *EXP-002: Test containerized PostgreSQL connection and Hibernate DDL generation.*
- [ ] *EXP-003: Simulate expired JWT token injection to verify 401 response and filter chain.*
- [ ] *EXP-004: Upload sample corrupted receipt image to test OCR fallback handling.*
- [ ] *EXP-005: Concurrency test on multi-party group split creation.*
