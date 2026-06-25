# YAGNI — You Aren't Gonna Need It

> "Always implement things when you actually need them, never when you just foresee that you need them." — Ron Jeffries (co-founder of Extreme Programming)

---

## Table of Contents

- [What YAGNI Really Means](#what-yagni-really-means)
- [The Cost of Building What You Don't Need](#the-cost-of-building-what-you-dont-need)
- [Violation → Fix](#violation--fix)
- [YAGNI Decision Framework](#yagni-decision-framework)
- [When YAGNI Doesn't Apply](#when-yagni-doesnt-apply)
- [YAGNI vs Other Principles](#yagni-vs-other-principles)
- [Interview Q&A](#interview-qa)

---

## What YAGNI Really Means

**Common misunderstanding:** "Never plan ahead" or "don't think about architecture."

**Actual meaning:** Don't implement functionality until you have a CONCRETE, CURRENT requirement for it. Don't build abstractions, extension points, or infrastructure for hypothetical future needs. When the future arrives, build what's actually needed — not what you imagined 6 months ago.

YAGNI is NOT anti-planning. It's anti-SPECULATIVE-BUILDING.

```
YAGNI violation:     Building it now because you THINK you'll need it later
NOT a YAGNI violation: Planning architecture that supports current requirements cleanly
```

**The Core Insight:** The features you predict you'll need are almost always WRONG. Either the requirement never materializes, or when it does, it looks completely different from what you imagined. The premature code is wasted work — or worse, it constrains the actual solution when it arrives.

---

## The Cost of Building What You Don't Need

Every line of speculative code has 4 costs, even if the feature IS eventually needed:

| Cost | Description | Example |
|---|---|---|
| **Cost of building** | Time spent implementing unused code | 2 weeks building a plugin system nobody uses |
| **Cost of maintaining** | Every refactor, upgrade, and code review must consider it | Unused Strategy interface bloats every PR |
| **Cost of testing** | Speculative code needs tests to stay green | 50 tests for a feature no customer uses |
| **Cost of understanding** | Every new developer must learn what it does and why | "What's this PluginRegistry? Oh, nobody uses it" |

And the hidden cost: **opportunity cost**. Time spent on speculative features is time NOT spent on features customers actually want.

```
Built speculatively:  2 weeks to build + maintained for 2 years = 100+ hours
Actually needed:      Turns out different from what we predicted → rewrite anyway
Total waste:          100+ hours + the rewrite = worst outcome

Built when needed:    2 weeks to build exactly what's required
Total cost:           2 weeks
```

---

## Violation → Fix

### Violation 1: Speculative Generality

**❌ Violation:**
```java
// "We might need different notification channels someday"
interface NotificationChannel {
    void send(Notification notification);
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

// 5 classes, 2 interfaces... to send emails. The ONLY channel we have.
// "Someday" has not arrived in 18 months.
```

**✅ Fix:**
```java
class EmailNotificationService {
    private final EmailClient client;

    public void send(String to, String subject, String body) {
        client.send(to, subject, body);
    }
}

// When SMS is ACTUALLY needed (not hypothetically):
// 1. Extract NotificationChannel interface (10 min refactor)
// 2. Create SmsNotificationService
// 3. Done. Total cost: 10 min refactor + SMS implementation.
// Saved: 18 months of maintaining unused abstraction.
```

### Violation 2: Premature Configuration

**❌ Violation:**
```java
// "We might want to change these values without redeploying"
class AppConfig {
    @Value("${greeting.prefix:Hello}")
    private String greetingPrefix;

    @Value("${greeting.suffix:!}")
    private String greetingSuffix;

    @Value("${greeting.includeTime:false}")
    private boolean includeTime;

    @Value("${greeting.timeFormat:HH:mm}")
    private String timeFormat;

    @Value("${greeting.maxLength:100}")
    private int maxLength;
}

// 5 configurable properties for a greeting message.
// In 2 years, nobody has EVER changed any of these.
// But every developer must understand what each config does.
```

**✅ Fix:**
```java
class GreetingService {
    public String greet(String name) {
        return "Hello " + name + "!";
    }
}
// Hardcoded. Clear. When (IF) we need configurability, add it THEN.
```

### Violation 3: Premature Optimization

**❌ Violation:**
```java
// "This endpoint might get high traffic, so let's add caching"
class ProductService {
    @Cacheable(value = "products", key = "#id",
               unless = "#result == null",
               cacheManager = "caffeineCacheManager")
    public Product getProduct(String id) {
        return productRepository.findById(id).orElse(null);
    }
}

// Plus: Caffeine cache config, eviction policies, cache warming,
// cache invalidation on product updates, monitoring cache hit rates...
// Current traffic: 10 requests per minute. Cache adds complexity for 0 benefit.
```

**✅ Fix:**
```java
class ProductService {
    public Product getProduct(String id) {
        return productRepository.findById(id).orElse(null);
    }
}
// No cache. Database handles 10 req/min trivially.
// When traffic hits 1000 req/min AND profiling shows DB is the bottleneck,
// THEN add caching with measured parameters.
```

### Violation 4: Unused Extension Points

**❌ Violation:**
```java
// "What if we need to support different databases?"
interface OrderRepository {
    Order findById(String id);
    void save(Order order);
    List<Order> findByStatus(OrderStatus status);
}

class PostgresOrderRepository implements OrderRepository { /* actual implementation */ }

// We've used PostgreSQL for 4 years. No plans to switch.
// But every new query requires: add to interface + implement in Postgres class.
// The interface doubles the work for every change.
```

**✅ Fix:**
```java
@Repository
class OrderRepository {
    public Order findById(String id) { /* JPA/JDBC implementation */ }
    public void save(Order order) { /* ... */ }
    public List<Order> findByStatus(OrderStatus status) { /* ... */ }
}

// No interface. When (IF) we need a second database:
// 1. Extract interface from existing class (IDE refactor: 30 seconds)
// 2. Create new implementation
// 3. The existing class becomes PostgresOrderRepository automatically
```

### Violation 5: Future-Proof Data Model

**❌ Violation:**
```java
// "What if we need to support multiple currencies, languages, and time zones?"
class Product {
    private Map<String, String> names;           // {"en": "Widget", "fr": "Gadget"}
    private Map<String, String> descriptions;    // {"en": "A widget", "fr": "Un gadget"}
    private Map<String, BigDecimal> prices;      // {"USD": 9.99, "EUR": 8.99}
    private Map<String, String> taxCategories;   // {"US": "STANDARD", "EU": "REDUCED"}
    private ZonedDateTime createdAt;             // With timezone
    private String defaultLocale;
    private String defaultCurrency;
}

// We operate in ONE country. ONE currency. ONE language.
// This data model makes every query 3x more complex.
```

**✅ Fix:**
```java
class Product {
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
// When internationalization is ACTUALLY on the roadmap,
// migrate the schema with a planned effort — not premature complexity.
```

---

## YAGNI Decision Framework

When someone proposes a feature or abstraction, ask:

```
1. "Do we need this TODAY for a current user story?"
   YES → Build it.
   NO  → Go to 2.

2. "Is there a concrete, scheduled requirement for this?"
   YES, within 2 sprints → Build it.
   YES, but vague/distant → Don't build it. Revisit when it's imminent.
   NO  → Don't build it.

3. "Will NOT having it make the current implementation significantly harder
    to change later?"
   YES → Build the MINIMAL version (interface only, no implementation)
   NO  → Don't build it. The future refactoring cost is lower than you think.
```

**Key insight:** The cost of refactoring LATER is almost always lower than the cost of maintaining UNUSED code NOW.

Modern tools make refactoring cheap:
- IDE "Extract Interface" → 30 seconds
- IDE "Extract Method" → 10 seconds
- IDE "Change Signature" → propagates everywhere
- Good test coverage → safe refactoring

---

## When YAGNI Doesn't Apply

YAGNI is not universal. Some decisions are expensive to change later and SHOULD be made upfront:

### 1. Security

```
DON'T: "We'll add authentication later"
DO:    "Security from day one — adding it later means retrofitting every endpoint"
```

Security is hard to retrofit. Input validation, authentication, authorization, encryption at rest — design these in from the start.

### 2. Data Model / Database Schema

```
DON'T: "We'll restructure the database when we need to"
DO:    "Think carefully about data relationships — migrations on 100M rows are expensive"
```

Schema changes with data migration are costly. Spend time on the data model upfront.

### 3. Public API Contracts

```
DON'T: "We'll change the API when we need to"
DO:    "Once published, API changes break clients — design carefully"
```

Public APIs are promises. Breaking changes require versioning and migration — much harder than internal refactoring.

### 4. Fundamental Architecture Decisions

```
DON'T: "We'll switch from monolith to microservices later"
DO:    "Choose the right starting architecture for our team size and domain complexity"
```

Monolith → microservices is a multi-month migration. The initial decision matters. (But: when in doubt, start with a monolith — it's easier to split than to merge.)

### Summary: When to Think Ahead

| Decision Type | Reversibility | YAGNI Applies? |
|---|---|---|
| Internal class design | Easy to change | ✅ Yes — don't over-abstract |
| Third-party library choice | Medium to change | ⚠️ Partially — wrap with adapter |
| Database schema | Hard to change at scale | ❌ No — design carefully |
| Public API | Very hard to change | ❌ No — design carefully |
| Security model | Very hard to retrofit | ❌ No — build from day one |
| Deployment architecture | Hard to change | ⚠️ Start simple, but be aware of constraints |

---

## YAGNI vs Other Principles

### YAGNI vs OCP

**Tension:** OCP says "design for extension." YAGNI says "don't build extensions you don't need."

**Resolution:** Build for current requirements using CLEAN CODE (small classes, clear interfaces) so that adding extensions later is easy. Don't build the EXTENSION MECHANISM until you need it. Clean code IS the extension mechanism — refactoring clean code is cheap.

### YAGNI vs DRY

**Tension:** DRY says "extract shared logic." YAGNI says "don't abstract prematurely."

**Resolution:** Rule of Three. First two duplications → tolerate (YAGNI). Third duplication → extract (DRY). This ensures you're abstracting based on real patterns, not hypothetical ones.

### YAGNI vs Defensive Programming

**Tension:** Defensive programming says "handle edge cases." YAGNI says "don't handle cases that can't happen."

**Resolution:** Validate at system BOUNDARIES (user input, API calls, external data). Don't validate internally between trusted components — if your own service passes a null to your own method, that's a bug to fix, not an edge case to handle.

### YAGNI vs SOLID (SRP/DIP)

**Tension:** SRP says split classes. DIP says depend on abstractions. YAGNI says don't create classes or interfaces that aren't needed yet.

**Resolution:** Apply SRP when a class has DEMONSTRATED multiple reasons to change (not hypothetical ones). Apply DIP at system BOUNDARIES (infrastructure ↔ domain) but not between every internal class. A concrete class with no interface is perfectly fine until you have two implementations.

---

## Interview Q&A

**Q: "YAGNI says don't build for the future. But good architecture IS about the future. How do you reconcile?"**
A: YAGNI says don't build FEATURES for the future. It doesn't say don't build CLEAN CODE. Clean, well-structured code with small classes and clear boundaries IS future-proof — not because it has extension points, but because it's easy to refactor. The best way to prepare for an unknown future is to make your code easy to change, not to predict what the change will be.

**Q: "Your manager asks you to build a feature 'because we might need it in Q3.' What do you do?"**
A: (1) Ask: "Is this confirmed for Q3 or speculative?" If confirmed and scheduled → plan for it. If speculative → push back. (2) Quantify the cost: "Building this now takes 2 weeks. If we don't need it, that's 2 weeks wasted plus ongoing maintenance." (3) Propose: "Let's design the current system so it's EASY to add this in Q3, but not build it until it's confirmed."

**Q: "You chose NOT to add an abstraction. 6 months later, you need it and it's a 3-day refactor. Was YAGNI wrong?"**
A: No. The calculation: (1) You saved 6 months of maintaining unused code. (2) The 3-day refactor is informed by REAL requirements — you built exactly what's needed. (3) If you'd built speculatively, it likely wouldn't match the actual need — requiring modification anyway. Total: YAGNI cost you 3 days. Speculative building would have cost 6 months of maintenance + modification when reality diverged.

**Q: "How do you apply YAGNI in an interview system design question?"**
A: Start with the minimal viable design. Then EXPLICITLY state: "I'm starting simple because YAGNI. If the interviewer wants me to handle specific scale/features, I'll add complexity incrementally." This shows discipline. Example: "For 1000 users, a single Postgres instance is sufficient. If we need 1M users, I'd add read replicas and caching. But I won't design for 1M if the requirement is 1000."

**Q: "How does YAGNI apply to testing?"**
A: Don't write tests for features that don't exist. Don't mock scenarios that can't happen. Don't build test infrastructure "in case we need it." BUT: DO test thoroughly for what EXISTS. YAGNI applies to SCOPE, not to QUALITY. 100% coverage of actual features > 30% coverage spread across actual + speculative features.

**Q: "Isn't 'we'll refactor later' just technical debt?"**
A: Only if you DON'T actually refactor when needed. YAGNI works when paired with continuous refactoring. The contract is: "We won't build it now, but we WILL invest in refactoring when the need arises." Without that commitment, YAGNI becomes an excuse for shortcuts. The difference: YAGNI = deliberate deferral of unnecessary work. Technical debt = neglecting necessary work.

**Q: "How do you convince a team that loves over-engineering to adopt YAGNI?"**
A: (1) **Track waste**: for one quarter, tag every feature/abstraction built speculatively. At quarter's end, count how many were actually used. (The number is usually < 20%.) (2) **Measure lead time**: how long to ship a feature through the over-engineered system vs a simpler one? (3) **Celebrate deletion**: make removing unused code a team metric. "We deleted 5K lines this sprint" is an achievement. (4) **Start small**: pick one new feature and build it YAGNI-style. Compare the result with the over-engineered approach.
