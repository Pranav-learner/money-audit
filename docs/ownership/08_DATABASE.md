# 08 — Database & Persistence

## Purpose
Document the PostgreSQL schema, JPA entity mappings, relationships, indexes, transaction boundaries, and migration strategy.

## Scope
`src/main/java/com/Pranav/finance_tracker/**/entity/`, repositories, and database configuration.

## Current Status
- **Status**: Initialized (Pre-Level 0)
- **Deep Inspection**: Scheduled for Level 8

---

## 1. Persistence Stack [OBSERVED]
- **Engine**: PostgreSQL 15 (Docker container `finance_postgres`)
- **ORM**: Hibernate 6 / Spring Data JPA
- **DDL Strategy**: `hibernate.ddl-auto: update`
- **Connection Pool**: HikariCP (Spring Boot default)

---

## 2. Identified Primary Entities [OBSERVED from packages]
- `User` (`user/entity`)
- `Expense`, `ExpenseActivity` (`expense/entity`)
- `Category` (`category/entity`)
- `Budget` (`budget/entity`)
- `Saving` (`savings/entity`)
- `Group`, `GroupMember`, `GroupExpense`, `GroupExpenseSplit`, `GroupInvitation` (`group/entity`)
- `Friendship` (`friend/entity`)
- `Payment` (`payment/entity`)
- `Receipt` (`receipt/entity`)

---

## 3. Placeholders for Level 8
- [ ] *Full Entity-Relationship (ER) diagram with cardinality.*
- [ ] *Index audit on foreign keys and search columns.*
- [ ] *Transaction boundary audit (`@Transactional` propagation and isolation).*
- [ ] *Flyway migration plan to replace `ddl-auto: update`.*
