# Anti-Patterns Guide

> Anti-patterns are recurring "solutions" that appear helpful but ultimately create more problems than they solve. Recognizing anti-patterns in code reviews, system design, and legacy codebases is a core staff-level skill. Knowing HOW to refactor away from them — incrementally, without big-bang rewrites — is what separates architects from developers.

---

## Table of Contents

- [What Makes Something an Anti-Pattern](#what-makes-something-an-anti-pattern)
- [Design Anti-Patterns](#design-anti-patterns)
  - [God Object / Blob](#god-object--blob)
  - [Singleton Abuse](#singleton-abuse)
  - [Golden Hammer](#golden-hammer)
  - [Lava Flow](#lava-flow)
  - [Poltergeist](#poltergeist)
  - [Circular Dependency](#circular-dependency)
  - [Sequential Coupling](#sequential-coupling)
  - [Speculative Generality](#speculative-generality)
- [Architectural Anti-Patterns](#architectural-anti-patterns)
  - [Distributed Monolith](#distributed-monolith)
  - [Service Locator](#service-locator)
  - [Anemic Domain Model](#anemic-domain-model)
  - [Big Ball of Mud](#big-ball-of-mud)
  - [Inner Platform Effect](#inner-platform-effect)
  - [Vendor Lock-In](#vendor-lock-in)
- [Coding Anti-Patterns](#coding-anti-patterns)
  - [Copy-Paste Programming](#copy-paste-programming)
  - [Magic Numbers / Strings](#magic-numbers--strings)
  - [Primitive Obsession](#primitive-obsession)
  - [Feature Envy](#feature-envy)
  - [Leaky Abstraction](#leaky-abstraction)
  - [Callback Hell / Pyramid of Doom](#callback-hell--pyramid-of-doom)
- [Concurrency Anti-Patterns](#concurrency-anti-patterns)
  - [Double-Checked Locking (Broken)](#double-checked-locking-broken)
  - [Thread-Unsafe Singleton](#thread-unsafe-singleton)
  - [Lock Contention / Over-Synchronization](#lock-contention--over-synchronization)
  - [Deadlock-Prone Design](#deadlock-prone-design)
- [How to Refactor Anti-Patterns](#how-to-refactor-anti-patterns)
- [Interview Questions: Anti-Pattern Recognition](#interview-questions-anti-pattern-recognition)

---

## What Makes Something an Anti-Pattern

An anti-pattern is NOT simply "bad code." It has specific characteristics:

| Property | Description |
|---|---|
| **Initially appealing** | It seems like a reasonable solution at first |
| **Recurring** | Many teams independently make the same mistake |
| **Has negative consequences** | Causes maintenance burden, bugs, or scaling issues over time |
| **Has a known better alternative** | A documented fix or refactoring exists |

**Anti-pattern vs Bad practice:** Anti-patterns are structural/architectural — they infect the design. Bad practices are tactical (missing null check, poor variable name). Anti-patterns require refactoring. Bad practices require a code review comment.

---

## Design Anti-Patterns

---

### God Object / Blob

**What it is:** A single class that knows too much or does too much. It centralizes logic that should be distributed across multiple classes, becoming a bottleneck for changes and a magnet for bugs.

**Why it happens:** Quick feature additions ("just add it to UserService"), lack of design upfront, or one developer who owns a "core" class and keeps adding to it.

**Warning Signs:**
- Class has 1000+ lines
- Class has 30+ methods
- Class imports 20+ other classes
- Every bug fix touches this class
- Multiple developers constantly merge-conflict on this file

**❌ Example:**
```java
// God Object — handles user CRUD, authentication, notifications, reporting, AND caching
class UserManager {
    private Database db;
    private EmailClient email;
    private Cache cache;
    private MetricsCollector metrics;
    private TemplateEngine templates;
    private SMSGateway sms;

    public User createUser(UserDTO dto) { /* 50 lines */ }
    public User findById(String id) { /* ... */ }
    public void updateUser(User user) { /* ... */ }
    public void deleteUser(String id) { /* ... */ }
    public boolean authenticate(String email, String password) { /* 30 lines */ }
    public void resetPassword(String email) { /* 40 lines */ }
    public void sendWelcomeEmail(User user) { /* ... */ }
    public void sendPasswordResetEmail(User user) { /* ... */ }
    public void sendSMS(User user, String message) { /* ... */ }
    public void invalidateCache(String userId) { /* ... */ }
    public void warmUpCache() { /* ... */ }
    public Report generateUserReport(DateRange range) { /* 60 lines */ }
    public void trackUserMetrics(User user, String action) { /* ... */ }
    // ... 40 more methods
}
```

**✅ Refactored:**
```java
class UserRepository {
    public User save(User user) { /* persistence only */ }
    public Optional<User> findById(String id) { /* ... */ }
    public void delete(String id) { /* ... */ }
}

class AuthenticationService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AuthResult authenticate(String email, String password) { /* ... */ }
    public void resetPassword(String email) { /* ... */ }
}

class UserNotificationService {
    private final EmailClient email;
    private final SMSGateway sms;

    public void sendWelcomeEmail(User user) { /* ... */ }
    public void sendPasswordReset(User user) { /* ... */ }
}

class UserReportService {
    public Report generateReport(DateRange range) { /* ... */ }
}
```

**Interview Q&A:**

**Q: "You inherit a God Object with 5000 lines. How do you refactor without breaking production?"**
A: Strangler Fig approach: (1) Identify cohesive method clusters (all notification methods, all persistence methods). (2) Extract one cluster into a new class. (3) Delegate from the God Object to the new class (old callers still work). (4) Gradually update callers to use the new class directly. (5) Remove delegation when all callers migrated. Do ONE cluster at a time — each is a safe, reviewable PR. Never big-bang.

**Q: "How do you prevent God Objects from forming?"**
A: (1) **Code review rule**: "No class > 300 lines without explicit justification." (2) **ArchUnit test**: `classes().should().haveSimpleNameNotEndingWith("Manager")` (tongue-in-cheek but effective). (3) **Domain modeling**: start with bounded contexts and aggregates BEFORE coding. (4) **Team culture**: "Where should this code live?" conversation before adding methods.

**Q: "Is a Spring `@Service` with 15 methods a God Object?"**
A: Depends on cohesion. If all 15 methods operate on the same entity for the same purpose (OrderService: create, update, cancel, refund, query — all order lifecycle), it's probably fine. If it handles orders AND inventory AND notifications, it's a God Object. The test: can you describe the class's responsibility in ONE sentence without using "and"?

---

### Singleton Abuse

**What it is:** Using Singleton for convenience (global access) rather than for genuine single-instance requirements. Results in hidden dependencies, untestable code, and tightly coupled systems.

**Why it happens:** Singleton is easy to use — `MyThing.getInstance()` works from anywhere without passing dependencies. It's the "global variable" of OOP, dressed in a pattern name to feel legitimate.

**Warning Signs:**
- `getInstance()` calls scattered across the codebase
- Tests require complex setup/teardown to reset Singleton state
- Order-dependent tests (test A passes alone, fails after test B — because Singleton state leaked)
- "We can't run tests in parallel" (shared Singleton state)

**❌ Example:**
```java
// Used everywhere — untestable, hides dependencies
class OrderProcessor {
    public void processOrder(Order order) {
        // Hidden dependency #1
        Config config = ConfigSingleton.getInstance();
        double taxRate = config.getTaxRate();

        // Hidden dependency #2
        Logger logger = LoggerSingleton.getInstance();
        logger.log("Processing order " + order.getId());

        // Hidden dependency #3
        DatabaseConnection db = DatabaseSingleton.getInstance();
        db.save(order);

        // Hidden dependency #4
        EmailService email = EmailSingleton.getInstance();
        email.send(order.getCustomerEmail(), "Order confirmed");
    }
}
// How do you test this? You can't mock anything. You need a real DB, real email server.
```

**✅ Refactored:**
```java
class OrderProcessor {
    private final OrderRepository repository;
    private final NotificationService notifications;
    private final TaxCalculator taxCalculator;
    private final Logger logger;

    // All dependencies visible, injectable, mockable
    public OrderProcessor(OrderRepository repository,
                         NotificationService notifications,
                         TaxCalculator taxCalculator,
                         Logger logger) {
        this.repository = repository;
        this.notifications = notifications;
        this.taxCalculator = taxCalculator;
        this.logger = logger;
    }

    public void processOrder(Order order) {
        double tax = taxCalculator.calculate(order);
        logger.info("Processing order {}", order.getId());
        repository.save(order);
        notifications.orderConfirmed(order);
    }
}
// Test: new OrderProcessor(mockRepo, mockNotifier, mockTax, mockLogger) — done.
```

**Interview Q&A:**

**Q: "When IS Singleton legitimate?"**
A: (1) Hardware interface (one printer spooler, one display driver). (2) Thread pool / connection pool (truly shared resource with bounded instances). (3) Configuration loaded once at startup (read-only after init). Even then: prefer DI container singleton scope over manual `getInstance()`. The DI container manages lifecycle, enables testing, and makes the dependency explicit.

**Q: "How do you migrate a codebase away from Singletons?"**
A: (1) Introduce DI container (Spring Boot, Guice). (2) For each Singleton: register it as a singleton-scoped bean. (3) Replace `XSingleton.getInstance()` calls with constructor injection — one class at a time. (4) Delete the `getInstance()` method when no callers remain. Each step is backward-compatible. Takes weeks for a large codebase but each PR is small and safe.

---

### Golden Hammer

**What it is:** "When all you have is a hammer, everything looks like a nail." Using one familiar pattern/technology/approach for every problem, regardless of fit.

**Why it happens:** Comfort, expertise in one area, team familiarity, or a successful past project using that approach ("it worked last time").

**Warning Signs:**
- Every service uses the same architecture (event sourcing for a CRUD app)
- Team uses microservices for a 2-person project
- Everything is abstracted behind a Strategy/Factory even when there's only one implementation
- "We always use Kafka" regardless of whether a simple REST call suffices

**❌ Examples:**
```
// Golden Hammer: "Everything must be an event!"
User clicks "update email" →
  publish EmailUpdateRequested event →
    consumer validates →
      publish EmailValidated event →
        consumer updates DB →
          publish EmailUpdated event →
            consumer sends confirmation email

// vs. what it should be:
User clicks "update email" →
  validate → update DB → send confirmation
  (One synchronous request. Done.)
```

```java
// Golden Hammer: "Every class needs a Factory!"
class StringFormatterFactory {
    public static StringFormatter create() {
        return new StringFormatter();  // Only one implementation. Why does this exist?
    }
}
```

**Interview Q&A:**

**Q: "How do you recognize Golden Hammer in code reviews?"**
A: Ask: "Why this approach?" If the answer is "that's what we always use" instead of "because this problem needs X property that this approach provides," it's a Golden Hammer. Concrete signs: (1) The solution is more complex than the problem. (2) Simpler alternatives exist but weren't considered. (3) The approach requires workarounds to fit the current problem.

**Q: "How do you push back when a team lead insists on their Golden Hammer?"**
A: (1) Don't attack the tool — reframe around the PROBLEM. "What properties does this problem need? Let's evaluate options." (2) Build a comparison: "Kafka adds 3 services + eventual consistency for this. REST call does it in 2 lines, synchronously." (3) Propose an experiment: "Let's prototype both, compare complexity." (4) If they insist: document the trade-off in an ADR — future teams will understand the context.

---

### Lava Flow

**What it is:** Dead code, abandoned experiments, and half-implemented features that remain in the codebase because nobody is sure if they're still needed. Like cooled lava — hard, immovable, and in the way.

**Why it happens:** Developers leave. Features are abandoned mid-implementation. Nobody deletes code "just in case." Fear of removing something that might still be used somewhere.

**Warning Signs:**
- Commented-out code blocks
- Classes/methods with no callers (unused code)
- TODO comments from 3 years ago
- Feature flags that are always on/off
- `*_old`, `*_backup`, `*_v2` class names

**❌ Example:**
```java
class PaymentProcessor {
    // TODO: Remove after migration — added 2021-03-15
    @Deprecated
    public void processPaymentLegacy(Payment p) {
        // Old Stripe integration... 200 lines nobody touches
    }

    // public void processPaymentV2(Payment p) {
    //     // Half-implemented PayPal integration from 2022 Q2 hackathon
    //     // PayPalClient client = new PayPalClient();
    //     // ...
    // }

    public void processPayment(Payment p) {
        // Current implementation
    }

    // Is this still used? Nobody knows. Nobody dares delete it.
    public void processPaymentBatch(List<Payment> payments) {
        // 150 lines of batch logic for a feature that was cancelled
    }
}
```

**✅ Fix:**
```java
class PaymentProcessor {
    public void processPayment(Payment p) {
        // Current implementation — the only one
    }
}
// Delete everything else. Git has history if you truly need it back.
```

**Interview Q&A:**

**Q: "How do you safely identify Lava Flow for removal?"**
A: (1) **Static analysis**: IntelliJ "unused declaration" inspection, or `unused-exports` for JS. (2) **Call graph analysis**: tools like ArchUnit, Deptective, or IDE "find usages." (3) **Runtime monitoring**: add metrics to suspect code — if it's never called in 30 days, it's dead. (4) **Feature flags**: wrap suspect code in a flag, turn it off, wait. No incidents? Delete. (5) **Git blame**: if last modified 3+ years ago and no tests, likely dead.

**Q: "Team is afraid to delete old code. How do you change the culture?"**
A: (1) "Git has history" — repeat until internalized. Deleting code doesn't lose it. (2) Celebrate deletions: "negative LOC" PRs are achievements. (3) Start small: delete one clearly dead file, show nothing breaks. Build confidence. (4) CI coverage reports: uncovered code is a deletion candidate. (5) Scheduled "pruning sprints": dedicate time specifically for cleanup.

---

### Poltergeist

**What it is:** A class that has no real responsibility — it only exists to invoke methods on another class. It adds a layer of indirection without adding value. Like a ghost: it appears, delegates, and vanishes.

**Why it happens:** Misapplied MVC ("we need a controller for everything"), over-engineering, or cargo-culting layer architectures without understanding why each layer exists.

**Warning Signs:**
- Class has only one method that calls one method on another class
- Class name ends in `Controller`, `Manager`, `Handler` but adds no logic
- Removing the class would require only changing the caller's import statement

**❌ Example:**
```java
// Poltergeist — exists only to forward calls
class OrderController {
    private OrderService service;

    public Order getOrder(String id) {
        return service.getOrder(id);  // That's it. No validation, no transformation.
    }

    public void createOrder(OrderDTO dto) {
        service.createOrder(dto);     // Just forwarding. Why does this class exist?
    }
}
```

**✅ Fix:** Eliminate the Poltergeist. Have callers use `OrderService` directly. Only add a controller layer when it adds VALUE (input validation, DTO mapping, authentication checks, response formatting).

**Interview Q&A:**

**Q: "When IS an intermediary class justified?"**
A: When it adds behavior: (1) **Input validation/sanitization** before forwarding. (2) **DTO ↔ Domain mapping** (API layer responsibility). (3) **Authorization checks**. (4) **Response transformation** (hide internal model from API consumers). (5) **Transaction management** (wrap multiple service calls in a transaction). If the class does NONE of these — it's a Poltergeist. Delete it.

---

### Circular Dependency

**What it is:** Class A depends on Class B, which depends on Class A (directly or transitively). Creates initialization problems, makes the system impossible to understand in isolation, and prevents independent deployment of modules.

**Why it happens:** Bidirectional relationships ("User has Orders, Order has User"), feature creep, or lack of clear module boundaries.

**Warning Signs:**
- Spring context fails to start: "Circular reference involving beans..."
- Can't compile one module without the other
- Changing A always requires changing B and vice versa
- Can't test A without initializing B

**❌ Example:**
```java
// Circular: Order → Customer → OrderHistory → Order
class Order {
    private Customer customer;  // Order knows Customer

    public void notifyCustomer() {
        customer.sendNotification("Your order is ready");
    }
}

class Customer {
    private OrderHistory history;  // Customer knows OrderHistory

    public List<Order> getRecentOrders() {
        return history.getRecent(this.id);
    }
}

class OrderHistory {
    public void recordOrder(Order order) {  // OrderHistory knows Order — cycle!
        orders.add(order);
        order.getCustomer().updateLoyaltyPoints();  // And back to Customer!
    }
}
```

**✅ Fix — Break the Cycle:**
```java
// Option 1: Dependency Inversion — introduce interface at the boundary
interface OrderEventListener {
    void onOrderPlaced(OrderEvent event);
}

class Order {
    private String customerId;  // ID reference only, not object reference
}

class Customer implements OrderEventListener {
    public void onOrderPlaced(OrderEvent event) {
        this.loyaltyPoints += event.getPoints();
    }
}

// Option 2: Mediator / Event Bus
class OrderService {
    private final EventBus eventBus;

    public void placeOrder(Order order) {
        repository.save(order);
        eventBus.publish(new OrderPlacedEvent(order.getId(), order.getCustomerId()));
        // Customer and OrderHistory subscribe independently — no circular deps
    }
}
```

**Interview Q&A:**

**Q: "How do you detect circular dependencies before they cause problems?"**
A: (1) **ArchUnit**: `slices().matching("com.myapp.(*)..").should().beFreeOfCycles()` — CI fails on cycles. (2) **Module system** (Java 9 modules, Gradle subprojects): won't compile if circular. (3) **IDE plugin**: IntelliJ "Analyze Dependencies" shows cycles visually. (4) **Maven enforcer plugin**: prevents circular module dependencies. Prevention > detection: define module boundaries FIRST, then code.

**Q: "Circular dependency between microservices — how do you break it?"**
A: Service A calls Service B, B calls A. Fixes: (1) **Extract shared logic** into Service C (both depend on C, not each other). (2) **Event-driven**: A publishes event, B reacts asynchronously (no direct call). (3) **Merge**: if A and B are this coupled, maybe they're one service. (4) **Callback/webhook**: B returns result to A's callback URL instead of calling A's API.

---

### Sequential Coupling

**What it is:** Methods on a class must be called in a specific order, but nothing in the API enforces this. Callers must "just know" the correct sequence. Violation causes subtle bugs.

**Why it happens:** Initializing complex state step-by-step, legacy APIs designed around a procedure, or multi-phase processing without a clear abstraction.

**Warning Signs:**
- Comments like "// Must call init() before process()"
- `IllegalStateException: "Not initialized"`
- Order-dependent bugs that only appear in production
- Classes with `init()`, `prepare()`, `setup()` methods that must be called first

**❌ Example:**
```java
class ReportGenerator {
    private DataSource source;
    private Template template;
    private OutputFormat format;

    public void setDataSource(DataSource ds) { this.source = ds; }
    public void setTemplate(Template t) { this.template = t; }
    public void setOutputFormat(OutputFormat f) { this.format = f; }

    // Caller MUST call all three setters above before calling this.
    // Nothing enforces it. NPE if you forget one.
    public Report generate() {
        return template.render(source.fetchData(), format);  // NPE if template is null!
    }
}
```

**✅ Fix — Use Builder or Constructor to enforce completeness:**
```java
class ReportGenerator {
    private final DataSource source;
    private final Template template;
    private final OutputFormat format;

    // All required dependencies in constructor — can't forget any
    public ReportGenerator(DataSource source, Template template, OutputFormat format) {
        this.source = Objects.requireNonNull(source);
        this.template = Objects.requireNonNull(template);
        this.format = Objects.requireNonNull(format);
    }

    public Report generate() {
        return template.render(source.fetchData(), format);  // Always safe
    }
}

// Or Step Builder for complex multi-step initialization:
Report report = ReportBuilder.withDataSource(source)
    .withTemplate(template)       // Can't skip — returns different type
    .withFormat(OutputFormat.PDF)  // Can't skip — returns different type
    .generate();                  // Only available after all steps
```

**Interview Q&A:**

**Q: "How do you fix Sequential Coupling in an existing API without breaking callers?"**
A: (1) Add the Builder/constructor API as a NEW entry point. (2) Deprecate the old setter-based API. (3) Add runtime validation: if `generate()` is called without all setters, throw a clear error message explaining what's missing. (4) Migrate callers one by one. (5) Delete deprecated API when all callers migrated.

---

### Speculative Generality

**What it is:** Building abstractions, extension points, and flexibility for requirements that don't exist yet. "We might need this someday." The result: complex code that handles imaginary use cases, making the actual use case harder to understand.

**Why it happens:** Well-intentioned developers trying to "future-proof," over-application of OCP ("what if we need another implementation?"), or architects designing for scale that never arrives.

**Warning Signs:**
- Abstract classes with only one concrete subclass
- Strategy interfaces with only one implementation
- Unused generic type parameters
- Configuration for options nobody uses
- Classes named `AbstractBaseGenericFactory`

**❌ Example:**
```java
// We only have one notification channel (email) but "might add more later"
interface NotificationChannel {
    void send(Notification n);
}

interface NotificationChannelFactory {
    NotificationChannel create(ChannelType type);
}

abstract class AbstractNotificationProcessor {
    protected abstract NotificationChannel getChannel();
    protected abstract void preProcess(Notification n);
    protected abstract void postProcess(Notification n);

    public final void process(Notification n) {
        preProcess(n);
        getChannel().send(n);
        postProcess(n);
    }
}

class EmailNotificationProcessor extends AbstractNotificationProcessor {
    protected NotificationChannel getChannel() { return new EmailChannel(); }
    protected void preProcess(Notification n) { /* nothing */ }
    protected void postProcess(Notification n) { /* nothing */ }
}

// 5 classes, 1 interface, 1 abstract class... to send an email.
```

**✅ Fix — YAGNI:**
```java
class EmailNotificationService {
    private final EmailClient client;

    public void send(String to, String subject, String body) {
        client.send(to, subject, body);
    }
}
// When SMS arrives: THEN extract NotificationChannel interface.
// Refactoring cost: 10 minutes. Maintenance cost of premature abstraction: ongoing.
```

**Interview Q&A:**

**Q: "How do you balance OCP with YAGNI?"**
A: **Rule of Three**: don't abstract until you have 3 concrete implementations. One = inline. Two = maybe parameterize. Three = definitely abstract. The cost of abstracting too early (wrong abstraction, unused flexibility) exceeds the cost of refactoring later (straightforward extract-interface).

**Q: "A team member argues for 'future-proofing.' How do you push back?"**
A: Ask: "What concrete requirement drives this?" If the answer is "we might need..." — that's speculative. Counter with: "Let's add a TODO and revisit when the requirement materializes. Right now, simplicity serves the team better." Document the DECISION (ADR) not to abstract, so it's a conscious choice, not oversight.

---

## Architectural Anti-Patterns

---

### Distributed Monolith

**What it is:** A system that has the deployment complexity of microservices but the coupling of a monolith. Services can't be deployed independently, share databases, require coordinated releases, and call each other synchronously in long chains.

**Why it happens:** Splitting a monolith by technical layer (API service, business service, data service) instead of by business capability. Or: microservices that share a database, share libraries with domain logic, or make synchronous chains of calls.

**Warning Signs:**
- Deploying service A requires also deploying services B and C
- Services share a database (or tables within a database)
- Changing a shared library forces redeployment of all services
- One service failure cascades to all other services
- You have a "deployment order" document

**❌ Example:**
```
API Gateway → User Service → Order Service → Payment Service → Inventory Service
                    ↕                 ↕               ↕
                 [Shared PostgreSQL Database — all services read/write same tables]
```

**✅ Fix:**
```
API Gateway → User Service (own DB)
           → Order Service (own DB)
                → publishes OrderCreated event
           → Payment Service (own DB)
                → subscribes to OrderCreated, publishes PaymentProcessed
           → Inventory Service (own DB)
                → subscribes to PaymentProcessed
```

**Interview Q&A:**

**Q: "How do you tell if your microservices are actually a distributed monolith?"**
A: Litmus test: "Can I deploy service A without touching service B?" If no → distributed monolith. Also: "Can I turn off service B and service A still partially works?" If no → too coupled. More signs: shared database, shared domain model library, no async communication, coordinated deployment schedules.

**Q: "How do you evolve a distributed monolith into proper microservices?"**
A: (1) **Database-per-service**: start splitting the shared DB — each service gets its own schema, with data sync via events. (2) **Async communication**: replace synchronous call chains with events (eventual consistency). (3) **API contracts**: define clear interfaces — services can't reach into each other's internals. (4) **Independent deployment**: CI/CD pipeline per service. (5) Do this incrementally — one service boundary at a time.

---

### Service Locator

**What it is:** A registry that provides dependencies to classes at runtime. Classes ask the registry "give me an X" instead of receiving X through their constructor. Hides dependencies and makes code untestable.

**Why it happens:** Pre-dates DI containers. Was the "standard" approach in early J2EE (JNDI lookups). Still appears in legacy code and some frameworks.

**Warning Signs:**
- `ServiceLocator.get(OrderRepository.class)` calls scattered in business logic
- Can't tell what a class depends on without reading its implementation
- Tests must initialize the entire ServiceLocator with mocks
- Dependency graph is invisible to tools

**❌ Example:**
```java
class OrderService {
    public void placeOrder(Order order) {
        // Hidden dependencies — not visible in constructor or method signature
        OrderRepository repo = ServiceLocator.get(OrderRepository.class);
        PaymentGateway payment = ServiceLocator.get(PaymentGateway.class);
        NotificationService notifier = ServiceLocator.get(NotificationService.class);

        repo.save(order);
        payment.charge(order.getTotal());
        notifier.orderPlaced(order);
    }
}
// Q: What does OrderService depend on? A: Read all 500 lines to find out.
```

**✅ Fix — Constructor Injection:**
```java
class OrderService {
    private final OrderRepository repo;
    private final PaymentGateway payment;
    private final NotificationService notifier;

    // Dependencies are VISIBLE, EXPLICIT, and INJECTABLE
    public OrderService(OrderRepository repo, PaymentGateway payment,
                       NotificationService notifier) {
        this.repo = repo;
        this.payment = payment;
        this.notifier = notifier;
    }

    public void placeOrder(Order order) {
        repo.save(order);
        payment.charge(order.getTotal());
        notifier.orderPlaced(order);
    }
}
```

**Interview Q&A:**

**Q: "Service Locator vs DI — is there ever a case for Service Locator?"**
A: Rarely, but yes: (1) Framework/plugin code where you CAN'T control construction (Android `Activity`, some game engines). (2) Legacy code migration — use Service Locator as a stepping stone to DI (wrap static lookups in a locator, then migrate to injection). (3) Optional dependencies that may or may not be present at runtime (plugin loading). But for application code: always prefer DI.

---

### Anemic Domain Model

**What it is:** Domain objects that are pure data holders (getters/setters only) with all business logic in separate "service" classes. The domain objects have no behavior — they're just bags of data being operated on by external procedures.

**Why it happens:** JDBC/ORM-centric thinking ("entities map to tables"), procedural background, or misunderstanding of MVC ("models hold data, services hold logic").

**Warning Signs:**
- Domain classes are all getters/setters with no methods
- Service classes that take domain objects as parameters and mutate them
- Logic like `if (order.getStatus() == Status.PAID && order.getItems().size() > 0)` in services instead of `order.canShip()`
- Domain objects could be replaced with `Map<String, Object>` and nothing would change

**❌ Example:**
```java
// Anemic — just data, no behavior
class Order {
    private String id;
    private List<LineItem> items;
    private OrderStatus status;
    private double total;
    // Only getters and setters — no business methods
}

// All logic lives in the service — procedural, not OOP
class OrderService {
    public void addItem(Order order, Product product, int qty) {
        LineItem item = new LineItem(product, qty);
        order.getItems().add(item);
        double newTotal = 0;
        for (LineItem li : order.getItems()) {
            newTotal += li.getProduct().getPrice() * li.getQuantity();
        }
        order.setTotal(newTotal);
    }

    public boolean canShip(Order order) {
        return order.getStatus() == OrderStatus.PAID
            && !order.getItems().isEmpty()
            && order.getTotal() > 0;
    }
}
```

**✅ Fix — Rich Domain Model:**
```java
class Order {
    private String id;
    private List<LineItem> items;
    private OrderStatus status;

    // Behavior LIVES with the data it operates on
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot modify non-draft order");
        }
        items.add(new LineItem(product, quantity));
    }

    public Money getTotal() {
        return items.stream()
            .map(LineItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }

    public boolean canShip() {
        return status == OrderStatus.PAID && !items.isEmpty();
    }

    public void markPaid() {
        if (items.isEmpty()) throw new IllegalStateException("Cannot pay for empty order");
        this.status = OrderStatus.PAID;
    }
}
```

**Interview Q&A:**

**Q: "When IS an anemic model acceptable?"**
A: (1) Simple CRUD with no business rules — if "add item to order" has no validation, no side effects, no invariants, anemic is fine. (2) DTOs/view models — data transfer objects SHOULD be anemic (they're not domain objects). (3) Small microservices that are essentially data pipelines. Anemic model is a problem only when business rules EXIST but live in the wrong place. No rules? No problem.

**Q: "How do you migrate from anemic to rich domain model?"**
A: (1) Find service methods that take a domain object + operate on its fields. (2) Move that method INTO the domain object. (3) Make fields private (no public setters). (4) Service becomes a thin coordinator that calls domain methods + handles infrastructure (persistence, events). Do one entity at a time — start with the entity that has the most business rules.

---

### Big Ball of Mud

**What it is:** A system with no discernible architecture — no clear module boundaries, everything depends on everything, changes in one area unpredictably break other areas. The software equivalent of spaghetti.

**Why it happens:** Rapid prototyping without refactoring, time pressure ("just ship it"), turnover (no one understands the system), and incremental growth without architectural thinking.

**Warning Signs:**
- No module/package structure — flat package with 200 classes
- Any class can import any other class
- Circular dependencies everywhere
- No one can explain the architecture
- "Don't touch that file, it'll break everything"
- Time-to-onboard new developers: months

**Interview Q&A:**

**Q: "You join a team with a Big Ball of Mud. What's your first month look like?"**
A: Week 1-2: **Map it** — draw dependency graphs (jdeps, Sonargraph), identify the highest-traffic classes (git log frequency), note which files always change together. Week 3-4: **Define boundaries** — propose module boundaries based on business capabilities, get team buy-in. Then: extract the MOST PAINFUL module first (strangler fig), build CI enforcement (ArchUnit) to prevent new violations.

**Q: "Can a Big Ball of Mud ever be the right choice?"**
A: For a prototype or proof-of-concept that will be THROWN AWAY — yes. Speed matters more than structure for throwaway code. The anti-pattern occurs when the prototype becomes production without being rewritten. The business lesson: always communicate that prototypes need rebuilding before scaling.

---

### Inner Platform Effect

**What it is:** Building a highly configurable system that essentially re-implements features that already exist in the underlying platform (programming language, framework, or OS). "We built our own ORM / workflow engine / rule engine / scripting language."

**Why it happens:** Desire for "flexibility," NIH (Not Invented Here) syndrome, or architectural astronauts who enjoy building platforms more than solving business problems.

**Warning Signs:**
- Custom configuration language that's Turing-complete
- Home-grown ORM/framework when mature options exist
- "We can configure any business rule without code changes" (but configuring takes longer than coding would)
- The "platform" team is larger than the "product" team

**Interview Q&A:**

**Q: "How do you distinguish legitimate framework code from Inner Platform?"**
A: Ask: (1) Does an existing OSS solution solve 80%+ of this? If yes → use it. (2) Is the custom solution simpler or more complex than using the platform? If more complex → Inner Platform. (3) Is the team maintaining infrastructure or building product? If >30% on infrastructure → re-evaluate. Legitimate framework: specific to your domain, NOT a re-implementation of general tooling.

---

### Vendor Lock-In

**What it is:** Designing your system so deeply coupled to a specific vendor's APIs, data formats, or services that switching vendors would require rewriting significant portions of the application.

**Why it happens:** Using vendor-specific features without abstraction layers, storing data in proprietary formats, or entire business logic embedded in vendor-specific tools (Salesforce Apex, AWS Step Functions, proprietary workflow engines).

**Warning Signs:**
- AWS/GCP/Azure SDK calls scattered throughout business logic
- Data stored in proprietary formats (no export path)
- Business rules encoded in vendor-specific DSLs
- Vendor price increase = existential risk

**✅ Fix — Hexagonal Architecture:**
```java
// Port (interface in domain layer)
interface FileStorage {
    void store(String key, byte[] data);
    byte[] retrieve(String key);
}

// Adapter (infrastructure layer — easy to swap)
class S3FileStorage implements FileStorage { /* AWS-specific */ }
class GCSFileStorage implements FileStorage { /* GCP-specific */ }
class LocalFileStorage implements FileStorage { /* for tests */ }

// Domain code depends on FileStorage interface — vendor agnostic
class DocumentService {
    private final FileStorage storage;  // Don't know or care if it's S3 or GCS
}
```

**Interview Q&A:**

**Q: "Isn't abstracting over cloud providers premature optimization?"**
A: It depends on the blast radius. For a startup: use AWS directly, ship fast, worry about lock-in when you have revenue. For an enterprise with multi-cloud strategy: abstract from day one. The middle ground: abstract at DOMAIN BOUNDARIES (storage, messaging, compute) but don't abstract everything (you'll never switch CloudWatch for Datadog mid-sprint). DIP at the edges, pragmatism inside.

---

## Coding Anti-Patterns

---

### Copy-Paste Programming

**What it is:** Duplicating code blocks instead of extracting shared logic into reusable functions/classes. When a bug is found, it must be fixed in N places — and inevitably, some copies are missed.

**Why it happens:** "It's faster to copy than to think about abstraction." Deadline pressure. Different teams implementing the same logic independently.

**❌ Example:**
```java
// Same validation logic copied in 5 controllers — when the rule changes, which will you miss?
class UserController {
    public void createUser(UserDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().contains("@") || dto.getEmail().length() > 255) {
            throw new ValidationException("Invalid email");
        }
        // ... rest of logic
    }
}

class AdminController {
    public void createAdmin(AdminDTO dto) {
        if (dto.getEmail() == null || !dto.getEmail().contains("@") || dto.getEmail().length() > 255) {
            throw new ValidationException("Invalid email");
        }
        // ... rest of logic
    }
}
```

**✅ Fix:**
```java
class EmailValidator {
    public static void validate(String email) {
        if (email == null || !email.contains("@") || email.length() > 255) {
            throw new ValidationException("Invalid email");
        }
    }
}
// One source of truth. Bug fix = one place.
```

---

### Magic Numbers / Strings

**What it is:** Unexplained literal values scattered throughout code. `if (status == 3)` — what's 3? `Thread.sleep(86400000)` — why that number? `if (role.equals("ADM_SUPER"))` — is that right?

**Why it happens:** Quick-and-dirty coding, prototypes that became production, or developers who "just know" what the number means.

**❌ Example:**
```java
if (retryCount > 3) { ... }              // Why 3? What changes it?
Thread.sleep(30000);                       // 30 seconds? 30 minutes?
if (amount > 10000) applyDiscount(0.15);   // Why 10000? Why 15%?
```

**✅ Fix:**
```java
private static final int MAX_RETRIES = 3;
private static final Duration RETRY_DELAY = Duration.ofSeconds(30);
private static final Money BULK_DISCOUNT_THRESHOLD = Money.of(10_000);
private static final double BULK_DISCOUNT_RATE = 0.15;

if (retryCount > MAX_RETRIES) { ... }
Thread.sleep(RETRY_DELAY.toMillis());
if (amount > BULK_DISCOUNT_THRESHOLD.value()) applyDiscount(BULK_DISCOUNT_RATE);
```

---

### Primitive Obsession

**What it is:** Using primitive types (String, int, double) to represent domain concepts instead of creating domain-specific types. Leads to invalid states, misuse of values, and scattered validation.

**Why it happens:** "It's just a string" mentality. Primitives are easy. Creating a class "for just an email" feels like over-engineering.

**❌ Example:**
```java
// Which String is what? Can you swap userId and orderId accidentally? YES.
void processOrder(String userId, String orderId, String email, double amount, String currency) {
    // amount can be negative. currency can be "blah". email can be "not an email".
    // NOTHING prevents misuse.
}

// Bug that compiles fine:
processOrder(orderId, userId, "not-an-email", -500, "INVALID");
```

**✅ Fix — Value Objects:**
```java
record UserId(String value) {
    public UserId { Objects.requireNonNull(value); }
}
record OrderId(String value) {
    public OrderId { Objects.requireNonNull(value); }
}
record Email(String value) {
    public Email {
        if (!value.matches("^[\\w.]+@[\\w.]+$")) throw new IllegalArgumentException("Invalid email");
    }
}
record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.signum() < 0) throw new IllegalArgumentException("Negative amount");
    }
}

// Type-safe — can't swap userId and orderId (different types!)
void processOrder(UserId userId, OrderId orderId, Email email, Money payment) {
    // Invalid states are unrepresentable. Compiler catches misuse.
}
```

**Interview Q&A:**

**Q: "When is Primitive Obsession acceptable?"**
A: (1) Internal/private methods where the meaning is clear from context. (2) Performance-critical hot paths where object allocation matters (rare). (3) Simple utilities with no domain meaning (`int count`, `String message`). Apply value objects at DOMAIN BOUNDARIES (public APIs, constructors, DTOs) — not necessarily everywhere internally.

---

### Feature Envy

**What it is:** A method that uses more features (fields/methods) of another class than its own. The method "envies" the other class and should probably live there instead.

**❌ Example:**
```java
class OrderReporter {
    // This method uses 5 fields from Order but zero fields from OrderReporter
    public String generateSummary(Order order) {
        return order.getId() + ": "
            + order.getItems().size() + " items, "
            + order.getTotal() + " "
            + order.getCurrency()
            + " [" + order.getStatus() + "]";
    }
}
```

**✅ Fix:**
```java
class Order {
    public String summary() {
        return id + ": " + items.size() + " items, " + total + " " + currency + " [" + status + "]";
    }
}
```

---

### Leaky Abstraction

**What it is:** An abstraction that exposes implementation details to its consumers, defeating the purpose of the abstraction. Callers must understand the underlying implementation to use the abstraction correctly.

**Why it happens:** Incomplete abstraction, performance edge cases leaking through, or wrapping a complex system with a simple API that can't fully hide the complexity.

**❌ Example:**
```java
// "Simple" cache abstraction that leaks Redis details
interface Cache {
    void put(String key, Object value);
    Object get(String key);

    // Leaks! Callers must know about Redis-specific concerns:
    void setTTL(String key, int seconds);  // Not all cache implementations have TTL
    void selectDatabase(int dbIndex);       // Redis-specific concept
    String executeCommand(String rawRedisCommand);  // Completely exposes Redis
}
```

**✅ Fix:**
```java
interface Cache {
    void put(String key, Object value, Duration ttl);  // TTL is part of put, not separate
    Optional<Object> get(String key);
    void evict(String key);
    // NO implementation-specific methods. Any cache backend works.
}
```

**Interview Q&A:**

**Q: "Joel Spolsky says 'All non-trivial abstractions leak.' Does that mean we shouldn't abstract?"**
A: It means: accept that SOME leakage is inevitable, but minimize it. Design abstractions around the COMMON case — 95% of callers shouldn't need to peek behind the curtain. For the 5% that do: provide an escape hatch (access to the underlying implementation) rather than polluting the main interface. SQL is a leaky abstraction (you must know about indices for performance) — but it's still vastly better than raw file I/O.

---

### Callback Hell / Pyramid of Doom

**What it is:** Deeply nested callbacks where each async operation triggers the next, creating unreadable, unmaintainable code shaped like a pyramid. Common in Node.js, but any callback-based system is susceptible.

**❌ Example:**
```java
// Callback hell in async Java
userService.getUser(userId, user -> {
    orderService.getOrders(user.getId(), orders -> {
        paymentService.getPayments(orders.get(0).getId(), payments -> {
            notificationService.send(user.getEmail(), payments, result -> {
                auditService.log("notification sent", logResult -> {
                    // 5 levels deep. Error handling? Good luck.
                });
            });
        });
    });
});
```

**✅ Fix — CompletableFuture chain:**
```java
userService.getUser(userId)
    .thenCompose(user -> orderService.getOrders(user.getId()))
    .thenCompose(orders -> paymentService.getPayments(orders.get(0).getId()))
    .thenCompose(payments -> notificationService.send(email, payments))
    .thenCompose(result -> auditService.log("notification sent"))
    .exceptionally(ex -> handleError(ex));
// Flat, readable, single error handler.
```

---

## Concurrency Anti-Patterns

---

### Double-Checked Locking (Broken)

**What it is:** An attempt to optimize lazy initialization by checking the condition twice — but implemented WITHOUT `volatile`, causing a subtle visibility bug where threads can see a partially constructed object.

**❌ Broken:**
```java
class Singleton {
    private static Singleton instance;  // BUG: not volatile!

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                    // JVM may reorder: allocate → assign reference → run constructor
                    // Another thread sees non-null instance before constructor finishes!
                }
            }
        }
        return instance;  // May return partially constructed object
    }
}
```

**✅ Fix:**
```java
// Option 1: volatile (correct DCL)
private static volatile Singleton instance;

// Option 2: Holder idiom (simpler, preferred)
class Singleton {
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    public static Singleton getInstance() {
        return Holder.INSTANCE;  // Lazy, thread-safe, no synchronization needed
    }
}

// Option 3: Enum (best for true singletons)
enum Singleton { INSTANCE; }
```

---

### Thread-Unsafe Singleton

**What it is:** A Singleton implementation that allows multiple instances to be created under concurrent access, defeating the entire purpose of the pattern.

**❌ Race condition:**
```java
class Singleton {
    private static Singleton instance;

    public static Singleton getInstance() {
        // Thread A checks: null → enters if block
        // Thread B checks: null → also enters if block (before A finishes)
        // Result: TWO instances created
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

---

### Lock Contention / Over-Synchronization

**What it is:** Protecting too much code with a single lock, creating a bottleneck where threads wait in line even when they're accessing independent resources.

**❌ Example:**
```java
class UserCache {
    private final Map<String, User> cache = new HashMap<>();

    // ALL operations on the entire cache are serialized
    // Thread A updating user "alice" blocks Thread B reading user "bob"
    public synchronized User get(String id) { return cache.get(id); }
    public synchronized void put(String id, User user) { cache.put(id, user); }
    public synchronized void evict(String id) { cache.remove(id); }
    public synchronized int size() { return cache.size(); }
}
```

**✅ Fix — Minimize lock scope and granularity:**
```java
class UserCache {
    // ConcurrentHashMap: lock-free reads, per-segment write locks
    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();

    public User get(String id) { return cache.get(id); }  // No lock!
    public void put(String id, User user) { cache.put(id, user); }  // Minimal lock
    public void evict(String id) { cache.remove(id); }

    // For compound operations:
    public User computeIfAbsent(String id, Function<String, User> loader) {
        return cache.computeIfAbsent(id, loader);  // Atomic, per-key locking
    }
}
```

**Interview Q&A:**

**Q: "How do you identify lock contention in production?"**
A: (1) **Thread dump analysis**: `jstack <pid>` — look for many threads in `BLOCKED` state on the same monitor. (2) **JMX/Micrometer metrics**: expose lock wait time. (3) **Async profilers** (async-profiler, JFR): show lock contention flame graphs. (4) **Symptoms**: CPU is idle but throughput is low — threads are waiting, not working.

---

### Deadlock-Prone Design

**What it is:** Multiple threads acquiring multiple locks in different orders, creating a cycle where each thread holds a lock the other needs.

**❌ Example:**
```java
// Thread 1: lock(A) → lock(B)
// Thread 2: lock(B) → lock(A)
// Deadlock! Each thread holds one lock and waits for the other.

class TransferService {
    public void transfer(Account from, Account to, Money amount) {
        synchronized (from) {           // Thread 1: locks "alice"
            synchronized (to) {         // Thread 1: waits for "bob"
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
// transfer(alice, bob, $50) + transfer(bob, alice, $30) = DEADLOCK
```

**✅ Fix — Consistent lock ordering:**
```java
class TransferService {
    public void transfer(Account from, Account to, Money amount) {
        // Always lock in consistent order (by ID) — prevents cycles
        Account first = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Account second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
// Both threads now lock in same order → no deadlock possible
```

**Interview Q&A:**

**Q: "How do you detect deadlocks in production?"**
A: (1) **JVM built-in**: `jstack` detects and reports deadlock cycles automatically. (2) **JMX**: `ThreadMXBean.findDeadlockedThreads()`. (3) **Monitoring**: alert on thread count growing while throughput drops. (4) **Prevention is better**: use `tryLock(timeout)` instead of `synchronized` — if you can't get the lock in 5 seconds, back off and retry. (5) **Lock-free algorithms**: `ConcurrentHashMap`, `AtomicReference`, CAS operations.

---

## How to Refactor Anti-Patterns

> Anti-pattern removal in production codebases must be incremental. Big-bang rewrites fail. Here's the systematic approach:

### The Strangler Fig Method

```
1. IDENTIFY: Find the anti-pattern. Measure its cost (bugs, change frequency, time-to-modify).
2. WRAP: Put an interface/facade around the problematic code.
3. IMPLEMENT: Build the clean solution behind the interface.
4. REDIRECT: Route callers to the new implementation one by one.
5. DELETE: Remove the old code when no callers remain.
```

### Prioritization Matrix

| Impact | Frequency of Change | Action |
|---|---|---|
| High pain | Changes often | Refactor NOW — highest ROI |
| High pain | Rarely changes | Monitor — refactor when next touched |
| Low pain | Changes often | Refactor soon — prevents future pain |
| Low pain | Rarely changes | Leave it — refactoring cost > benefit |

### Refactoring Budget Rule

Allocate 15-20% of sprint capacity to technical debt. Don't ask for permission — include it alongside feature work. "This feature touches the God Object, so we'll also extract the affected methods into a proper service."

---

## Interview Questions: Anti-Pattern Recognition

> These questions test your ability to identify, diagnose, and fix anti-patterns under pressure. Staff-level engineers are expected to spot these in code reviews and propose incremental fixes.

**Q1: "You're reviewing a PR that introduces a Singleton. What questions do you ask?"**
A: (1) "Does this object NEED to be single-instance, or do you just want convenient access?" (If just convenience → DI). (2) "How will this be tested?" (If `getInstance()` in tests → problem). (3) "Is this object stateless or stateful?" (Stateful singletons = global mutable state = bugs). (4) "Could this be `@Singleton` scope in your DI container instead?" (Almost always yes).

**Q2: "A service has 47 dependencies injected via constructor. What anti-pattern is this, and how do you fix it?"**
A: **God Object** — 47 dependencies means 47 reasons to change. Fix: (1) Group related dependencies into cohesive services (OrderCreation, OrderFulfillment, OrderNotification). (2) Apply Facade — maybe callers don't need all 47 capabilities. (3) Check for Poltergeists — some dependencies might just be forwarding calls. (4) Domain decomposition — this service probably spans multiple bounded contexts.

**Q3: "Your team has a shared library used by 12 services. Every change breaks multiple services. What's the anti-pattern?"**
A: **Distributed Monolith** via shared library coupling. The library contains domain logic, not just utilities. Fix: (1) Split the library: utilities (stable, shared) vs domain logic (should live in each service). (2) Version the library — services opt-in to upgrades (don't force latest). (3) Evaluate: maybe some services should OWN their logic rather than sharing it.

**Q4: "Legacy code has 500 `if-else` branches based on customer type. What anti-patterns are present?"**
A: (1) **Golden Hammer** (if-else for everything). (2) **OCP violation** (adding a customer type = modifying existing code). (3) Possibly **Primitive Obsession** (`String customerType` instead of a type hierarchy). Fix: **Strategy + Factory** — each customer type becomes a class implementing a `CustomerBehavior` interface. Factory creates the right behavior based on type. 500 branches → 50 classes (much more maintainable).

**Q5: "A developer argues: 'We need to build our own caching framework because Redis doesn't support our exact use case.' Respond."**
A: Warning sign for **Inner Platform Effect**. Questions to ask: (1) "What specific use case does Redis not support?" (Usually it does with the right data structure). (2) "How long to build and maintain your own vs. working around Redis's limitation?" (3) "Who maintains it when you leave the team?" Recommendation: use Redis with an adapter for your specific need. Only build custom if the gap is fundamental and no library covers it.

**Q6: "You find a class with methods: init(), loadConfig(), prepare(), validate(), execute(), cleanup(). What anti-pattern?"**
A: **Sequential Coupling** — callers must know the exact order. Calling `execute()` without `prepare()` = undefined behavior. Fix: (1) Builder pattern (enforce order via types). (2) Template Method (framework calls steps in order, subclass provides implementations). (3) Single entry point: `execute()` internally calls init → load → prepare → validate → run → cleanup. Callers shouldn't manage internal lifecycle.

**Q7: "Two teams independently wrote similar validation logic. One uses regex, the other uses a library. Is this an anti-pattern?"**
A: **Copy-Paste Programming** at the team level (accidental duplication). Fix depends on: do these validations evolve together? If yes → extract shared library/service. If no (different business contexts, different rules) → it's NOT duplication, it's coincidence. Merging coincidental duplication creates coupling that will break later. Ask: "If team A's rules change, should team B's automatically change too?"

**Q8: "A microservice passes JSON through 4 services unchanged (receives from A, forwards to B, B forwards to C). What anti-pattern?"**
A: **Poltergeist** at the service level — intermediary services add no value, just forward data. Also potentially a **Distributed Monolith** (tight coupling chain). Fix: (1) Direct communication between A and C (skip intermediaries). (2) Event bus (A publishes, C subscribes — no forwarding). (3) If intermediaries DO add headers/auth → they're legitimate middleware, not Poltergeists.

---

> 📐 [Anti-Pattern Diagram](Anti-Pattern.excalidraw)
