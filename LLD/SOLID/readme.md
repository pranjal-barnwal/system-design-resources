# SOLID Principles Guide

> These 5 principles are the foundation of clean, maintainable object-oriented design. Every design pattern exists to satisfy one or more of these principles. Understanding SOLID deeply — including tensions between principles — separates senior engineers from staff.

---

## Table of Contents

- [S — Single Responsibility Principle (SRP)](#s--single-responsibility-principle-srp)
- [O — Open/Closed Principle (OCP)](#o--openclosed-principle-ocp)
- [L — Liskov Substitution Principle (LSP)](#l--liskov-substitution-principle-lsp)
- [I — Interface Segregation Principle (ISP)](#i--interface-segregation-principle-isp)
- [D — Dependency Inversion Principle (DIP)](#d--dependency-inversion-principle-dip)
- [Cross-Principle Tensions](#cross-principle-tensions)
- [SOLID in Microservices](#solid-in-microservices)

---

## S — Single Responsibility Principle (SRP)

> "A class should have only one reason to change." — Robert C. Martin

**What it really means:** A class should be responsible to ONE actor (stakeholder). If two different people (or teams) would request changes to the same class for different reasons, that class has too many responsibilities.

### Violation → Fix

**❌ Violation:**
```java
// This class has 3 reasons to change:
// 1. Business logic changes (tax calculation)
// 2. Persistence changes (switch from MySQL to Mongo)
// 3. Presentation changes (email format)
class InvoiceService {
    public double calculateTotal(Invoice invoice) {
        double total = 0;
        for (LineItem item : invoice.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        total += total * 0.18; // Tax — hardcoded!
        return total;
    }

    public void saveToDatabase(Invoice invoice) {
        // JDBC code to save invoice
        Connection conn = DriverManager.getConnection("jdbc:mysql://...");
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO invoices...");
        stmt.execute();
    }

    public void sendEmail(Invoice invoice) {
        // Email formatting + sending
        String body = "<html><body>Invoice #" + invoice.getId() + "...</body></html>";
        emailClient.send(invoice.getCustomerEmail(), "Invoice", body);
    }
}
```

**✅ Fix:**
```java
// Each class has ONE reason to change
class InvoiceTaxCalculator {
    private final TaxRateProvider taxRateProvider;

    public double calculateTotal(Invoice invoice) {
        double subtotal = invoice.getItems().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        return subtotal + subtotal * taxRateProvider.getRate(invoice.getRegion());
    }
}

class InvoiceRepository {
    public void save(Invoice invoice) {
        // Persistence logic — changes only when storage changes
    }
}

class InvoiceNotificationService {
    private final EmailTemplateEngine templateEngine;

    public void sendInvoiceEmail(Invoice invoice) {
        // Notification logic — changes only when notification needs change
    }
}
```

### Interview Q&A

**Q: "How granular should SRP be? Can you take it too far?"**
A: Yes — over-applying SRP creates "ravioli code" (too many tiny classes, hard to follow the flow). The key word is "reason to change," not "does one thing." A `UserService` that handles user CRUD is fine — all those operations change for the same reason (user domain changes). But a `UserService` that also sends emails and generates PDFs has 3 unrelated change drivers.

**Q: "How does SRP apply at the module/service level?"**
A: SRP scales up. A microservice should have a single business capability (bounded context). If your `OrderService` handles orders AND inventory AND notifications, it's a distributed monolith. Change in notification logic shouldn't require redeploying the order service. SRP at service level = one team owns one service.

**Q: "SRP vs DRY — when do they conflict?"**
A: When two classes use the same code but for DIFFERENT REASONS. Example: `InvoiceValidator` and `ShippingValidator` both check address format. DRY says: extract `AddressValidator`. But if invoice addresses and shipping addresses evolve differently (invoices add VAT ID, shipping adds delivery instructions), the shared class becomes a coupling point. **Prefer duplication over wrong abstraction** — merge later when you're sure they evolve together.

---

## O — Open/Closed Principle (OCP)

> "Software entities should be open for extension, but closed for modification."

**What it really means:** You should be able to add new behavior WITHOUT modifying existing, tested, working code. Achieved through abstraction (interfaces, abstract classes) and polymorphism.

### Violation → Fix

**❌ Violation:**
```java
// Adding a new shape requires modifying this method (closed for extension!)
class AreaCalculator {
    public double calculateArea(Object shape) {
        if (shape instanceof Circle c) {
            return Math.PI * c.getRadius() * c.getRadius();
        } else if (shape instanceof Rectangle r) {
            return r.getWidth() * r.getHeight();
        } else if (shape instanceof Triangle t) {
            // New shape = modify this class every time!
            return 0.5 * t.getBase() * t.getHeight();
        }
        throw new IllegalArgumentException("Unknown shape");
    }
}
```

**✅ Fix:**
```java
// Adding a new shape = add a new class, zero changes to existing code
interface Shape {
    double area();
}

class Circle implements Shape {
    private final double radius;
    public double area() { return Math.PI * radius * radius; }
}

class Rectangle implements Shape {
    private final double width, height;
    public double area() { return width * height; }
}

// New shape — just add a class, nothing else changes
class Hexagon implements Shape {
    private final double side;
    public double area() { return (3 * Math.sqrt(3) / 2) * side * side; }
}

class AreaCalculator {
    public double totalArea(List<Shape> shapes) {
        return shapes.stream().mapToDouble(Shape::area).sum();
    }
}
```

### Interview Q&A

**Q: "OCP sounds great in theory, but doesn't it lead to over-engineering?"**
A: Yes, if applied prematurely. **Don't predict variation** — wait for the second use case. If you only have Circle and Rectangle, the if-else is fine. When Triangle arrives, THEN refactor to polymorphism. The rule: "fool me once, use if-else. Fool me twice, use polymorphism." OCP is a principle for managing KNOWN variation, not hypothetical future changes.

**Q: "How does OCP relate to the Strategy pattern?"**
A: Strategy IS OCP in action. The context class (closed for modification) delegates to a strategy interface (open for extension). Adding a new algorithm = new strategy class. No changes to the context. This is why Strategy is the most frequently applicable pattern — any time you see if-else on a type, it's an OCP violation that Strategy can fix.

**Q: "Can you be OCP-compliant with enums?"**
A: Enums inherently violate OCP — adding a new enum value requires modifying the enum and every switch statement that handles it. This is acceptable when: (1) The set is truly fixed (days of week, HTTP methods). (2) You control all switch statements (single module). When the enum grows with business changes (payment types, order states), refactor to polymorphism.

---

## L — Liskov Substitution Principle (LSP)

> "Subtypes must be substitutable for their base types without altering the correctness of the program."

**What it really means:** If your code works with a `Shape`, it should work with ANY subclass of `Shape` without surprises — no exceptions that only one subclass throws, no preconditions that only one subclass requires, no postconditions that only one subclass violates.

### Violation → Fix

**❌ Violation (Classic):**
```java
class Rectangle {
    protected int width, height;

    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

// Square IS-A Rectangle mathematically, but NOT behaviorally!
class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w;  // Surprise! Setting width also changes height
    }

    @Override
    public void setHeight(int h) {
        this.width = h;   // Surprise! Setting height also changes width
        this.height = h;
    }
}

// This test passes for Rectangle but FAILS for Square
void testArea(Rectangle r) {
    r.setWidth(5);
    r.setHeight(4);
    assert r.area() == 20;  // Square: 4*4=16, not 20! LSP violated.
}
```

**✅ Fix:**
```java
// Don't use inheritance for IS-A when behavior differs
interface Shape {
    int area();
}

// Immutable — no setters, no behavioral surprises
record Rectangle(int width, int height) implements Shape {
    public int area() { return width * height; }
}

record Square(int side) implements Shape {
    public int area() { return side * side; }
}
```

### Interview Q&A

**Q: "How do you detect LSP violations in practice?"**
A: Red flags: (1) Subclass throws `UnsupportedOperationException` (e.g., `Stack extends Vector` — stack doesn't support `add(index, element)`). (2) Subclass has preconditions the base doesn't (e.g., "call `init()` before `process()`"). (3) `instanceof` checks in client code (code doesn't trust the base type). (4) Tests pass for base class but fail for subclass. Tool: write tests against the INTERFACE, run them against ALL implementations.

**Q: "Java's Collections and LSP — are there violations?"**
A: Yes. `Collections.unmodifiableList()` returns a `List` that throws `UnsupportedOperationException` on `add()`. Callers expecting a mutable `List` get a runtime surprise — LSP violation. Better: return a different type (`UnmodifiableList` or just `Iterable` if mutation isn't needed). This is why Kotlin has `List` (read-only) and `MutableList` — the type system prevents LSP violations.

**Q: "LSP in microservices — does it apply?"**
A: Yes — at the API contract level. If service B is a drop-in replacement for service A (same API), it must honor the same contract: same response format, same error codes, same latency guarantees. If service B returns 404 where A returned an empty list, clients break. This is contract testing (Pact): verify substitutability at the API boundary.

---

## I — Interface Segregation Principle (ISP)

> "Clients should not be forced to depend on interfaces they do not use."

**What it really means:** Don't create fat interfaces. If a class only needs 2 of 10 methods on an interface, it shouldn't be forced to implement (or depend on) the other 8. Split into smaller, focused interfaces.

### Violation → Fix

**❌ Violation:**
```java
// Fat interface — forces every worker to implement methods they don't need
interface Worker {
    void work();
    void eat();        // Robots don't eat
    void sleep();      // Robots don't sleep
    void attendMeeting(); // Interns don't attend meetings
}

class Robot implements Worker {
    public void work() { /* OK */ }
    public void eat() { throw new UnsupportedOperationException(); }   // Forced!
    public void sleep() { throw new UnsupportedOperationException(); } // Forced!
    public void attendMeeting() { throw new UnsupportedOperationException(); }
}
```

**✅ Fix:**
```java
// Segregated interfaces — implement only what you need
interface Workable {
    void work();
}

interface Feedable {
    void eat();
}

interface Restable {
    void sleep();
}

interface MeetingAttendee {
    void attendMeeting();
}

class Human implements Workable, Feedable, Restable, MeetingAttendee {
    public void work() { /* ... */ }
    public void eat() { /* ... */ }
    public void sleep() { /* ... */ }
    public void attendMeeting() { /* ... */ }
}

class Robot implements Workable {
    public void work() { /* ... */ }
    // No forced methods!
}
```

### Interview Q&A

**Q: "ISP vs SRP — aren't they the same thing?"**
A: Related but different scope. SRP: a CLASS should have one reason to change (about the implementation). ISP: an INTERFACE should be cohesive — don't force clients to depend on methods they don't use (about the contract). You can have a class that satisfies SRP but implements a fat interface (ISP violation). Fix: split the interface, keep the class implementing all pieces if needed.

**Q: "When is a 'fat' interface acceptable?"**
A: When ALL clients use ALL methods. `java.util.List` has ~25 methods — but list clients typically use most of them. The interface is cohesive: all methods relate to "ordered collection." A fat interface is only a problem when different clients use non-overlapping subsets. Also: breaking a heavily-used interface is expensive — if 200 classes implement `Worker`, splitting it is a major refactor. Pragmatism matters.

**Q: "How does ISP relate to microservice API design?"**
A: An API endpoint that returns 50 fields when the caller only needs 3 is an ISP violation at the API level. Fixes: (1) GraphQL — client queries exactly the fields it needs. (2) BFF (Backend for Frontend) — tailored API per client type. (3) Field filtering: `GET /users?fields=name,email`. Don't force mobile clients to download 50 fields when they display 3.

---

## D — Dependency Inversion Principle (DIP)

> "High-level modules should not depend on low-level modules. Both should depend on abstractions."

**What it really means:** Your business logic should not import concrete infrastructure classes (MySQL driver, SMTP client, S3 SDK). Instead, define interfaces in the domain layer, and let infrastructure implement those interfaces. The dependency arrow points inward (toward the domain), not outward.

### Violation → Fix

**❌ Violation:**
```java
// High-level (business logic) depends directly on low-level (MySQL)
class OrderService {
    private MySQLOrderRepository repository;  // Concrete dependency!
    private SendGridEmailClient emailClient;  // Concrete dependency!

    public OrderService() {
        this.repository = new MySQLOrderRepository();  // Hardcoded!
        this.emailClient = new SendGridEmailClient();  // Hardcoded!
    }

    public void placeOrder(Order order) {
        repository.save(order);          // Coupled to MySQL
        emailClient.send(order.email()); // Coupled to SendGrid
    }
}
// Problem: Can't test without MySQL. Can't switch to Postgres. Can't switch email providers.
```

**✅ Fix:**
```java
// Abstractions defined in domain layer
interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
}

interface NotificationService {
    void notifyOrderPlaced(Order order);
}

// High-level module depends on abstractions
class OrderService {
    private final OrderRepository repository;
    private final NotificationService notificationService;

    // Dependencies injected — can be anything that implements the interface
    public OrderService(OrderRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void placeOrder(Order order) {
        repository.save(order);
        notificationService.notifyOrderPlaced(order);
    }
}

// Low-level modules implement abstractions
class PostgresOrderRepository implements OrderRepository { /* ... */ }
class SendGridNotificationService implements NotificationService { /* ... */ }
class InMemoryOrderRepository implements OrderRepository { /* for tests */ }
```

### Interview Q&A

**Q: "DIP vs Dependency Injection (DI) — same thing?"**
A: No. **DIP** is a PRINCIPLE (depend on abstractions). **DI** is a TECHNIQUE (inject dependencies through constructor/setter). **DI Container** is a TOOL (Spring, Guice) that automates DI. You can follow DIP without a DI container (manually pass interfaces). DIP tells you WHAT to do. DI tells you HOW. Spring tells you "I'll do it for you."

**Q: "When is DIP overkill?"**
A: When: (1) There's genuinely only one implementation and no tests need mocking (utility classes like `Math`). (2) The dependency is stable and never changes (Java SDK classes). (3) Internal to a module — DIP at module BOUNDARIES, not inside. Don't create `StringFormatterInterface` for a private helper. Apply DIP at architectural boundaries: domain ↔ infrastructure, service ↔ service.

**Q: "How does DIP enable testability?"**
A: By depending on interfaces, you can inject test doubles: `new OrderService(new InMemoryOrderRepository(), new FakeNotificationService())`. No database, no email server, no network — test runs in milliseconds. Without DIP: tests need a real database, are slow, flaky, and test infrastructure instead of business logic.

---

## Cross-Principle Tensions

> SOLID principles sometimes pull in opposite directions. Recognizing and navigating these tensions is what separates staff from senior engineers.

### SRP vs DRY

**Tension:** Two classes have duplicate code, but they change for DIFFERENT reasons.

**Example:** `InvoiceValidator` and `ShippingValidator` both validate addresses. DRY says extract `AddressValidator`. But invoice addresses need VAT validation, shipping addresses need delivery zone lookup. Shared class becomes a mess.

**Resolution:** Prefer SRP. Tolerate duplication until you're SURE the two evolve together. Martin Fowler: "Duplication is far cheaper than the wrong abstraction."

### OCP vs YAGNI

**Tension:** OCP says "make it extensible." YAGNI says "don't build what you don't need yet."

**Example:** Should you create a `NotificationStrategy` interface when you only have email? OCP says yes. YAGNI says "add the interface when SMS arrives."

**Resolution:** Prefer YAGNI until the second variation appears. Then refactor to OCP. The refactoring cost of adding an interface later is low. The maintenance cost of premature abstraction is ongoing.

### SRP vs Encapsulation

**Tension:** SRP says split the class. Encapsulation says keep related data and behavior together.

**Example:** `Order` has `calculateTotal()`, `validate()`, and `formatForDisplay()`. SRP says: 3 reasons to change → 3 classes. But now `Order`'s fields are exposed to 3 external classes.

**Resolution:** Consider: do these really change for different reasons? Calculation and validation might change together (same business rules). Only split when you see ACTUAL independent change, not hypothetical.

### ISP vs Convenience

**Tension:** ISP says small interfaces. But callers may want one interface to work with.

**Resolution:** Use interface inheritance. Small interfaces compose into larger ones where needed: `interface FullWorker extends Workable, Feedable, Restable {}`. Callers that need everything use `FullWorker`. Callers that need a subset use the specific interface.

---

## SOLID in Microservices

| Principle | Micro-level (class) | Macro-level (service) |
|---|---|---|
| **SRP** | Class has one reason to change | Service owns one bounded context |
| **OCP** | Extend via polymorphism | Extend via new services, not modifying existing |
| **LSP** | Subtypes are substitutable | Service versions honor backward compatibility |
| **ISP** | Thin interfaces | API returns only what clients need (GraphQL, BFF) |
| **DIP** | Depend on interfaces | Services communicate via contracts (not implementations) |

**Key insight:** SOLID principles apply at every level of abstraction — method, class, module, service, system. The problems are the same; the scale differs.

---

> 📐 [SOLID Diagram](solid.excalidraw)
