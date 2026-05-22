# Order Booking & Portfolio API

A production-grade stock trading desk backend built with Spring Boot 3.2 and Java 17+. Supports order lifecycle management, portfolio tracking with sector grouping, and sector overlap analysis against benchmark baskets.

## How to Run

```bash
# Run with dev profile (H2 in-memory database)
./gradlew bootRun --args="--spring.profiles.active=dev"

# Run tests
./gradlew test

# Build JAR
./gradlew build
```

The application starts on `http://localhost:8080`. The H2 console is available at `http://localhost:8080/h2-console` (dev profile only).

## API Endpoints

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | POST | `/api/orders` | Place a BUY or SELL order |
| 2 | POST | `/api/orders/{id}/fill` | Fill a pending order |
| 3 | POST | `/api/orders/{id}/cancel` | Cancel a pending order |
| 4 | GET | `/api/portfolio/{traderId}` | Get portfolio with sector breakdown |
| 5 | GET | `/api/portfolio/{traderId}/overlap` | Sector overlap analysis |
| 6 | POST | `/api/portfolio/{traderId}/add` | Add holdings directly |

### Example Requests

```bash
# Place an order
POST /api/orders
{"traderId": "T001", "stock": "AAPL", "sector": "TECH", "quantity": 50, "side": "BUY"}

# Fill an order
POST /api/orders/1/fill

# Cancel an order
POST /api/orders/1/cancel

# Get portfolio
GET /api/portfolio/T001

# Sector overlap analysis
GET /api/portfolio/T001/overlap

# Add to portfolio
POST /api/portfolio/T001/add
{"stock": "NVDA", "sector": "TECH", "quantity": 100}
```

## Architecture & Design Decisions

### Layered Architecture

```
Controller → Service → Repository → Database
                ↕
        OverlapCalculator (pure Java, no framework deps)
```

- **Controllers**: HTTP concerns only — validation, status codes, response mapping
- **Services**: Business logic, transaction boundaries, orchestration
- **Repositories**: Data access with pessimistic locking queries
- **OverlapCalculator**: Standalone pure Java class — no Spring, no DB, independently testable

### Why Pessimistic Locking (not Optimistic)?

| Factor | Pessimistic | Optimistic |
|--------|-------------|------------|
| Conflict rate | Handles high conflict well | Assumes low conflict |
| Retry logic | Not needed | Requires app-level retries |
| User experience | Deterministic — immediate success/failure | Non-deterministic |
| Financial correctness | Guaranteed — no silent failures | Requires careful retry design |

**Decision**: Pessimistic locking (`SELECT FOR UPDATE`) because:
1. Financial operations must not silently fail and retry — traders need immediate feedback
2. The conflict rate for the same order (fill vs cancel race) is inherently high
3. The pending count check is a classic "check-then-act" race that optimistic locking cannot solve without retry loops
4. Lock hold time is short (~5-20ms), so throughput impact is minimal

### Concurrency Strategy

| Scenario | Protection Mechanism |
|----------|---------------------|
| Pending order limit (max 3) | `SELECT FOR UPDATE` on trader's pending orders — serializes concurrent placements for same trader |
| Fill/Cancel race | `SELECT FOR UPDATE` on order row — only one thread can transition state |
| Portfolio updates during fill | Same transaction as order state change — atomic via `@Transactional` |
| Concurrent add-to-portfolio | `SELECT FOR UPDATE` on holding row — prevents lost updates |

### Transaction Boundaries

- **Order fill** = state change + portfolio update in a single `@Transactional` method. If either fails, both roll back.
- **PortfolioService.updateHoldingForFill** uses `Propagation.MANDATORY` — it MUST be called within an existing transaction (enforced at runtime).

### Database Schema Design

- `CHECK` constraints on `quantity > 0`, `side IN ('BUY','SELL')`, `state IN ('PENDING','FILLED','CANCELLED')` — defense-in-depth beyond application validation
- Composite index on `(trader_id, state)` — the pending count query runs on every order placement
- Unique constraint on `(trader_id, stock)` in holdings — enforces one row per trader-stock pair

### Error Handling

Consistent error responses via `@RestControllerAdvice`:
- 400: Validation errors, business rule violations (pending limit, insufficient holdings)
- 404: Order or trader not found
- 409: Invalid state transition, concurrent modification
- 500: Unexpected errors (generic message to client, full stack trace in logs)

### Correlation IDs

Every request gets a UUID correlation ID via a servlet filter. It's:
- Set in SLF4J MDC for structured logging
- Returned in `X-Correlation-Id` response header for client-side tracing

## Trade-offs & Limitations

### What This Design Handles Well
- Dozens of concurrent traders (trading desk scale)
- Race conditions on order state transitions
- Data integrity under concurrent portfolio modifications
- Clear, auditable state transitions with logging

### What Would Need to Change for Higher Scale
- **Rate limiting**: No per-trader throttling (add Bucket4j or Redis-based limiter)
- **Horizontal scaling**: Single instance; multi-instance would need distributed locks (Redisson) or database advisory locks
- **Async processing**: All operations are synchronous; for thousands of orders/sec, move to event queue (Kafka) with eventual consistency
- **Caching**: Portfolio reads hit DB every time; add Redis cache for read-heavy workloads
- **Authentication**: Not implemented; would add Spring Security + JWT for production

### Intentionally Skipped (with reasons)
- **Authentication/Authorization**: Not in assignment scope. Would use Spring Security + JWT with trader-scoped access.
- **Real-time updates (WebSocket)**: Not in assignment scope. Would use Spring WebSocket + STOMP for order status push notifications.
- **Audit trail table**: Would add an `order_events` table for full state transition history in production.
- **Idempotency keys**: Would add for order placement to handle network retries safely.

## Testing Strategy

| Layer | Framework | What's Tested |
|-------|-----------|---------------|
| Property-based | jqwik | Overlap calculator correctness (Dice formula, risk flags) |
| Unit | JUnit 5 + Mockito | Service logic, exception handling, edge cases |
| Integration | @SpringBootTest + H2 | Concurrency guarantees with real DB locking |

### Concurrency Tests Verify:
- Concurrent fills on same order → exactly 1 succeeds
- Concurrent fill + cancel → exactly 1 succeeds
- Concurrent order placement at limit → limit not exceeded
- Concurrent add-holding → no lost updates (final = sum)
- Independent traders → no interference

## Technology Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Framework | Spring Boot 3.2.5 | Industry standard, mature ecosystem |
| Language | Java 17 | Records, sealed classes, modern features |
| ORM | Spring Data JPA (Hibernate) | Declarative repos, built-in locking annotations |
| DB (dev) | H2 (PostgreSQL mode) | Fast local dev, compatible SQL dialect |
| DB (prod) | PostgreSQL 15+ | MVCC, robust locking, production-proven |
| Connection Pool | HikariCP | Sub-millisecond acquisition, Spring Boot default |
| Build | Gradle 8.5 | Fast incremental builds |
| Testing | JUnit 5 + jqwik + Mockito | Property-based + example-based + mocking |
