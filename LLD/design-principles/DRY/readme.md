# DRY — Don't Repeat Yourself

> "Every piece of knowledge must have a single, unambiguous, authoritative representation within a system." — Andy Hunt & Dave Thomas, *The Pragmatic Programmer* (1999)

---

## Table of Contents

- [What DRY Really Means](#what-dry-really-means)
- [The Three Types of Duplication](#the-three-types-of-duplication)
- [Violation → Fix](#violation--fix)
- [When Duplication Is Acceptable](#when-duplication-is-acceptable)
- [DRY Beyond Code](#dry-beyond-code)
- [DRY vs Other Principles](#dry-vs-other-principles)
- [Interview Q&A](#interview-qa)

---

## What DRY Really Means

**Common misunderstanding:** "Don't write similar-looking code twice."

**Actual meaning:** Don't duplicate **knowledge** — the same business rule, algorithm, or truth should live in exactly one place. If a fact changes, you should only need to change it in one location.

DRY is about **knowledge duplication**, not **code duplication**. Two code blocks can look identical but represent different knowledge — merging them would create coupling that hurts when they diverge.

```
DRY violation:     Same KNOWLEDGE expressed in multiple places
NOT a DRY violation: Same CODE that represents DIFFERENT knowledge
```

**Example of the distinction:**

```java
// These look identical but represent DIFFERENT business rules
// Merging them would be WRONG — they'll diverge independently

// Tax validation: addresses need country for tax calculation
boolean isValidForTax(Address addr) {
    return addr.getStreet() != null && addr.getCountry() != null;
}

// Shipping validation: addresses need country for routing
boolean isValidForShipping(Address addr) {
    return addr.getStreet() != null && addr.getCountry() != null;
}

// Today they're identical. Tomorrow:
// - Tax adds: && addr.getVatId() != null
// - Shipping adds: && addr.getPostalCode() != null
// If you merged them, BOTH break when either changes.
```

---

## The Three Types of Duplication

| Type | What It Is | Danger Level | Example |
|---|---|---|---|
| **Knowledge duplication** | Same business rule in multiple places | 🔴 High — fix in one, forget the other | Tax rate hardcoded in `OrderService` AND `InvoiceService` |
| **Code duplication** | Same lines of code, same knowledge | 🟡 Medium — extract when stable | Same validation logic copy-pasted in 5 controllers |
| **Coincidental duplication** | Same code, DIFFERENT knowledge | 🟢 Low — leave it alone | Tax validation and shipping validation look identical today |

**The critical question before extracting:** "If I change this code in one place, MUST the other place change too?" If yes → DRY violation, extract. If no → coincidental, leave duplicated.

---

## Violation → Fix

### Violation 1: Hardcoded Business Rules in Multiple Places

**❌ Violation:**
```java
class OrderService {
    public double calculateTotal(Order order) {
        double subtotal = order.getItems().stream()
            .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        return subtotal + subtotal * 0.18;  // Tax rate hardcoded HERE
    }
}

class InvoiceService {
    public String generateInvoice(Order order) {
        double tax = order.getSubtotal() * 0.18;  // SAME tax rate hardcoded HERE
        return "Subtotal: " + order.getSubtotal() + "\nTax: " + tax;
    }
}

class ReportService {
    public double getMonthlyTaxCollected(List<Order> orders) {
        return orders.stream()
            .mapToDouble(o -> o.getSubtotal() * 0.18)  // AND HERE
            .sum();
    }
}
// Tax rate changes to 0.21 → you must find and update ALL THREE places.
// Miss one? Silent bug. Customers charged wrong tax.
```

**✅ Fix:**
```java
// Single source of truth for tax rate
class TaxConfig {
    private static final double DEFAULT_RATE = 0.18;

    public double getRate(String region) {
        return taxRateRepository.findByRegion(region)
            .orElse(DEFAULT_RATE);
    }
}

class OrderService {
    private final TaxConfig taxConfig;

    public double calculateTotal(Order order) {
        double subtotal = order.getSubtotal();
        return subtotal + subtotal * taxConfig.getRate(order.getRegion());
    }
}

class InvoiceService {
    private final TaxConfig taxConfig;

    public String generateInvoice(Order order) {
        double tax = order.getSubtotal() * taxConfig.getRate(order.getRegion());
        return "Subtotal: " + order.getSubtotal() + "\nTax: " + tax;
    }
}
// Tax rate changes? Update ONE place (TaxConfig or database). Done.
```

### Violation 2: Repeated Validation Logic

**❌ Violation:**
```java
class UserController {
    public void register(UserDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.]+@[\\w.]+\\.[a-z]{2,}$")) {
            throw new ValidationException("Invalid email");
        }
    }
}

class ProfileController {
    public void updateEmail(String newEmail) {
        if (newEmail == null || !newEmail.matches("^[\\w.]+@[\\w.]+\\.[a-z]{2,}$")) {
            throw new ValidationException("Invalid email");
        }
    }
}

class AdminController {
    public void createUser(AdminUserDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().matches("^[\\w.]+@[\\w.]+\\.[a-z]{2,}$")) {
            throw new ValidationException("Invalid email");
        }
    }
}
// Regex has a bug? Fix in 3 places. Miss one? Invalid emails slip through.
```

**✅ Fix:**
```java
class EmailValidator {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.]+@[\\w.]+\\.[a-z]{2,}$");

    public static void validate(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email: " + email);
        }
    }
}

// All controllers use the same validator — one source of truth
class UserController {
    public void register(UserDTO dto) {
        EmailValidator.validate(dto.getEmail());
    }
}
```

### Violation 3: Repeated Structural Patterns (Boilerplate)

**❌ Violation:**
```java
// Every repository method has the same try-catch-log-close pattern
public User findUserById(String id) {
    Connection conn = null;
    try {
        conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        stmt.setString(1, id);
        ResultSet rs = stmt.executeQuery();
        return mapUser(rs);
    } catch (SQLException e) {
        logger.error("Failed to find user: " + id, e);
        throw new DataAccessException(e);
    } finally {
        if (conn != null) conn.close();
    }
}

public Order findOrderById(String id) {
    // 90% identical structure, different SQL and mapper
    Connection conn = null;
    try {
        conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders WHERE id = ?");
        stmt.setString(1, id);
        ResultSet rs = stmt.executeQuery();
        return mapOrder(rs);
    } catch (SQLException e) {
        logger.error("Failed to find order: " + id, e);
        throw new DataAccessException(e);
    } finally {
        if (conn != null) conn.close();
    }
}
```

**✅ Fix — Template Method / Higher-Order Function:**
```java
class JdbcTemplate {
    private final DataSource dataSource;

    public <T> T query(String sql, Object[] params, RowMapper<T> mapper) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            return mapper.map(rs);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}

// Now every query is one line
User user = jdbcTemplate.query("SELECT * FROM users WHERE id = ?", new Object[]{id}, this::mapUser);
// Spring's JdbcTemplate exists precisely because of this DRY violation.
```

---

## When Duplication Is Acceptable

DRY is NOT absolute. There are times when duplication is the right choice:

### 1. Coincidental Duplication (Different Knowledge)

```java
// These LOOK the same but represent different business rules — DO NOT merge
class OrderValidator {
    boolean isValid(Order o) { return o.getTotal() > 0 && o.getItems().size() > 0; }
}

class CartValidator {
    boolean isValid(Cart c) { return c.getTotal() > 0 && c.getItems().size() > 0; }
}
// Tomorrow: orders require minimum $10, carts don't. If merged, you'd have to un-merge.
```

### 2. Decoupling Is More Important

In microservices, each service owns its domain model. Two services may have identical `Address` classes — merging them into a shared library creates deployment coupling.

**Rule:** Prefer duplication over shared libraries between services, unless the shared code is truly stable infrastructure (logging, HTTP client).

### 3. Premature Abstraction Is Worse Than Duplication

> "Duplication is far cheaper than the wrong abstraction." — Sandi Metz

**Rule of Three:** See it once → write it. See it twice → note it. See it thrice → extract it.

---

## DRY Beyond Code

DRY applies to ALL representations of knowledge in a system:

| Domain | DRY Violation | Fix |
|---|---|---|
| **Database schema** | Column constraint duplicated in app code AND DDL | Use ORM annotations as single source, generate DDL |
| **API documentation** | Manually written docs that drift from actual API | OpenAPI spec generates both code AND docs |
| **Configuration** | Same timeout value in 3 config files | Centralized config service (Spring Cloud Config, Consul) |
| **Build scripts** | Same dependency version in 5 modules | BOM / version catalog (Gradle version catalogs, Maven BOM) |
| **Tests** | Same test setup in every test class | `@BeforeAll` in base test class, test fixtures, Object Mother |
| **Constants** | Magic number `86400` (seconds in a day) in 10 files | `Duration.ofDays(1).toSeconds()` or named constant |

---

## DRY vs Other Principles

### DRY vs SRP

**Tension:** Two classes duplicate the same code, but they change for DIFFERENT REASONS (different stakeholders).

**Resolution:** SRP wins. Keep the duplication. Merging would create a class with multiple reasons to change.

### DRY vs Decoupling

**Tension:** Extracting shared code between modules creates a dependency (coupling).

**Resolution:** Evaluate the stability of the shared knowledge. Stable utilities → extract. Volatile business rules → keep duplicated per module.

### DRY vs Readability

**Tension:** Extracting a 3-line snippet into a helper with a 10-line name makes code harder to read.

**Resolution:** If the extraction obscures the flow and the duplication is small and local, duplication is more readable. DRY is about knowledge, not about saving keystrokes.

---

## Interview Q&A

**Q: "DRY says don't repeat yourself. But I see duplication across microservices that's intentional. Explain."**
A: DRY applies within a bounded context, not across service boundaries. Each microservice owns its domain model. Sharing models via libraries creates deployment coupling — changing one service forces redeployment of others. The duplication is intentional: each service can evolve independently. This is the DDD concept of "shared kernel" vs "separate ways."

**Q: "How do you identify DRY violations in a large codebase?"**
A: (1) **Static analysis**: SonarQube's "duplicated blocks" detection, IntelliJ's "locate duplicates." (2) **Code review heuristic**: if a bug fix requires changing the same logic in multiple files, it's a DRY violation. (3) **Shotgun surgery smell**: a single change requires many small edits across many classes. (4) **Search for magic numbers**: `grep -r "0.18"` — if the same constant appears in multiple files, it's knowledge duplication.

**Q: "Is copy-pasting code always a DRY violation?"**
A: No. It's a DRY violation only if both copies represent the SAME knowledge. If they represent different rules that happen to look identical today, keeping them separate is correct. The test: "If I change one, MUST the other change?" Yes → violation. No → coincidental.

**Q: "How does DRY relate to the Template Method pattern?"**
A: Template Method is DRY applied to algorithm structure. When multiple subclasses share the same algorithm SEQUENCE but differ in specific steps, the shared sequence is knowledge that shouldn't be duplicated. Template Method extracts the shared structure into a base class. Same idea: Spring's `JdbcTemplate`, `RestTemplate`.

**Q: "Your team extracted a shared utility used by 12 services. Now every change requires coordinating 12 deployments. What went wrong?"**
A: They applied DRY across a boundary where decoupling was more important. Fix: (1) Version the shared library — services opt-in to upgrades. (2) Evaluate whether the "shared" code is actually shared knowledge or coincidental similarity. (3) If it contains business logic, inline it into each service.

**Q: "DRY in database design — normalized vs denormalized?"**
A: Normalization IS DRY for databases — each fact stored once, no update anomalies. Denormalization deliberately violates DRY for read performance. Acceptable when: read frequency >> write frequency, eventual consistency is OK, and you have sync mechanisms. CQRS is DRY-violating by design — separate read/write models with duplicate data.
