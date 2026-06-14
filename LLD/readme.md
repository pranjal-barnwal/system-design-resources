# Low Level Design Notes: Design Patterns
Comprehensive notes covering all GoF patterns plus modern patterns. Every pattern includes: **internal mechanics, SOLID connections, explicit differentiation from confused patterns, interview Q&A, production examples, and tradeoff analysis**.

---

## Quick Reference Matrix

| Pattern | Category | One-Line Purpose | Core Mechanism | SOLID Principle | Confused With | Killer Example | Key Tradeoff | Modern Alternative | Freq |
|---------|----------|-----------------|---------------|----------------|---------------|---------------|-------------|-------------------|------|
| [Singleton](#singleton) | Creational | One instance, global access | Private ctor + static accessor | — | Service Locator | Spring bean scope | Global access ↔ Hidden coupling, testability | DI container scoping | Very High |
| [Factory Method](#factory-method) | Creational | Delegate instantiation to subclasses | Inheritance + polymorphic create | OCP, DIP | Abstract Factory, Static Factory | `Collection.iterator()` | Extensibility ↔ Parallel class hierarchies | Lambda/Supplier | Very High |
| [Abstract Factory](#abstract-factory) | Creational | Create families of related objects | Composition of factory methods | OCP, DIP | Factory Method | Cross-platform UI widgets | Family consistency ↔ Costly to add new product | Config-based factory map | High |
| [Builder](#builder) | Creational | Construct complex objects step-by-step | Fluent method chaining | SRP | Factory | `HttpRequest.newBuilder()` | Readable construction ↔ Verbose boilerplate | Kotlin DSL, records | Very High |
| [Prototype](#prototype) | Creational | Clone existing objects | Clone/copy method | — | Factory | Game engine prefabs | Cheap duplication ↔ Deep vs shallow copy | `copy()`, serialize-deserialize | Medium |
| [Adapter](#adapter) | Structural | Make incompatible interfaces work together | Wraps adaptee, implements target | DIP, OCP | Bridge, Facade | `Arrays.asList()` | Integration ↔ One more layer to maintain | — | Very High |
| [Bridge](#bridge) | Structural | Separate abstraction from implementation | Two hierarchies + composition | OCP, SRP | Strategy, Adapter | JDBC Driver/Connection | M+N classes ↔ Upfront design complexity | — | Medium |
| [Composite](#composite) | Structural | Treat individual and group objects uniformly | Recursive tree + shared interface | OCP, LSP | Decorator | File system (File + Directory) | Uniform API ↔ Type safety loss | — | High |
| [Decorator](#decorator) | Structural | Add behavior dynamically without subclassing | Wraps same interface, delegates | OCP, SRP | Proxy, Chain of Resp | Java I/O streams | Flexible composition ↔ Many small objects, order matters | AOP, annotations | Very High |
| [Facade](#facade) | Structural | Simplified interface to a complex subsystem | Delegates to subsystem classes | — | Adapter, Mediator | `JdbcTemplate` | Simplicity ↔ Hides power-user access | — | High |
| [Flyweight](#flyweight) | Structural | Share state to support large numbers of objects | Intrinsic (shared) + extrinsic (passed) | — | Singleton, Cache | `Integer.valueOf(127)` | Memory savings ↔ Complexity of state separation | Object pooling | Medium |
| [Proxy](#proxy) | Structural | Controlled access to another object | Same interface, controls delegation | SRP | Decorator | Spring `@Transactional` | Transparent indirection ↔ Hidden latency | Dynamic proxy, AOP | High |
| [Chain of Responsibility](#chain-of-responsibility) | Behavioral | Pass request along a chain of handlers | Linked handlers, pass-or-handle | OCP, SRP | Decorator | Express.js middleware | Decoupled handlers ↔ No guarantee of handling | Middleware pipelines | High |
| [Command](#command) | Behavioral | Encapsulate a request as an object | Action as first-class object | SRP, OCP | Strategy | DB migrations (`up`/`down`) | Undo/queue/log ↔ Indirection overhead | Lambdas, CQRS | High |
| [Iterator](#iterator) | Behavioral | Sequential access without exposing internals | Cursor + hasNext/next | SRP | — | Java enhanced for-loop | Uniform traversal ↔ Sequential only | Streams, generators | Medium |
| [Mediator](#mediator) | Behavioral | Centralize complex communication | Central hub, N connections | SRP | Observer, Facade | Redux store | Decoupled components ↔ God mediator risk | Event bus | Medium |
| [Memento](#memento) | Behavioral | Capture and restore object state | Opaque state snapshot | — | Command | Game save/checkpoint | Simple undo ↔ Memory cost of snapshots | Event sourcing | Medium |
| [Observer](#observer) | Behavioral | Notify dependents of state changes | Subject maintains subscriber list | OCP | Mediator, Pub/Sub | DOM `addEventListener` | Loose coupling ↔ Cascade/leak risks | Reactive Streams (RxJava) | Very High |
| [State](#state) | Behavioral | Alter behavior when internal state changes | Context delegates to state object | OCP, SRP | Strategy | TCP connection states | Clean state logic ↔ Class explosion per state | State machine libs (XState) | High |
| [Strategy](#strategy) | Behavioral | Swap algorithms at runtime | Context delegates to strategy | OCP, DIP | State, Template Method | `Comparator<T>` | Runtime flexibility ↔ Client must know strategies | Lambdas/functions | Very High |
| [Template Method](#template-method) | Behavioral | Define algorithm skeleton, defer steps | Inheritance + hook methods | OCP, DIP | Strategy | JUnit setUp/test/tearDown | Fixed structure ↔ Fragile base class | Strategy + composition | High |
| [Visitor](#visitor) | Behavioral | Add operations without modifying elements | Double dispatch (accept + visit) | OCP, SRP | Strategy | Compiler AST visitors | Easy new ops ↔ Hard new element types | Pattern matching | Medium |

---

## Part 1: Creational Patterns

> Creational patterns abstract the instantiation process — they help make a system independent of how objects are created, composed, and represented.

---

### Singleton

**Definition:** The Singleton pattern ensures that a class has only one instance and provides a global point of access to that instance. It encapsulates the instantiation logic within the class itself, restricting construction to a single object throughout the application lifecycle.

**In Simple Terms:** Imagine a country can have only one president at a time. No matter who asks "who is the president?", everyone gets the same answer — the same person. Singleton works the same way: no matter how many times you ask for the object, you always get the exact same one.

**Intent:** Ensure a class has exactly one instance and provide a global point of access to it.

**Core Mechanism:**
- Private constructor prevents external instantiation
- Static field holds the single instance
- Static accessor method returns that instance (creating it on first call if lazy)
- In multithreaded environments: double-checked locking with `volatile`, enum-based, or eager initialization

**Internal Mechanics (Why it works):**
```
1. Client calls Singleton.getInstance()
2. First call: instance == null → synchronized block → create instance → store in static field
3. Subsequent calls: instance != null → return immediately (no synchronization cost)
4. Double-checked locking: outer null check avoids synchronization overhead for 99% of calls
5. `volatile` prevents instruction reordering — ensures instance is fully constructed before visible to other threads
```

**The `volatile` keyword here prevents this:** Without it, the JVM may reorder the constructor's memory writes. Another thread could see a non-null reference to a partially constructed object (fields not yet initialized). `volatile` establishes a happens-before relationship guaranteeing full construction before publication.

**SOLID Connection:** Singleton violates SRP (the class manages its own lifecycle AND its core business) and DIP (clients depend on the concrete class, not an abstraction). This is why DI containers replaced manual Singleton.

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Static Class** | Singleton can implement interfaces, be lazily loaded, passed as parameter, mocked. Static class cannot. |
| **vs. Service Locator** | Both provide global access. Service Locator is a registry holding many services. Singleton is one specific instance. Service Locator is a bigger anti-pattern. |
| **vs. DI Singleton Scope** | Same goal (one instance), different mechanism. DI singleton is container-managed → testable, injectable. Manual Singleton is self-managed → hidden dependency. |
| **vs. Flyweight** | Both share instances. Flyweight has many shared instances (pool). Singleton has exactly one. |

**Implementation Variants:**

| Approach | Thread-Safe? | Lazy? | Reflection-Safe? | Serialization-Safe? |
|---|---|---|---|---|
| Eager init (static field) | ✅ | ❌ | ❌ | ❌ |
| Double-checked locking | ✅ | ✅ | ❌ | ❌ |
| Bill Pugh (inner class) | ✅ | ✅ | ❌ | ❌ |
| **Enum (recommended)** | ✅ | ✅ (JVM lazy loads classes) | ✅ | ✅ |

**Interview Q&A:**

**Q: "How do you break a Singleton?"**
A: (1) Reflection — set constructor accessible, call it. (2) Serialization — deserialize creates new instance. (3) Cloning — `clone()` creates a copy. (4) Multiple classloaders — each loader creates its own instance.

**Q: "Why is Singleton considered an anti-pattern?"**
A: Hidden coupling (any class can access it without declaring the dependency), untestable (can't substitute a mock easily), shared mutable state (concurrency hazard), violates SRP and DIP. Modern solution: let DI container manage singleton scope — same one-instance guarantee without the downsides.

**Q: "When IS Singleton justified?"**
A: When the class literally represents a system-level unique resource (Runtime, Logger coordination point, thread pool) AND you can't use a DI container. In practice: almost never in application code, sometimes in infrastructure/framework code.

**When to use:**
- Shared resource that's expensive to create (connection pools, thread pools)
- Configuration that must be consistent across the app (but `@ConfigurationProperties` is better)
- Logger coordination (but SLF4J `LoggerFactory` is already one)
- Registry or service locator (carefully)

**When to avoid:**
- When testability matters (singletons are global state — hard to mock/reset between tests)
- When you confuse "I only need one" with "the system must enforce only one"
- In distributed systems (singleton per-JVM ≠ singleton per-cluster)
- When it introduces hidden coupling (anything can access global state)

**Real-world examples:**
- Java: `Runtime.getRuntime()`, Spring beans (default singleton scope)
- Python: module-level instances (modules are naturally singletons)
- Spring's `ApplicationContext` — one per application, globally accessible

**Production war story:** "At Bentley, our configuration service uses Spring singleton scope — the container manages the lifecycle. We never use manual Singleton because it makes unit testing impossible without PowerMock hacks."

<details>
<summary><strong>Code Examples — All Singleton Variants (from Low-Level-Design-Course/Lecture 10)</strong></summary>

```java
// 1. No Singleton — demonstrates the problem (multiple instances created)
public class NoSingleton {
    public NoSingleton() {
        System.out.println("Singleton Constructor called. New Object created.");
    }

    public static void main(String[] args) {
        NoSingleton s1 = new NoSingleton();
        NoSingleton s2 = new NoSingleton();
        System.out.println(s1 == s2); // false — two different objects!
    }
}

// 2. Simple Singleton (Lazy Init — NOT thread-safe)
public class SimpleSingleton {
    private static SimpleSingleton instance = null;

    private SimpleSingleton() {
        System.out.println("Singleton Constructor called");
    }

    public static SimpleSingleton getInstance() {
        if (instance == null) {
            instance = new SimpleSingleton();
        }
        return instance;
    }

    public static void main(String[] args) {
        SimpleSingleton s1 = SimpleSingleton.getInstance();
        SimpleSingleton s2 = SimpleSingleton.getInstance();
        System.out.println(s1 == s2); // true
    }
}

// 3. Thread-Safe with Synchronized Block (correct but SLOW — lock on every call)
public class ThreadSafeLockingSingleton {
    private static ThreadSafeLockingSingleton instance = null;

    private ThreadSafeLockingSingleton() {
        System.out.println("Singleton Constructor Called!");
    }

    public static ThreadSafeLockingSingleton getInstance() {
        synchronized (ThreadSafeLockingSingleton.class) { // Lock for thread safety
            if (instance == null) {
                instance = new ThreadSafeLockingSingleton();
            }
            return instance;
        }
    }

    public static void main(String[] args) {
        ThreadSafeLockingSingleton s1 = ThreadSafeLockingSingleton.getInstance();
        ThreadSafeLockingSingleton s2 = ThreadSafeLockingSingleton.getInstance();
        System.out.println(s1 == s2); // true
    }
}

// 4. Double-Checked Locking (RECOMMENDED for lazy init — fast + thread-safe)
public class ThreadSafeDoubleLockingSingleton {
    private static ThreadSafeDoubleLockingSingleton instance = null;

    private ThreadSafeDoubleLockingSingleton() {
        System.out.println("Singleton Constructor Called!");
    }

    public static ThreadSafeDoubleLockingSingleton getInstance() {
        if (instance == null) { // First check (no locking — fast path for 99% of calls)
            synchronized (ThreadSafeDoubleLockingSingleton.class) { // Lock only if needed
                if (instance == null) { // Second check (after acquiring lock)
                    instance = new ThreadSafeDoubleLockingSingleton();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        ThreadSafeDoubleLockingSingleton s1 = ThreadSafeDoubleLockingSingleton.getInstance();
        ThreadSafeDoubleLockingSingleton s2 = ThreadSafeDoubleLockingSingleton.getInstance();
        System.out.println(s1 == s2); // true
    }
}

// 5. Eager Initialization (simplest thread-safe — instance created at class load)
public class ThreadSafeEagerSingleton {
    private static ThreadSafeEagerSingleton instance = new ThreadSafeEagerSingleton();

    private ThreadSafeEagerSingleton() {
        System.out.println("Singleton Constructor Called!");
    }

    public static ThreadSafeEagerSingleton getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        ThreadSafeEagerSingleton s1 = ThreadSafeEagerSingleton.getInstance();
        ThreadSafeEagerSingleton s2 = ThreadSafeEagerSingleton.getInstance();
        System.out.println(s1 == s2); // true
    }
}
```

</details>

---

### Factory Method

**Definition:** The Factory Method pattern defines an interface for creating an object, but lets subclasses alter the type of objects that will be created. It delegates the responsibility of instantiation to derived classes through polymorphism, decoupling the client from concrete product classes.

**In Simple Terms:** Think of a pizza store franchise. The headquarters defines HOW to run a store (take order, bake, box, deliver), but each city’s branch decides WHICH type of pizza to make. New York makes thin-crust, Chicago makes deep-dish — same process, different products created by different "factories."

**Intent:** Define an interface for creating an object, but let subclasses decide which class to instantiate.

**Core Mechanism:**
- A base class declares an abstract/virtual creation method returning a product interface
- Subclasses override this method to return different concrete product types
- Client code works with the base class and product interface — never knows the concrete types
- The "decision" of WHAT to create is pushed into the subclass via polymorphism

**Internal Mechanics (Why it works):**
```
Client → calls Creator.someOperation()
  → someOperation() internally calls this.createProduct() (factory method)
  → At runtime, `this` is a ConcreteCreator subclass
  → Polymorphism routes to ConcreteCreator.createProduct()
  → Returns ConcreteProduct (client sees only Product interface)
```

The indirection through inheritance means adding new products requires ONLY a new creator subclass — zero changes to existing code (OCP).

**SOLID Connection:**
- **OCP:** New product types added by creating new subclasses, not modifying existing code
- **DIP:** High-level code depends on abstract `Product` interface, not concrete implementations
- **SRP:** Object creation logic is separated from object usage logic

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Simple Factory (static method)** | Simple Factory uses a static method with if-else/switch — violates OCP (must modify factory for new types). Factory Method uses polymorphism — add new subclass for new types. |
| **vs. Abstract Factory** | Factory Method creates ONE product via inheritance. Abstract Factory creates a FAMILY of related products via composition of multiple factory methods. |
| **vs. Builder** | Factory Method selects WHICH type to create. Builder handles HOW to construct a complex object step-by-step. |
| **vs. Prototype** | Factory Method creates new instances via `new`. Prototype creates copies of existing instances via `clone()`. |

**The Three Factories — Clear Differentiation:**

```java
// 1. Simple Factory (NOT a GoF pattern — just a helper method)
class NotificationFactory {
    static Notification create(String type) {
        return switch (type) {  // Violates OCP — must modify for new types
            case "email" -> new EmailNotification();
            case "sms" -> new SmsNotification();
            default -> throw new IllegalArgumentException();
        };
    }
}

// 2. Factory Method (GoF — inheritance-based polymorphic creation)
abstract class NotificationService {
    abstract Notification createNotification(); // Subclass decides
    
    public void send(String message) {
        Notification n = createNotification(); // Factory method call
        n.setMessage(message);
        n.deliver();
    }
}
class EmailNotificationService extends NotificationService {
    Notification createNotification() { return new EmailNotification(); }
}
// Adding SMS: create SmsNotificationService — NO changes to existing code

// 3. Abstract Factory (GoF — family of related products)
interface NotificationFactory {
    Notification createNotification();
    Template createTemplate();     // Family: notification + template
    Formatter createFormatter();   // All must be compatible
}
class EmailFactory implements NotificationFactory {
    public Notification createNotification() { return new EmailNotification(); }
    public Template createTemplate() { return new HtmlTemplate(); }
    public Formatter createFormatter() { return new HtmlFormatter(); }
}
```

**Interview Q&A:**

**Q: "When would you use Factory Method over just calling `new`?"**
A: When the exact type to instantiate isn't known at compile time — it depends on configuration, user input, or context. Also when you want to follow OCP: adding new types shouldn't require modifying existing creation code. And for testability — factory methods can be overridden in test subclasses to return mocks.

**Q: "Give a real example of Factory Method in Java standard library."**
A: `Collection.iterator()` — each collection type (ArrayList, HashSet, TreeMap) overrides the factory method to return its specific iterator implementation. The client code (`for (Item i : collection)`) never knows which iterator type it's using.

**Q: "How does Spring use Factory Method?"**
A: `BeanFactory.getBean()` is a factory method. `FactoryBean<T>.getObject()` is user-defined Factory Method — you tell Spring HOW to create a complex bean. `@Bean` methods in `@Configuration` classes are factory methods that Spring calls to create beans.

**When to use:**
- Framework/library design — let users extend by subclassing (you provide the skeleton, they provide the product creation)
- When a class can't anticipate which type to create (decided by subclass/configuration)
- When you want to localize "which class gets created" knowledge in one place
- When testing requires substitutable creation (override factory in test to return mock)

**When to avoid:**
- Simple creation with no variation — just use `new`
- Only one product type — factory with one option is pointless overhead
- When it leads to parallel class hierarchies that are expensive to maintain

**Real-world examples:**
- Java: `Collection.iterator()`, `NumberFormat.getInstance()`, `Calendar.getInstance()`
- SLF4J: `LoggerFactory.getLogger()` — returns Log4j, Logback, etc. based on classpath
- Spring: `BeanFactory`, `@Bean` methods, `FactoryBean<T>`
- JDBC: `Connection.createStatement()` — each driver returns its own Statement implementation

<details>
<summary><strong>Code Example — Factory Method (Burger Restaurant — from Low-Level-Design-Course/Lecture 09)</strong></summary>

```java
// Product Interface and subclasses
interface Burger {
    void prepare();
}

class BasicBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Basic Burger with bun, patty, and ketchup!");
    }
}

class StandardBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Standard Burger with bun, patty, cheese, and lettuce!");
    }
}

class PremiumBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Premium Burger with gourmet bun, premium patty, cheese, lettuce, and secret sauce!");
    }
}

class BasicWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Basic Wheat Burger with bun, patty, and ketchup!");
    }
}

class StandardWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Standard Wheat Burger with bun, patty, cheese, and lettuce!");
    }
}

class PremiumWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Premium Wheat Burger with gourmet bun, premium patty, cheese, lettuce, and secret sauce!");
    }
}

// Factory Interface and Concrete Factories
// Each factory subclass DECIDES which family of burgers to create — OCP satisfied
interface BurgerFactory {
    Burger createBurger(String type);
}

class SinghBurger implements BurgerFactory {
    public Burger createBurger(String type) {
        if (type.equalsIgnoreCase("basic")) {
            return new BasicBurger();
        } else if (type.equalsIgnoreCase("standard")) {
            return new StandardBurger();
        } else if (type.equalsIgnoreCase("premium")) {
            return new PremiumBurger();
        } else {
            System.out.println("Invalid burger type!");
            return null;
        }
    }
}

class KingBurger implements BurgerFactory {
    public Burger createBurger(String type) {
        if (type.equalsIgnoreCase("basic")) {
            return new BasicWheatBurger();
        } else if (type.equalsIgnoreCase("standard")) {
            return new StandardWheatBurger();
        } else if (type.equalsIgnoreCase("premium")) {
            return new PremiumWheatBurger();
        } else {
            System.out.println("Invalid burger type!");
            return null;
        }
    }
}

// Client code — works with factory interface, doesn't know concrete types
public class FactoryMethod {
    public static void main(String[] args) {
        String type = "basic";

        BurgerFactory myFactory = new SinghBurger(); // Can swap to KingBurger without changing client
        Burger burger = myFactory.createBurger(type);

        if (burger != null) {
            burger.prepare();
        }
    }
}
```

</details>

---

### Abstract Factory

**Definition:** The Abstract Factory pattern provides an interface for creating families of related or dependent objects without specifying their concrete classes. It enforces that products from the same family are used together, ensuring compatibility among created objects.

**In Simple Terms:** Imagine furnishing a room — you pick either a "Modern" catalog or a "Victorian" catalog. Once you pick a catalog, ALL your furniture (sofa, table, chair) comes from that same style. You can't accidentally mix a modern sofa with a Victorian table. Abstract Factory is that catalog — it gives you a complete matching set.

**Intent:** Provide an interface for creating families of related objects without specifying their concrete classes.

**Core Mechanism:**
- Abstract factory interface declares creation methods for EACH product type in the family
- Concrete factories implement ALL methods → guarantees products from the same family work together
- Clients use only the factory interface and product interfaces — fully decoupled from concrete implementations
- The "family" constraint is the key differentiator from independent Factory Methods

**Internal Mechanics:**
```
Client receives AbstractFactory (doesn't know if it's AwsFactory or GcpFactory)
  → client.createStorage() → returns S3Storage or GcsStorage (client sees only Storage interface)
  → client.createQueue()   → returns SqsQueue or PubSubQueue (client sees only Queue interface)
  → Guarantee: S3Storage + SqsQueue are compatible (same AWS family)
```

**SOLID Connection:**
- **OCP:** New families added by creating a new concrete factory — existing code unchanged
- **DIP:** Client depends on AbstractFactory and abstract product interfaces, never concrete classes
- **LSP:** Any concrete factory is substitutable wherever AbstractFactory is expected

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Factory Method** | Factory Method: ONE creation method, ONE product type, inheritance-based. Abstract Factory: MULTIPLE creation methods for a FAMILY of related products, composition-based. |
| **vs. Builder** | Abstract Factory creates a family of distinct products. Builder constructs ONE complex product step-by-step. |
| **vs. Bridge** | Both have two hierarchies. Bridge separates abstraction/implementation of the SAME concept. Abstract Factory creates families of DIFFERENT products that must be compatible. |

**The "Family Constraint" — Why This Pattern Exists:**

Without Abstract Factory (mixing products):
```java
Storage storage = new S3Storage();        // AWS
Queue queue = new PubSubQueue();          // GCP  ← WRONG: incompatible family!
Notification notif = new AzureNotification(); // Azure ← BROKEN: mixed providers
```

With Abstract Factory (family guaranteed):
```java
CloudFactory factory = getFactory("aws"); // Returns AwsFactory
Storage storage = factory.createStorage(); // S3
Queue queue = factory.createQueue();       // SQS
// Guaranteed: all products from same family, tested together, compatible
```

**Interview Q&A:**

**Q: "What's the biggest weakness of Abstract Factory?"**
A: Adding a NEW product type to the family is expensive — you must modify the abstract factory interface AND every concrete factory. For N factories, that's N+1 changes. Design the product set carefully upfront. If products are likely to grow, consider a different approach (config-based selection, service locator).

**Q: "Real-world example in enterprise code?"**
A: Cross-cloud abstraction: `AwsServiceFactory` creates S3Client + SQS + SNS. `GcpServiceFactory` creates GCS + PubSub + FCM. Your application code uses only `CloudServiceFactory` — switching clouds requires changing one line (which factory to inject). Also: Spring profiles achieving similar effect with `@Profile("aws")` on different `@Configuration` classes.

**Q: "When does Abstract Factory emerge naturally?"**
A: When you notice your tests need to swap an entire "stack" (switch from production services to test doubles). If you're mocking 5 related services in every test, they form a family — wrap them in a TestServiceFactory.

**When to use:**
- System must work with multiple product families (cloud providers, UI themes, database vendors)
- Products in a family MUST be used together — mixing is a bug
- You want to enforce consistency at compile time
- Library that hides implementation details, exposing only interfaces

**When to avoid:**
- Only one product family exists (use Factory Method)
- Products from different families can be mixed safely
- Product families unlikely to grow — overhead isn't justified
- When Spring's `@Profile` or conditional beans achieve the same result more simply

**Real-world examples:**
- JDBC: `DriverManager` + `ConnectionFactory` produces Driver-specific Connection/Statement/ResultSet families
- AWS/GCP/Azure SDK abstraction layers
- Java AWT/Swing: `LookAndFeel` produces platform-consistent widgets
- Test infrastructure: `TestServiceFactory` producing mocks vs `ProductionServiceFactory` producing real services

<details>
<summary><strong>Code Example — Abstract Factory (Burger + GarlicBread Meal — from Low-Level-Design-Course/Lecture 09)</strong></summary>

```java
// --- Product 1 --> Burger ---
interface Burger {
    void prepare();
}

class BasicBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Basic Burger with bun, patty, and ketchup!");
    }
}

class StandardBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Standard Burger with bun, patty, cheese, and lettuce!");
    }
}

class PremiumBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Premium Burger with gourmet bun, premium patty, cheese, lettuce, and secret sauce!");
    }
}

class BasicWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Basic Wheat Burger with bun, patty, and ketchup!");
    }
}

class StandardWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Standard Wheat Burger with bun, patty, cheese, and lettuce!");
    }
}

class PremiumWheatBurger implements Burger {
    public void prepare() {
        System.out.println("Preparing Premium Wheat Burger with gourmet bun, premium patty, cheese, lettuce, and secret sauce!");
    }
}

// --- Product 2 --> GarlicBread ---
interface GarlicBread {
    void prepare();
}

class BasicGarlicBread implements GarlicBread {
    public void prepare() {
        System.out.println("Preparing Basic Garlic Bread with butter and garlic!");
    }
}

class CheeseGarlicBread implements GarlicBread {
    public void prepare() {
        System.out.println("Preparing Cheese Garlic Bread with extra cheese and butter!");
    }
}

class BasicWheatGarlicBread implements GarlicBread {
    public void prepare() {
        System.out.println("Preparing Basic Wheat Garlic Bread with butter and garlic!");
    }
}

class CheeseWheatGarlicBread implements GarlicBread {
    public void prepare() {
        System.out.println("Preparing Cheese Wheat Garlic Bread with extra cheese and butter!");
    }
}

// --- Abstract Factory --- creates FAMILY of related products (burger + garlic bread)
interface MealFactory {
    Burger createBurger(String type);
    GarlicBread createGarlicBread(String type);
}

// --- Concrete Factory 1: SinghBurger (regular buns) ---
class SinghBurger implements MealFactory {
    public Burger createBurger(String type) {
        if (type.equalsIgnoreCase("basic")) return new BasicBurger();
        else if (type.equalsIgnoreCase("standard")) return new StandardBurger();
        else if (type.equalsIgnoreCase("premium")) return new PremiumBurger();
        else { System.out.println("Invalid burger type!"); return null; }
    }

    public GarlicBread createGarlicBread(String type) {
        if (type.equalsIgnoreCase("basic")) return new BasicGarlicBread();
        else if (type.equalsIgnoreCase("cheese")) return new CheeseGarlicBread();
        else { System.out.println("Invalid Garlic bread type!"); return null; }
    }
}

// --- Concrete Factory 2: KingBurger (wheat buns) ---
class KingBurger implements MealFactory {
    public Burger createBurger(String type) {
        if (type.equalsIgnoreCase("basic")) return new BasicWheatBurger();
        else if (type.equalsIgnoreCase("standard")) return new StandardWheatBurger();
        else if (type.equalsIgnoreCase("premium")) return new PremiumWheatBurger();
        else { System.out.println("Invalid burger type!"); return null; }
    }

    public GarlicBread createGarlicBread(String type) {
        if (type.equalsIgnoreCase("basic")) return new BasicWheatGarlicBread();
        else if (type.equalsIgnoreCase("cheese")) return new CheeseWheatGarlicBread();
        else { System.out.println("Invalid Garlic bread type!"); return null; }
    }
}

// Client: uses the abstract factory — guaranteed consistent family
public class AbstractFactory {
    public static void main(String[] args) {
        String burgerType = "basic";
        String garlicBreadType = "cheese";

        // Switching family = changing ONE line (SinghBurger → KingBurger)
        MealFactory mealFactory = new SinghBurger();

        Burger burger = mealFactory.createBurger(burgerType);
        GarlicBread garlicBread = mealFactory.createGarlicBread(garlicBreadType);

        if (burger != null) burger.prepare();
        if (garlicBread != null) garlicBread.prepare();
    }
}
```

</details>

---

### Builder

**Definition:** The Builder pattern separates the construction of a complex object from its representation, allowing the same construction process to create various representations. It provides a step-by-step approach to building composite objects, isolating the construction logic from the final product.

**In Simple Terms:** Think of ordering a custom burger. You don’t shout all 10 ingredients at once — you go step by step: "add lettuce, add cheese, no onions, extra sauce." At the end you say "done" and get your custom burger. Builder lets you construct complex objects one piece at a time, and only finalize when you’re ready.

**Intent:** Separate the construction of a complex object from its representation so that the same construction process can create different representations.

**Core Mechanism:**
- Mutable builder accumulates configuration via fluent setters (each returns `this`)
- Final `build()` method validates accumulated state and constructs the immutable product
- Product's constructor is private — only accessible from builder (enforces controlled construction)
- Optional: Director class orchestrates builder steps for predefined configurations

**Internal Mechanics:**
```
Client code:
  new Builder()          → creates mutable accumulator
  .field1(value1)        → stores value, returns this (chaining)
  .field2(value2)        → stores value, returns this
  .build()               → validates all state → calls private Product constructor → returns immutable product

Key insight: Builder is MUTABLE during construction, Product is IMMUTABLE after construction.
This solves the "telescoping constructor" problem (10+ constructor params) AND the
"valid intermediate state" problem (you can't create an incomplete Product).
```

**Types of Builder Pattern:**

There are three distinct variants of the Builder pattern, each solving a slightly different problem:

**1. Normal Builder (Fluent Builder)**

The most common variant. The client chains setter methods in any order and calls `build()` at the end. No external orchestrator — the client decides what to set and in what order.

```java
// Client has full freedom — set any field, any order
HttpRequest request = HttpRequest.newBuilder()
    .uri("https://api.example.com")
    .header("Authorization", "Bearer token")
    .timeout(Duration.ofSeconds(30))
    .build();
```

**When to use:** Objects with many optional parameters, configuration objects, test data factories. This is 90% of builder usage in production.

**2. Director Builder**

Adds a Director class that encapsulates predefined construction recipes. The Director knows WHICH steps to call and in WHAT order — the client just picks a recipe. Useful when the same builder can produce different configurations based on predefined "profiles."

```java
// Director encapsulates construction recipes
class HttpRequestDirector {
    public HttpRequest createAuthenticatedGet(String url, String token) {
        return HttpRequest.newBuilder()
            .uri(url)
            .method("GET")
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .build();
    }

    public HttpRequest createFileUpload(String url, byte[] data) {
        return HttpRequest.newBuilder()
            .uri(url)
            .method("POST")
            .header("Content-Type", "multipart/form-data")
            .body(data)
            .timeout(Duration.ofSeconds(60))
            .build();
    }
}

// Client doesn't need to know which fields to set
HttpRequest req = director.createAuthenticatedGet("/api/users", myToken);
```

**When to use:** When you have reusable construction recipes, when clients shouldn't decide step order, when the same builder needs to produce multiple standard configurations (like document converters: same steps → HTML, PDF, or Markdown output).

**3. Step Builder (Staged Builder / Wizard Builder)**

Enforces a SPECIFIC ORDER of construction steps at compile time using separate interfaces for each step. Each step returns the NEXT step's interface — so the client CANNOT skip steps or call them out of order. This provides the strongest compile-time safety.

```java
// Each step returns the interface for the NEXT step — order enforced by types
interface UrlStep {
    MethodStep url(String url);       // Must set URL first
}
interface MethodStep {
    HeaderStep method(String method); // Then method
}
interface HeaderStep {
    HeaderStep header(String k, String v); // Headers are optional/repeatable
    BuildStep noHeaders();                  // Skip to build
}
interface BuildStep {
    HttpRequest build();              // Only available after required steps
}

// Usage: compiler FORCES this order
HttpRequest req = HttpRequest.stepBuilder()
    .url("https://api.example.com")   // Returns MethodStep
    .method("GET")                     // Returns HeaderStep
    .header("Auth", "Bearer x")        // Returns HeaderStep (chainable)
    .build();                          // Only available after required steps

// COMPILE ERROR: can't call .method() before .url()
// COMPILE ERROR: can't call .build() before .method()
```

**When to use:** When construction has MANDATORY steps in a FIXED ORDER, when skipping steps would be a bug (e.g., database connection: host → port → credentials → connect), when you want compile-time enforcement rather than runtime validation in `build()`.

**Comparison of the Three Types:**

| Aspect | Normal Builder | Director Builder | Step Builder |
|---|---|---|---|
| Order enforced? | ❌ Any order | ✅ Director decides | ✅ Compiler enforces |
| Required fields checked | At runtime (`build()`) | By Director logic | At compile time (can't skip) |
| Client flexibility | Maximum | Pick a recipe | Must follow sequence |
| Complexity | Low | Medium | High (many interfaces) |
| Best for | Optional config | Reusable recipes | Mandatory ordered steps |
| Real example | Lombok `@Builder` | `DocumentBuilder` presets | JOOQ query builder |

**SOLID Connection:**
- **SRP:** Construction logic separated from the product's domain logic
- Builder encapsulates "how to put it together" — product encapsulates "what it does"
- **SRP:** Construction logic separated from the product's domain logic
- Builder encapsulates "how to put it together" — product encapsulates "what it does"

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Factory** | Factory decides WHICH class to instantiate. Builder decides HOW to construct a specific class with many configuration options. |
| **vs. Prototype** | Builder creates from scratch step-by-step. Prototype copies an existing configured instance. |
| **vs. Telescoping Constructor** | Constructor with 10 params: unreadable, error-prone (was that the 6th or 7th param?). Builder: named methods, any order, required/optional distinction. |

**Interview Q&A:**

**Q: "When is Builder overkill?"**
A: When the object has 1-3 mandatory params and no optional configuration. Just use a constructor. Also when Java Records or Kotlin data classes give you what you need (named params + immutability) without builder ceremony.

**Q: "Where should validation live — in setters or in build()?"**
A: In `build()`. Rationale: (1) Intermediate builder state may be temporarily invalid during construction. (2) Cross-field validation (e.g., "if auth is OAuth, then clientId is required") can only be checked when all fields are set. (3) Individual setter validation prevents valid construction orders.

**Q: "How does Lombok's @Builder work?"**
A: Annotation processor generates the builder class at compile time. Same pattern — just eliminates boilerplate. Important: you can customize it with `@Builder.Default` for defaults and `@Singular` for collections.

**Q: "Builder in your production code?"**
A: "At Bentley, we use Builder for our circuit-breaker configuration: `CircuitBreakerConfig.builder().failureThreshold(50).waitDuration(30s).slidingWindowSize(10).build()`. It has 12+ configuration options — most with sensible defaults, some required. Builder makes this readable and validates that required fields are set."

**When to use:**
- Objects with 4+ parameters (especially mix of required/optional)
- Immutable objects with many fields
- DSL-like APIs: query builders, config builders, request builders
- Test data factories: `TestUser.builder().withRole(ADMIN).withAge(25).build()`
- When object construction has validation rules across multiple fields

**When to avoid:**
- Simple objects with 1-3 parameters
- Mutable objects where you can just set properties
- When Java Records / Kotlin data classes suffice

**Real-world examples:**
- `HttpRequest.newBuilder()`, `StringBuilder`, Protobuf message builders
- Lombok `@Builder`, Immutables library
- OkHttp `Request.Builder()`, Retrofit `Retrofit.Builder()`
- Spring's `WebClient.builder()`, `RestClient.builder()`

<details>
<summary><strong>Code Example — Builder (HttpRequest with Director — from Low-Level-Design-Course/Lecture 28)</strong></summary>

```java
import java.util.*;

// Immutable Product — can only be built via the Builder
public class HttpRequest {
    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private String body;
    private int timeout; // in seconds

    // Private constructor — only accessible by the Builder
    HttpRequest() {
        headers = new HashMap<>();
        queryParams = new HashMap<>();
        body = "";
    }

    public void execute() {
        System.out.println("Executing " + method + " request to " + url);
        if (!queryParams.isEmpty()) {
            System.out.println("Query Parameters:");
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                System.out.println("  " + param.getKey() + "=" + param.getValue());
            }
        }
        System.out.println("Headers:");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            System.out.println("  " + header.getKey() + ": " + header.getValue());
        }
        if (body != null && !body.isEmpty()) {
            System.out.println("Body: " + body);
        }
        System.out.println("Timeout: " + timeout + " seconds");
        System.out.println("Request executed successfully!");
    }

    // Nested Builder class — fluent method chaining
    public static class HttpRequestBuilder {
        private HttpRequest req;

        public HttpRequestBuilder() { req = new HttpRequest(); }

        public HttpRequestBuilder withUrl(String u) { req.url = u; return this; }
        public HttpRequestBuilder withMethod(String method) { req.method = method; return this; }
        public HttpRequestBuilder withHeader(String key, String value) {
            req.headers.put(key, value); return this;
        }
        public HttpRequestBuilder withQueryParams(String key, String value) {
            req.queryParams.put(key, value); return this;
        }
        public HttpRequestBuilder withBody(String body) { req.body = body; return this; }
        public HttpRequestBuilder withTimeout(int timeout) { req.timeout = timeout; return this; }

        public HttpRequest build() {
            if (req.url == null || req.url.isEmpty()) {
                throw new RuntimeException("URL cannot be empty");
            }
            return req;
        }
    }
}

// Director — encapsulates predefined construction recipes
public class HttpRequestDirector {
    public static HttpRequest createGetRequest(String url) {
        return new HttpRequest.HttpRequestBuilder()
                .withUrl(url)
                .withMethod("GET")
                .build();
    }

    public static HttpRequest createJsonPostRequest(String url, String jsonBody) {
        return new HttpRequest.HttpRequestBuilder()
            .withUrl(url)
            .withMethod("POST")
            .withHeader("Content-Type", "application/json")
            .withHeader("Accept", "application/json")
            .withBody(jsonBody)
            .build();
    }
}

// Client usage
public class Main {
    public static void main(String[] args) {
        // Direct builder usage — full control
        HttpRequest normalRequest = new HttpRequest.HttpRequestBuilder()
            .withUrl("https://api.example.com")
            .withMethod("POST")
            .withHeader("Content-Type", "application/json")
            .withHeader("Accept", "application/json")
            .withQueryParams("key", "12345")
            .withBody("{\"name\": \"Aditya\"}")
            .withTimeout(60)
            .build();
        normalRequest.execute();

        // Via Director — pre-configured templates
        HttpRequest getRequest = HttpRequestDirector.createGetRequest("https://api.example.com/users");
        getRequest.execute();

        HttpRequest postRequest = HttpRequestDirector.createJsonPostRequest(
            "https://api.example.com/users",
            "{\"name\": \"Aditya\", \"email\": \"aditya@example.com\"}");
        postRequest.execute();
    }
}
```

</details>

---

### Prototype

**Definition:** The Prototype pattern specifies the kinds of objects to create using a prototypical instance, and creates new objects by copying this prototype. It avoids the cost of creating objects from scratch when a similar object already exists.

**In Simple Terms:** Instead of filling out a job application form from scratch every time, you photocopy a filled-out template and just change the details that differ. Prototype works the same way — clone an existing fully-configured object and tweak what’s different, rather than building from nothing.

**Intent:** Create new objects by copying existing ones rather than constructing from scratch.

**Core Mechanism:**
- Objects implement a `clone()`/`copy()` method
- Client requests a copy without knowing the concrete class
- Optional: Prototype Registry holds pre-configured templates by name/key
- On request: clone the appropriate prototype, customize the copy if needed

**Internal Mechanics:**
```
1. Registry holds: {"orc" → preconfigured Orc instance, "dragon" → preconfigured Dragon instance}
2. Client: registry.spawn("orc")
3. Registry: finds prototype, calls prototype.clone()
4. clone() creates a new object with same state (shallow or deep copy)
5. Client gets fresh copy — can customize without affecting the prototype
```

**Deep vs. Shallow Copy — The Critical Decision:**

| Type | What it copies | Nested objects | When to use |
|---|---|---|---|
| **Shallow** | Primitive fields copied. Reference fields share same objects. | Shared (mutations affect both copies) | When nested objects are immutable or shared state is intentional |
| **Deep** | Everything recursively duplicated. Completely independent copy. | Independent | When you need full isolation between copies |

```java
// Shallow: orc1.inventory == orc2.inventory (same list!)
// Deep: orc1.inventory and orc2.inventory are independent lists

// Java's clone() is SHALLOW by default. Deep copy requires:
// 1. Manual: recursively clone nested objects
// 2. Serialization: serialize → deserialize (heavy but reliable)
// 3. Copy constructor: explicit deep copy logic
```

**SOLID Connection:** Minimal direct SOLID connection. Prototype is primarily about efficiency and decoupling from concrete classes.

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Factory Method** | Factory creates new instances from scratch. Prototype copies existing configured instances. Use Prototype when creation is expensive or initial state matters. |
| **vs. Builder** | Builder constructs step-by-step (knows how to build from nothing). Prototype duplicates (starts from existing complete object). |
| **vs. Singleton** | Singleton ensures one instance. Prototype creates many instances (copies). |
| **vs. Flyweight** | Flyweight shares one instance across contexts (read-only). Prototype gives each client their own copy (can modify independently). |

**Interview Q&A:**

**Q: "When is Prototype better than Factory?"**
A: When object creation is expensive (e.g., requires DB query, network call, or complex computation to determine initial state). Clone is cheap — just memory copy. Also when you need objects with specific initial configurations that are tedious to recreate via constructors. Game development (enemy templates, level prefabs) is the classic case.

**Q: "How does JavaScript's prototype chain differ from GoF Prototype?"**
A: JS prototype chain is about INHERITANCE (objects delegate property lookups to their prototype). GoF Prototype is about CLONING (creating independent copies). Despite sharing the name, they solve different problems. JS `Object.create(proto)` creates a new object that INHERITS from proto (shared, not copied).

**Q: "How would you implement deep copy in Java?"**
A: Three options: (1) Manual `clone()` overriding with recursive deep copy of each field. (2) Serialize → deserialize (implements Serializable, then `ObjectOutputStream` → `ObjectInputStream`). (3) Copy constructor that explicitly deep-copies everything. Option 2 is most reliable but slowest. Option 3 is clearest and preferred in production.

**When to use:**
- Object creation is expensive (configuration, DB load, computation)
- Need many objects with similar/identical initial state
- Runtime type isn't known — can only work with the base interface
- Game development: prefabs, templates, asset instancing

**When to avoid:**
- Objects are cheap to create from scratch
- Deep vs. shallow copy semantics are unclear (circular references!)
- Objects hold non-cloneable resources (file handles, DB connections)

<details>
<summary><strong>Code Example — Prototype (Document Template Registry)</strong></summary>

```java
import java.util.*;

// Prototype interface
interface DocumentPrototype {
    DocumentPrototype deepClone();
    void customize(String title);
    void print();
}

// Concrete prototype — complex object that's expensive to setup from scratch
class Report implements DocumentPrototype {
    private String title;
    private List<String> sections;
    private Map<String, String> metadata;
    
    public Report(String title, List<String> sections, Map<String, String> metadata) {
        this.title = title;
        this.sections = new ArrayList<>(sections);
        this.metadata = new HashMap<>(metadata);
        // Imagine: expensive template loading, formatting setup, etc.
        System.out.println("  [Heavy initialization for: " + title + "]");
    }
    
    // Deep clone — independent copy
    @Override
    public Report deepClone() {
        return new Report(this.title, new ArrayList<>(this.sections), new HashMap<>(this.metadata));
    }
    
    @Override
    public void customize(String newTitle) { this.title = newTitle; }
    
    @Override
    public void print() { 
        System.out.println("Report: " + title + " | Sections: " + sections);
    }
}

// Prototype Registry
class DocumentRegistry {
    private Map<String, DocumentPrototype> templates = new HashMap<>();
    
    public void register(String key, DocumentPrototype prototype) {
        templates.put(key, prototype);
    }
    
    public DocumentPrototype create(String key) {
        DocumentPrototype proto = templates.get(key);
        if (proto == null) throw new IllegalArgumentException("Unknown template: " + key);
        return proto.deepClone(); // Clone, not reuse
    }
}

public class PrototypeDemo {
    public static void main(String[] args) {
        // Setup registry once (expensive initialization happens here)
        DocumentRegistry registry = new DocumentRegistry();
        registry.register("quarterly-report", new Report(
            "Q Report Template",
            Arrays.asList("Summary", "Metrics", "Forecast"),
            Map.of("format", "PDF", "department", "Engineering")));
        
        // Create instances via cloning (cheap)
        DocumentPrototype q1 = registry.create("quarterly-report");
        q1.customize("Q1 2026 Engineering Report");
        
        DocumentPrototype q2 = registry.create("quarterly-report");
        q2.customize("Q2 2026 Engineering Report");
        
        q1.print(); // Q1 2026 Engineering Report
        q2.print(); // Q2 2026 Engineering Report (independent)
    }
}
```

</details>

---

## Part 2: Structural Patterns

> Structural patterns deal with composing classes and objects to form larger structures while keeping them flexible and efficient.

---

### Adapter

**Definition:** The Adapter pattern converts the interface of a class into another interface that clients expect. It allows classes with incompatible interfaces to collaborate by wrapping one object’s interface to match what another object expects.

**In Simple Terms:** When you travel abroad, your laptop charger doesn’t fit the wall socket. You use a power adapter — it doesn’t change how your charger works or how the socket works, it just makes them compatible. The Adapter pattern is that plug converter for code.

**Intent:** Convert the interface of a class into another interface clients expect. Makes incompatible interfaces work together.

**Core Mechanism:**
- Target interface: what the client expects
- Adaptee: existing class with incompatible interface
- Adapter: wraps the adaptee, implements the target, translates calls
- Client → calls Adapter (Target interface) → Adapter translates → calls Adaptee

**Internal Mechanics:**
```
Without Adapter:
  Client expects: paymentGateway.charge(amount, currency)
  Stripe offers:  stripeClient.createCharge(amountInCents, cur, idempotencyKey)
  → Incompatible! Client can't use Stripe directly.

With Adapter:
  Client → StripeAdapter.charge(49.99, "USD")
    → adapter converts: 49.99 → 4999 cents, generates idempotency key
    → adapter calls: stripeClient.createCharge(4999, "USD", "uuid-123")
  Client is happy. Stripe is happy. They never knew about each other.
```

**Class Adapter vs. Object Adapter:**

| Type | Mechanism | Pros | Cons |
|---|---|---|---|
| Class Adapter | Multiple inheritance (adapter extends BOTH target + adaptee) | No extra object, can override adaptee methods | Requires multiple inheritance (not available in Java/C#), tight coupling |
| **Object Adapter** (preferred) | Composition (adapter HAS-A adaptee reference) | Works everywhere, loose coupling, can adapt subclasses too | One more object in memory |

**SOLID Connection:**
- **DIP (primary):** Your domain defines the interface it needs (high-level policy). The adapter makes third-party code (low-level detail) conform to it. Dependencies point INWARD.
- **OCP:** New adapters can be added without modifying existing code. Switch from Stripe to PayPal? New adapter, same interface.
- **SRP:** Translation logic isolated in the adapter class, not spread through business code.

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Facade** | Adapter wraps ONE class to change its interface. Facade wraps a SUBSYSTEM (many classes) to simplify it. Adapter preserves functionality; Facade intentionally reduces it. |
| **vs. Bridge** | Adapter makes two EXISTING incompatible things work together (retrofit). Bridge is designed upfront to separate two hierarchies. Adapter is reactive; Bridge is proactive. |
| **vs. Proxy** | Adapter changes the interface. Proxy keeps the SAME interface but controls access. |
| **vs. Decorator** | Adapter changes the interface (adaptee.foo → target.bar). Decorator keeps the SAME interface and adds behavior. |

**Interview Q&A:**

**Q: "How does Adapter relate to Dependency Inversion Principle?"**
A: My domain defines `PaymentGateway` interface (high-level policy). Stripe's SDK is a third-party detail. The `StripePaymentAdapter` implements my interface by wrapping Stripe's API. My domain never depends on Stripe directly — dependencies point toward the domain. If I switch to Adyen, I write `AdyenPaymentAdapter` and nothing in my domain changes.

**Q: "When do you create an adapter vs. directly using the third-party API?"**
A: Adapter when: (1) I might switch providers (payment gateways, cloud SDKs). (2) I want to test without hitting the real service (mock my interface, not their SDK). (3) Their API is verbose/awkward and I want a clean domain-aligned API. Don't adapt when: single integration you'll never change and mocking isn't needed.

**Q: "Example of Adapter in Spring?"**
A: `HandlerAdapter` in Spring MVC. DispatcherServlet works with the `HandlerAdapter` interface. There are adapters for `@Controller` methods, `HttpRequestHandler`, plain `Controller` interface, etc. Each adapts a different handler type to the same dispatch mechanism.

**When to use:**
- Integrating third-party libraries/SDKs
- Wrapping legacy code behind a modern interface
- Making independently designed classes work together
- Infrastructure boundary: your domain interface ↔ external system

**When to avoid:**
- Interfaces are already compatible
- You control both sides — just refactor the interface
- The adaptee changes frequently (adapter becomes maintenance burden)

<details>
<summary><strong>Code Example — Adapter (XML to JSON Report — from Low-Level-Design-Course/Lecture 16)</strong></summary>

```java
// 1. Target interface expected by the client
interface IReports {
    String getJsonData(String data);
}

// 2. Adaptee: provides XML data from a raw input (3rd-party / legacy)
class XmlDataProvider {
    // Expect data in "name:id" format (e.g. "Alice:42")
    String getXmlData(String data) {
        int sep = data.indexOf(':');
        String name = data.substring(0, sep);
        String id   = data.substring(sep + 1);
        return "<user>"
                + "<name>" + name + "</name>"
                + "<id>"   + id   + "</id>"
                + "</user>";
    }
}

// 3. Adapter: implements target IReports by converting XML → JSON
class XmlDataProviderAdapter implements IReports {
    private XmlDataProvider xmlProvider;

    public XmlDataProviderAdapter(XmlDataProvider provider) {
        this.xmlProvider = provider;
    }

    public String getJsonData(String data) {
        // Get XML from the adaptee
        String xml = xmlProvider.getXmlData(data);

        // Parse out <name> and <id> values
        int startName = xml.indexOf("<name>") + 6;
        int endName   = xml.indexOf("</name>");
        String name   = xml.substring(startName, endName);

        int startId = xml.indexOf("<id>") + 4;
        int endId   = xml.indexOf("</id>");
        String id   = xml.substring(startId, endId);

        // Build and return JSON
        return "{\"name\":\"" + name + "\", \"id\":" + id + "}";
    }
}

// 4. Client code works only with IReports (target interface)
class Client {
    public void getReport(IReports report, String rawData) {
        System.out.println("Processed JSON: " + report.getJsonData(rawData));
    }
}

public class AdapterPattern {
    public static void main(String[] args) {
        // Create the adaptee (legacy XML provider)
        XmlDataProvider xmlProv = new XmlDataProvider();

        // Wrap in adapter so client can use it as JSON
        IReports adapter = new XmlDataProviderAdapter(xmlProv);

        // Client doesn't know about XML — only sees IReports
        Client client = new Client();
        client.getReport(adapter, "Alice:42");
        // → Processed JSON: {"name":"Alice", "id":42}
    }
}
```

</details>

---

### Bridge

**Definition:** The Bridge pattern decouples an abstraction from its implementation so that the two can vary independently. It splits a large class or a set of closely related classes into two separate hierarchies — abstraction and implementation — which can be developed independently of each other.

**In Simple Terms:** Think of a TV remote (abstraction) and the TV itself (implementation). Any remote can work with any TV — a basic remote or a smart remote can control a Sony or Samsung TV. You can change the remote without changing the TV, and vice versa. Bridge separates the "what" from the "how" so both sides can evolve freely.

**Intent:** Decouple an abstraction from its implementation so that the two can vary independently.

**Core Mechanism:**
- Split one monolithic hierarchy into TWO separate hierarchies:
  - **Abstraction** (what the client uses — the "what")
  - **Implementation** (how it's done — the "how")
- Connect them via COMPOSITION (abstraction holds reference to implementation)
- Both hierarchies can evolve independently

**Internal Mechanics — The Class Explosion Problem:**

Without Bridge (M × N classes):
```
Shapes × Renderers = class explosion:
  CircleOpenGL, CircleDirectX, CircleSVG,
  SquareOpenGL, SquareDirectX, SquareSVG,
  TriangleOpenGL, TriangleDirectX, TriangleSVG
  = 9 classes (3 shapes × 3 renderers)
  
Adding 1 shape: +3 classes. Adding 1 renderer: +3 classes.
```

With Bridge (M + N classes):
```
Shapes: Circle, Square, Triangle (each holds reference to Renderer)
Renderers: OpenGLRenderer, DirectXRenderer, SVGRenderer

= 6 classes (3 + 3)
Adding 1 shape: +1 class. Adding 1 renderer: +1 class.
```

**SOLID Connection:**
- **OCP:** Both hierarchies extend independently without modifying existing code
- **SRP:** Abstraction handles domain logic. Implementation handles platform/mechanism details.
- **DIP:** Abstraction depends on implementation interface, not concrete implementation

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Strategy** | Structurally identical (context delegates to an object). Bridge: BOTH sides have hierarchies. Strategy: only the "how" varies; context is stable. Bridge is about decoupling two evolving dimensions. Strategy is about swapping algorithms. |
| **vs. Adapter** | Adapter retrofits incompatible interfaces AFTER they exist. Bridge is designed UPFRONT to separate two concerns. Adapter = reactive fix. Bridge = proactive design. |
| **vs. Abstract Factory** | Abstract Factory creates families of products. Bridge separates abstraction from implementation of ONE concept. They sometimes work together (factory selects the implementation for the bridge). |

**Interview Q&A:**

**Q: "Give a real example of Bridge pattern."**
A: JDBC is the canonical example. The `java.sql` interfaces (Connection, Statement, ResultSet) are the abstraction. Each database driver (MySQL Connector, PostgreSQL JDBC, Oracle) is the implementation. Your application code uses the abstraction; the driver provides the implementation. Both evolve independently — new SQL features in the interface, new database versions in drivers.

**Q: "How do you decide between Bridge and Strategy?"**
A: Ask: "Do BOTH sides have multiple variants that evolve independently?" If yes → Bridge. "Does only the algorithm/behavior vary while the context is stable?" If yes → Strategy. Example: Notifications (urgent/reminder) × Channels (email/SMS/push) → both sides vary → Bridge. Sorting with different comparators → only algorithm varies → Strategy.

**Q: "When is Bridge premature?"**
A: When you have only ONE implementation. Bridge is justified when you can show at least 2 variants on each dimension. "What if we need another renderer someday?" is speculation. Wait until the second dimension actually emerges, then refactor.

**When to use:**
- Two orthogonal dimensions of variation (platform × feature, channel × message type)
- Want to switch implementation at runtime
- M × N class explosion from combining variations via inheritance
- Cross-platform code where the "what" (domain logic) and "how" (platform specifics) must be decoupled

**When to avoid:**
- Only one dimension varies (use Strategy)
- The two dimensions are tightly coupled and change together
- Only one implementation exists on one side (premature abstraction)

<details>
<summary><strong>Code Example — Bridge (Car × Engine — from Low-Level-Design-Course/Lecture 25)</strong></summary>

```java
// Implementation Hierarchy: Engine interface (the "HOW")
interface Engine {
    void start();
}

// Concrete Implementors
class PetrolEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Petrol engine starting with ignition!");
    }
}

class DieselEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Diesel engine roaring to life!");
    }
}

class ElectricEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Electric engine powering up silently!");
    }
}

// Abstraction Hierarchy: Car (the "WHAT")
abstract class Car {
    protected Engine engine; // Bridge to implementation

    public Car(Engine e) {
        this.engine = e;
    }

    public abstract void drive();
}

// Refined Abstraction: Sedan
class Sedan extends Car {
    public Sedan(Engine e) { super(e); }

    @Override
    public void drive() {
        engine.start();
        System.out.println("Driving a Sedan on the highway.");
    }
}

// Refined Abstraction: SUV
class SUV extends Car {
    public SUV(Engine e) { super(e); }

    @Override
    public void drive() {
        engine.start();
        System.out.println("Driving an SUV off-road.");
    }
}

// Without Bridge: Sedan × Engine = SedanPetrol, SedanDiesel, SedanElectric,
//                 SUV × Engine = SUVPetrol, SUVDiesel, SUVElectric = 6 classes
// With Bridge: 2 Cars + 3 Engines = 5 classes. M + N instead of M × N.
public class BridgePattern {
    public static void main(String[] args) {
        Engine petrolEng = new PetrolEngine();
        Engine dieselEng = new DieselEngine();
        Engine electricEng = new ElectricEngine();

        Car mySedan = new Sedan(petrolEng);     // Petrol + Sedan
        Car mySUV = new SUV(electricEng);       // Electric + SUV
        Car yourSUV = new SUV(dieselEng);       // Diesel + SUV

        mySedan.drive();
        mySUV.drive();
        yourSUV.drive();
    }
}
```

</details>

---

### Decorator

**Definition:** The Decorator pattern attaches additional responsibilities to an object dynamically. It provides a flexible alternative to subclassing for extending functionality by wrapping the original object with decorator objects that add behavior before or after delegating to the wrapped component.

**In Simple Terms:** Think of a plain coffee. You can add milk (one decorator), add sugar (another decorator), add whipped cream (yet another). Each addition wraps the previous one, and you can stack them in any combination. You never modify the original coffee — you just keep wrapping it with extras.

**Intent:** Attach additional responsibilities to an object dynamically. Flexible alternative to subclassing for extending functionality.

**Core Mechanism:**
- Decorator implements the SAME interface as the component it wraps
- Holds a reference to the wrapped component (HAS-A)
- Delegates all calls to the wrapped component + adds behavior before/after
- Decorators can be stacked: `D3(D2(D1(Component)))` — each adds one layer

**Internal Mechanics — The Onion Model:**
```
Call chain: client → Decorator3 → Decorator2 → Decorator1 → RealComponent

LoggingDecorator.execute(request)
  → log("entering")
  → CachingDecorator.execute(request)      [delegated to next layer]
    → check cache → miss
    → RetryDecorator.execute(request)       [delegated to next layer]
      → try:
        → RealService.execute(request)      [actual work]
        → return result
      → catch: retry 2 more times
    → store in cache
    → return cached result
  → log("exiting, took Xms")
  → return result
```

Each decorator adds ONE responsibility. Composed freely. Order matters (logging outside retry ≠ logging inside retry).

**SOLID Connection:**
- **OCP (primary):** Add new behavior by creating new decorator class — never modify existing classes. `RetryDecorator`, `LoggingDecorator`, `CachingDecorator` — each is a new class, not a modification.
- **SRP:** Each decorator has one job. `LoggingDecorator` only logs. `CachingDecorator` only caches. Compare to a monolithic service with logging+caching+retry baked in.
- **LSP:** Decorators are substitutable for the component (same interface).

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Proxy** | Same structure. Different INTENT. Decorator ADDS behavior (enrichment). Proxy CONTROLS ACCESS (lazy load, auth check, rate limit). Decorator is transparent enhancement. Proxy may prevent the call entirely. |
| **vs. Chain of Responsibility** | Similar pipeline structure. Decorator wraps the same component (onion layers). CoR passes to the NEXT handler in a chain (linear). Decorator always delegates to the same wrapped object. |
| **vs. Inheritance** | Inheritance: behavior added at compile time, applies to ALL instances of the class. Decorator: behavior added at runtime, applies to THIS specific instance. Decorator composes freely; inheritance is fixed. |
| **vs. Strategy** | Strategy swaps the entire algorithm. Decorator AUGMENTS existing behavior without replacing it. |

**Interview Q&A:**

**Q: "Why does Java I/O use Decorator instead of inheritance?"**
A: Because features (buffering, encoding, compression, encryption) need to be combined freely. With inheritance: `BufferedCompressedEncryptedFileInputStream` — one class per combination = exponential class explosion. With Decorator: `new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)))` — compose exactly what you need. M features = M classes, not 2^M.

**Q: "How does Spring use Decorator pattern?"**
A: Spring's `@Transactional`, `@Cacheable`, `@Retryable` are decorator-like (via AOP proxy). The proxy wraps your bean and adds behavior before/after method calls. Under the hood it's a Decorator — same interface, delegation + added behavior. Also: `HttpServletRequestWrapper` — decorates the request to add custom attributes/behavior.

**Q: "What happens when decorator order matters?"**
A: `Retry(Cache(Service))` — retries the cached result (wrong: retries won't help if cache returns stale data). `Cache(Retry(Service))` — caches the retried result (correct: cache stores the successfully retried response). In production, I define the composition order in configuration and document WHY that order was chosen.

**Q: "When does Decorator become a problem?"**
A: (1) Debugging: stack traces show 5 layers of decorators — hard to find the real code. (2) Identity: `decoratedObj != originalObj` breaks equals/identity checks. (3) Large interfaces: if the interface has 20 methods, each decorator must delegate all 20, even if it only adds behavior to 1.

**When to use:**
- Adding cross-cutting concerns: logging, caching, retry, metrics, auth
- When combinations of features are needed (not all features for all instances)
- Middleware pipelines
- When subclassing is impossible (final class) or impractical (too many combinations)

**When to avoid:**
- When order of decorators matters and is hard to enforce
- When identity comparison is needed
- When the component interface is very large (lots of passthrough delegation)
- When a single subclass suffices (one enhancement, always applied)

<details>
<summary><strong>Code Example — Decorator (Mario Power-Ups — from Low-Level-Design-Course/Lecture 13)</strong></summary>

```java
// Component Interface: defines a common interface for Mario and all power-up decorators.
interface Character {
    String getAbilities();
}

// Concrete Component: Basic Mario character with no power-ups.
class Mario implements Character {
    public String getAbilities() {
        return "Mario";
    }
}

// Abstract Decorator: CharacterDecorator "is-a" Character and "has-a" Character.
abstract class CharacterDecorator implements Character {
    protected Character character;  // Wrapped component

    public CharacterDecorator(Character c) {
        this.character = c;
    }
}

// Concrete Decorator: Height-Increasing Power-Up.
class HeightUp extends CharacterDecorator {
    public HeightUp(Character c) { super(c); }

    public String getAbilities() {
        return character.getAbilities() + " with HeightUp";
    }
}

// Concrete Decorator: Gun Shooting Power-Up.
class GunPowerUp extends CharacterDecorator {
    public GunPowerUp(Character c) { super(c); }

    public String getAbilities() {
        return character.getAbilities() + " with Gun";
    }
}

// Concrete Decorator: Star Power-Up (temporary ability).
class StarPowerUp extends CharacterDecorator {
    public StarPowerUp(Character c) { super(c); }

    public String getAbilities() {
        return character.getAbilities() + " with Star Power (Limited Time)";
    }
}

// Decorators can be STACKED freely — each adds ONE responsibility
public class DecoratorPattern {
    public static void main(String[] args) {
        // Create a basic Mario character.
        Character mario = new Mario();
        System.out.println("Basic Character: " + mario.getAbilities());

        // Decorate Mario with a HeightUp power-up.
        mario = new HeightUp(mario);
        System.out.println("After HeightUp: " + mario.getAbilities());

        // Decorate Mario further with a GunPowerUp.
        mario = new GunPowerUp(mario);
        System.out.println("After GunPowerUp: " + mario.getAbilities());

        // Finally, add a StarPowerUp decoration.
        mario = new StarPowerUp(mario);
        System.out.println("After StarPowerUp: " + mario.getAbilities());
        // Output: Mario with HeightUp with Gun with Star Power (Limited Time)
    }
}
```

</details>

---

### Proxy

**Definition:** The Proxy pattern provides a surrogate or placeholder for another object to control access to it. The proxy object acts as an intermediary, adding a level of indirection to support controlled, lazy, remote, or protected access to the real subject.

**In Simple Terms:** Think of a security guard at a building entrance. The guard isn’t the building, but you have to go through them to get in. They might check your ID (protection proxy), call ahead to see if someone’s available (virtual proxy), or represent a building in another city (remote proxy). The proxy controls your access to the real thing.

**Intent:** Provide a surrogate or placeholder for another object to control access to it.

**Core Mechanism:**
- Proxy implements the SAME interface as the real subject
- Client interacts with Proxy thinking it's the real thing
- Proxy decides WHEN/WHETHER/HOW to delegate to the real subject
- Unlike Decorator (which always delegates and adds), Proxy may never delegate (caching proxy), delay delegation (virtual proxy), or reject delegation (protection proxy)

**Types of Proxy:**

| Type | What it controls | Example |
|---|---|---|
| **Virtual** | Lazy initialization — defers expensive creation until needed | Hibernate entity proxy (loads from DB on first access) |
| **Protection** | Access control — checks permissions before delegating | Spring Security method proxies |
| **Remote** | Location — represents an object in a different address space | gRPC stubs, Java RMI |
| **Caching** | Performance — stores results, avoids repeated computation | `@Cacheable` in Spring |
| **Logging** | Observability — records calls without modifying behavior | AOP logging proxies |
| **Smart Reference** | Lifecycle — counts references, manages disposal | C++ smart pointers |

**SOLID Connection:**
- **SRP:** The proxy handles the cross-cutting concern (caching/auth/logging); the real subject handles business logic.
- **OCP:** Add new proxy types without modifying the real subject.
- **LSP:** Proxy is substitutable for the real subject (same interface).

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Decorator** | Same structure. Different intent. Proxy CONTROLS ACCESS (may block, delay, or shortcut). Decorator ENRICHES BEHAVIOR (always delegates, always adds). Proxy lifecycle: proxy creates/manages the real subject. Decorator lifecycle: client creates both and composes them. |
| **vs. Adapter** | Adapter CHANGES the interface (different API). Proxy PRESERVES the interface (same API, different behavior). |
| **vs. Facade** | Facade simplifies (exposes subset of complex subsystem). Proxy controls (same interface, adds control layer). |

**Spring's Proxy Mechanism — Deep Dive:**
```
When you annotate @Transactional:
1. Spring detects the annotation during BeanPostProcessing
2. Creates a PROXY wrapping your bean (CGLIB subclass or JDK dynamic proxy)
3. Proxy implements same interface as your bean
4. Client injects/calls the PROXY (not your actual bean)
5. On method call:
   - Proxy: begin transaction → delegate to real method → commit/rollback
   - Real bean: just executes business logic (doesn't know about txn)

This is Proxy pattern via bytecode generation (CGLIB) or reflection (JDK Proxy).
```

**Interview Q&A:**

**Q: "How does Spring AOP relate to Proxy pattern?"**
A: Spring AOP creates proxies around your beans. `@Transactional` → TransactionInterceptor proxy. `@Cacheable` → CacheInterceptor proxy. `@Async` → AsyncExecutionInterceptor proxy. Two mechanisms: JDK Dynamic Proxy (interface-based) and CGLIB (subclass-based). The proxy intercepts calls and adds behavior without modifying your code.

**Q: "Why doesn't @Transactional work on self-invocation?"**
A: Because internal calls (`this.method()`) bypass the proxy. The proxy only intercepts EXTERNAL calls (calls from other beans). This is the Proxy pattern's fundamental limitation — the proxy wraps the OBJECT, not individual method calls within it.

**Q: "Virtual Proxy vs. lazy initialization in constructor?"**
A: Virtual Proxy: object appears to exist (same interface), but isn't created until first use. Lazy init in constructor: entire object exists but an expensive field is deferred. Proxy is better when even creating the object shell is expensive, or when the object might never be used at all (JPA relationships that are never accessed).

**When to use:**
- Lazy loading (Hibernate entities, heavy resources)
- Access control (permission checks before method calls)
- Remote objects (gRPC/REST clients appearing as local objects)
- Caching (memoizing expensive computations)
- Logging/metrics without modifying real subject

**When to avoid:**
- When direct access is simpler and adds no benefit
- When proxy adds latency to a hot path
- When the interface is huge (delegation boilerplate)

<details>
<summary><strong>Code Example — Proxy (Virtual + Protection — from Low-Level-Design-Course/Lecture 21)</strong></summary>

```java
// ==================== VIRTUAL PROXY ====================
// Defers expensive creation until first use

interface IImage {
    void display();
}

class RealImage implements IImage {
    private String filename;

    public RealImage(String file) {
        this.filename = file;
        System.out.println("[RealImage] Loading image from disk: " + filename); // Expensive!
    }

    @Override
    public void display() {
        System.out.println("[RealImage] Displaying " + filename);
    }
}

// Proxy holds a reference but doesn't create the real object until needed
class ImageProxy implements IImage {
    private RealImage realImage;
    private String filename;

    public ImageProxy(String file) {
        this.filename = file;
        this.realImage = null; // NOT loaded yet
    }

    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename); // Lazy init on first use
        }
        realImage.display();
    }
}

public class VirtualProxy {
    public static void main(String[] args) {
        IImage image1 = new ImageProxy("sample.jpg");
        // No disk I/O yet — proxy is lightweight
        image1.display(); // NOW loads from disk
    }
}

// ==================== PROTECTION PROXY ====================
// Controls access based on permissions

interface IDocumentReader {
    void unlockPDF(String filePath, String password);
}

class RealDocumentReader implements IDocumentReader {
    @Override
    public void unlockPDF(String filePath, String password) {
        System.out.println("[RealDocumentReader] Unlocking PDF at: " + filePath);
        System.out.println("[RealDocumentReader] PDF unlocked successfully with password: " + password);
        System.out.println("[RealDocumentReader] Displaying PDF content...");
    }
}

class User {
    public String name;
    public boolean premiumMembership;

    public User(String name, boolean isPremium) {
        this.name = name;
        this.premiumMembership = isPremium;
    }
}

// Proxy checks permissions BEFORE delegating to real subject
class DocumentProxy implements IDocumentReader {
    private RealDocumentReader realReader;
    private User user;

    public DocumentProxy(User user) {
        this.realReader = new RealDocumentReader();
        this.user = user;
    }

    @Override
    public void unlockPDF(String filePath, String password) {
        if (!user.premiumMembership) {
            System.out.println("[DocumentProxy] Access denied. Only premium members can unlock PDFs.");
            return; // Proxy BLOCKS the call — doesn't delegate
        }
        realReader.unlockPDF(filePath, password); // Proxy ALLOWS the call
    }
}

public class ProtectionProxy {
    public static void main(String[] args) {
        User user1 = new User("Rohan", false);
        User user2 = new User("Rashmi", true);

        System.out.println("== Rohan (Non-Premium) tries to unlock PDF ==");
        IDocumentReader docReader = new DocumentProxy(user1);
        docReader.unlockPDF("protected_document.pdf", "secret123"); // Access denied

        System.out.println("\n== Rashmi (Premium) unlocks PDF ==");
        docReader = new DocumentProxy(user2);
        docReader.unlockPDF("protected_document.pdf", "secret123"); // Succeeds
    }
}
```

</details>

---

### Composite

**Definition:** The Composite pattern composes objects into tree structures to represent part-whole hierarchies. It lets clients treat individual objects and compositions of objects uniformly through a common interface, enabling recursive structures.

**In Simple Terms:** Think of a file system: a folder can contain files AND other folders. Whether you ask for the size of a single file or an entire folder (which recursively sums its children), you use the same operation. Composite lets you treat a single item and a group of items with the exact same code.

**Intent:** Compose objects into tree structures to represent part-whole hierarchies. Treat individual objects and compositions uniformly.

**Core Mechanism:**
- Component interface shared by BOTH leaf nodes and composite nodes
- Leaf: implements operations directly (base case)
- Composite: holds children (list of Components), implements operations by delegating to children (recursive case)
- Client code uses Component interface — doesn't know if it's a leaf or composite

**SOLID Connection:**
- **OCP:** New leaf/composite types can be added without modifying existing code
- **LSP:** Both leaf and composite satisfy the Component contract — substitutable anywhere a Component is expected

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Decorator** | Both wrap. Composite wraps MULTIPLE children (tree). Decorator wraps ONE component (chain). Composite's purpose: uniform tree structure. Decorator's purpose: add behavior. |
| **vs. Iterator** | Composite defines the STRUCTURE (tree). Iterator defines TRAVERSAL over a structure. Often combined. |

**Interview Q&A:**

**Q: "How would you model a file system?"**
A: Composite. `FileSystemNode` interface with `getSize()`, `getName()`, `display()`. `File` (leaf) returns its own size. `Directory` (composite) sums children's sizes. Client calls `root.getSize()` — works regardless of depth or structure.

**Q: "Where does Composite appear in UI frameworks?"**
A: Android: `View` (leaf) and `ViewGroup` (composite) — both extend `View`. A `LinearLayout` contains other views (including nested layouts). React: component tree — `<App>` contains `<Header>` contains `<Nav>` — rendered recursively.

**When to use:** Part-whole hierarchies, recursive structures, uniform APIs over trees (file systems, UI components, org charts, menu systems, expression trees).

**When to avoid:** Non-hierarchical data. When leaf and composite have very different operations. When type safety between leaf/composite matters more than uniformity.

<details>
<summary><strong>Code Example — Composite (File System — from Low-Level-Design-Course/Lecture 19)</strong></summary>

```java
import java.util.ArrayList;
import java.util.List;

// Base interface for files and folders — UNIFORM interface
interface FileSystemItem {
    void ls(int indent);            // List contents (like terminal ls)
    void openAll(int indent);       // Recursively show everything
    int getSize();                  // Size for leaf, total for composite
    FileSystemItem cd(String name); // Navigate into a subfolder
    String getName();
    boolean isFolder();
}

// Leaf: File — base case
class File implements FileSystemItem {
    private String name;
    private int size;

    public File(String n, int s) { name = n; size = s; }

    @Override public void ls(int indent) {
        System.out.println(" ".repeat(indent) + name);
    }
    @Override public void openAll(int indent) {
        System.out.println(" ".repeat(indent) + name);
    }
    @Override public int getSize() { return size; }
    @Override public FileSystemItem cd(String name) { return null; }
    @Override public String getName() { return name; }
    @Override public boolean isFolder() { return false; }
}

// Composite: Folder — holds children (Files or other Folders)
class Folder implements FileSystemItem {
    private String name;
    private List<FileSystemItem> children;

    public Folder(String n) { name = n; children = new ArrayList<>(); }

    public void add(FileSystemItem item) { children.add(item); }

    @Override
    public void ls(int indent) {
        String spaces = " ".repeat(indent);
        for (FileSystemItem child : children) {
            if (child.isFolder()) {
                System.out.println(spaces + "+ " + child.getName());
            } else {
                System.out.println(spaces + child.getName());
            }
        }
    }

    @Override
    public void openAll(int indent) {
        String spaces = " ".repeat(indent);
        System.out.println(spaces + "+ " + name);
        for (FileSystemItem child : children) {
            child.openAll(indent + 4); // Recursive call — key Composite mechanism
        }
    }

    @Override
    public int getSize() {
        int total = 0;
        for (FileSystemItem child : children) {
            total += child.getSize(); // Recursive aggregation
        }
        return total;
    }

    @Override
    public FileSystemItem cd(String target) {
        for (FileSystemItem child : children) {
            if (child.isFolder() && child.getName().equals(target)) {
                return child;
            }
        }
        return null;
    }

    @Override public String getName() { return name; }
    @Override public boolean isFolder() { return true; }
}

public class CompositePattern {
    public static void main(String[] args) {
        // Build file system tree
        Folder root = new Folder("root");
        root.add(new File("file1.txt", 1));
        root.add(new File("file2.txt", 1));

        Folder docs = new Folder("docs");
        docs.add(new File("resume.pdf", 1));
        docs.add(new File("notes.txt", 1));
        root.add(docs);

        Folder images = new Folder("images");
        images.add(new File("photo.jpg", 1));
        root.add(images);

        // Uniform operations work on BOTH files and folders
        root.ls(0);       // List immediate children
        docs.ls(0);       // List docs folder
        root.openAll(0);  // Recursive traversal

        // Navigate
        FileSystemItem cwd = root.cd("docs");
        if (cwd != null) { cwd.ls(0); }
    }
}
```

</details>

---

### Facade

**Definition:** The Facade pattern provides a unified interface to a set of interfaces in a subsystem. It defines a higher-level interface that makes the subsystem easier to use by shielding clients from the complexity of the subsystem’s components and their interactions.

**In Simple Terms:** When you press "brew" on a coffee machine, you don’t manually grind beans, boil water, measure dosage, and time the extraction. The one-button interface hides 10 steps behind it. Facade is that single simple button over a complicated system — it doesn’t add new features, it just makes existing ones easier to use together.

**Intent:** Provide a simplified interface to a complex subsystem. One entry point hides orchestration complexity.

**Core Mechanism:** Facade delegates to multiple subsystem classes, orchestrating their interactions. Client → Facade → (SubsystemA, SubsystemB, SubsystemC). Facade doesn't add new functionality — it organizes EXISTING subsystem capabilities into convenient workflows.

**SOLID Connection:** Embodies the **Principle of Least Knowledge (Law of Demeter)** — "only talk to your immediate friends." Prevents: `order.getPayment().getGateway().charge()` (reaching deep into objects). Provides: `paymentFacade.charge(orderId, amount)`.

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Adapter** | Adapter: wraps ONE class, changes interface. Facade: wraps MANY classes, simplifies. |
| **vs. Mediator** | Facade: client-facing simplification (unidirectional). Mediator: inter-component coordination (bidirectional). Facade doesn't know about clients; Mediator knows all participants. |

**Interview Q&A:**

**Q: "Example in Spring?"**
A: `JdbcTemplate` — facade over raw JDBC (Connection, Statement, ResultSet, exception handling, resource cleanup). `RestTemplate`/`WebClient` — facade over HTTP connection management, serialization, error handling.

**Q: "When does a Facade become a God Object?"**
A: When it implements logic instead of delegating. A good facade is thin — just orchestrates calls. If your facade has business logic, extract it into domain services. Also: if it has 30+ methods, split into role-based facades (`OrderReadFacade`, `OrderWriteFacade`).

**When to use:** Complex subsystems, layered architectures, library wrappers, reducing coupling between modules.

**When to avoid:** Already-simple subsystems, when clients need fine-grained subsystem access, when facade becomes the god class.

<details>
<summary><strong>Code Example — Facade (Computer Boot-Up — from Low-Level-Design-Course/Lecture 17)</strong></summary>

```java
// Subsystems — complex internals that the client shouldn't deal with directly
class PowerSupply {
    public void providePower() { System.out.println("Power Supply: Providing power..."); }
}

class CoolingSystem {
    public void startFans() { System.out.println("Cooling System: Fans started..."); }
}

class CPU {
    public void initialize() { System.out.println("CPU: Initialization started..."); }
}

class Memory {
    public void selfTest() { System.out.println("Memory: Self-test passed..."); }
}

class HardDrive {
    public void spinUp() { System.out.println("Hard Drive: Spinning up..."); }
}

class BIOS {
    public void boot(CPU cpu, Memory memory) {
        System.out.println("BIOS: Booting CPU and Memory checks...");
        cpu.initialize();
        memory.selfTest();
    }
}

class OperatingSystem {
    public void load() { System.out.println("Operating System: Loading into memory..."); }
}

// Facade — single entry point, orchestrates subsystem interactions
class ComputerFacade {
    private PowerSupply powerSupply = new PowerSupply();
    private CoolingSystem coolingSystem = new CoolingSystem();
    private CPU cpu = new CPU();
    private Memory memory = new Memory();
    private HardDrive hardDrive = new HardDrive();
    private BIOS bios = new BIOS();
    private OperatingSystem os = new OperatingSystem();

    // ONE method replaces knowing 7 subsystems + their ordering
    public void startComputer() {
        System.out.println("----- Starting Computer -----");
        powerSupply.providePower();
        coolingSystem.startFans();
        bios.boot(cpu, memory);
        hardDrive.spinUp();
        os.load();
        System.out.println("Computer Booted Successfully!");
    }
}

// Client — doesn't know about PowerSupply, BIOS, CPU, etc.
public class FacadePattern {
    public static void main(String[] args) {
        ComputerFacade computer = new ComputerFacade();
        computer.startComputer(); // One call does everything
    }
}
```

</details>

---

### Flyweight

**Definition:** The Flyweight pattern uses sharing to support large numbers of fine-grained objects efficiently. It separates intrinsic state (shared, immutable) from extrinsic state (unique, context-dependent), storing shared state in a pool to minimize memory consumption.

**In Simple Terms:** In a word processor, every letter ‘A’ on screen looks different (size, position, color) but they all share the same glyph shape data. Instead of storing the ‘A’ shape 10,000 times, you store it once and reuse it everywhere with different positions. Flyweight is about sharing the common parts to save memory.

**Intent:** Share state efficiently to support large numbers of similar objects without excessive memory.

**Core Mechanism:**
- Split state into **intrinsic** (shared, immutable — stored in flyweight) and **extrinsic** (unique per context — passed by client)
- Flyweight pool/factory ensures same intrinsic state → same object instance
- Client supplies extrinsic state at operation time

**Key insight:** `Integer.valueOf(127)` — Java caches Integer objects for -128 to 127. `valueOf(5) == valueOf(5)` is true (same object). `valueOf(200) == valueOf(200)` is false (different objects, no caching for large values).

**SOLID Connection:** Minimal. Flyweight is primarily a memory optimization technique, not a design improvement.

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Singleton** | Singleton: exactly ONE instance. Flyweight: MANY shared instances (one per unique intrinsic state combination). |
| **vs. Prototype** | Prototype: clone to get independent copies. Flyweight: share the same instance (read-only). |
| **vs. Cache** | Cache stores computed results (memoization). Flyweight shares object structure (same objects reused). |

**Interview Q&A:**

**Q: "When would you use Flyweight in system design?"**
A: Text editor rendering (millions of characters share ~100 glyph objects). Game engines (10K trees share same mesh data, each with unique position). CSS rendering (thousands of DOM nodes share few style objects).

**Q: "What's the critical requirement for Flyweight?"**
A: **Immutability** of shared state. If any client could mutate the flyweight, all clients are affected. Intrinsic state MUST be final/read-only.

**When to use:** Very large number of similar objects, memory is the bottleneck, most state can be externalized.

**When to avoid:** Few objects, significant unique state per object, mutability needed.

<details>
<summary><strong>Code Example — Flyweight (Character Glyph Rendering)</strong></summary>

```java
import java.util.HashMap;
import java.util.Map;

// Flyweight — only intrinsic (shared, immutable) state
class CharacterGlyph {
    private final char symbol;   // Intrinsic
    private final String font;   // Intrinsic

    public CharacterGlyph(char symbol, String font) {
        this.symbol = symbol;
        this.font = font;
        System.out.println("Creating glyph: '" + symbol + "' [" + font + "]");
    }

    // Extrinsic state (position, color) passed at render time — NOT stored
    public void render(int x, int y, String color) {
        System.out.println("  Render '" + symbol + "' at (" + x + "," + y + ") color=" + color);
    }
}

// Flyweight Factory — pool ensures sharing
class GlyphFactory {
    private static final Map<String, CharacterGlyph> pool = new HashMap<>();

    public static CharacterGlyph getGlyph(char symbol, String font) {
        String key = symbol + "-" + font;
        return pool.computeIfAbsent(key, k -> new CharacterGlyph(symbol, font));
    }

    public static int poolSize() { return pool.size(); }
}

public class FlyweightDemo {
    public static void main(String[] args) {
        // Render "HELLO" — 5 characters but only 4 unique glyphs (L shared)
        String text = "HELLO";
        for (int i = 0; i < text.length(); i++) {
            CharacterGlyph g = GlyphFactory.getGlyph(text.charAt(i), "Arial");
            g.render(i * 12, 0, "black"); // Position/color is extrinsic
        }
        System.out.println("Objects created: " + GlyphFactory.poolSize()); // 4 not 5
    }
}
```

</details>

---

## Part 3: Behavioral Patterns

> Behavioral patterns deal with algorithms and communication between objects — how objects interact and distribute responsibilities.

---

### Strategy

**Definition:** The Strategy pattern defines a family of algorithms, encapsulates each one in a separate class, and makes them interchangeable. It lets the algorithm vary independently from the clients that use it, enabling runtime selection of behavior without conditional statements.

**In Simple Terms:** Think of getting to the airport — you can drive, take a cab, ride the bus, or cycle. The goal is the same (reach the airport), but the strategy differs. You pick which one at runtime based on budget, time, or weather. Strategy lets your code swap algorithms the same way you swap transportation modes.

**Intent:** Define a family of algorithms, encapsulate each one, and make them interchangeable.

**Core Mechanism:**
- Context holds a reference to a Strategy interface
- Concrete strategies implement different algorithms
- Client/configuration selects which strategy the context uses
- Context delegates work to the strategy — doesn't implement algorithms itself

**Internal Mechanics:**
```
// Without Strategy (violates OCP — must modify for each new algorithm):
class Sorter {
    void sort(List list, String algorithm) {
        if ("quick".equals(algorithm)) { ... }      // Modify this class
        else if ("merge".equals(algorithm)) { ... } // for every new algorithm
        else if ("heap".equals(algorithm)) { ... }  // → OCP violation
    }
}

// With Strategy (OCP compliant — add new class, never modify existing):
class Sorter {
    private SortStrategy strategy;
    void sort(List list) { strategy.sort(list); } // Delegates
}
// Adding new algorithm: create HeapSort implements SortStrategy — DONE
```

**SOLID Connection:**
- **OCP (primary):** New algorithms added without modifying context or existing strategies
- **DIP:** Context depends on Strategy interface, not concrete implementations
- **SRP:** Each strategy encapsulates ONE algorithm

**Differentiation from Similar Patterns:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. State** | Identical structure. Different INTENT. Strategy: CLIENT selects algorithm (user picks sort order). State: OBJECT transitions itself (order moves from PAID → SHIPPED). Strategy objects are alternatives; State objects are a progression. |
| **vs. Template Method** | Both vary algorithms. Strategy: COMPOSITION (context HAS-A strategy). Template Method: INHERITANCE (subclass IS-A variant). Strategy: change at runtime, multiple strategies composable. Template: fixed at compile time, single inheritance. |
| **vs. Command** | Strategy: swaps HOW to do one thing (different algorithms for same goal). Command: encapsulates WHAT to do (different actions that can be queued/undone). |
| **vs. Bridge** | Both use composition. Strategy: only ONE dimension varies (the algorithm). Bridge: TWO dimensions vary independently. |

**Strategy + Lambda (Modern Java):**
```java
// Classic: full class hierarchy
interface CompressionStrategy { byte[] compress(byte[] data); }
class GzipStrategy implements CompressionStrategy { ... }
class ZstdStrategy implements CompressionStrategy { ... }

// Modern: lambda replaces the class (when strategy is a single method)
Function<byte[], byte[]> gzip = data -> GzipUtils.compress(data);
Function<byte[], byte[]> zstd = data -> ZstdUtils.compress(data);
// No class hierarchy needed! Strategy is still the pattern — ceremony reduced.
```

**Interview Q&A:**

**Q: "Where do you use Strategy in production?"**
A: "At Bentley, our retry mechanism uses Strategy. `RetryStrategy` interface with `ExponentialBackoff`, `FixedDelay`, and `NoRetry` implementations. The circuit-breaker selects which strategy based on failure type — transient errors get exponential backoff, permanent errors get no retry. Adding a new strategy (e.g., `JitteredBackoff`) required zero changes to existing code."

**Q: "Strategy vs. if-else — when is the pattern worth it?"**
A: When: (1) algorithms are complex enough to warrant separate classes (not 2-line lambdas). (2) New algorithms will be added over time (OCP benefit). (3) Algorithms need independent testing. (4) Algorithm selection is configurable/dynamic. DON'T use when: 2-3 simple options that will never change — a switch statement is clearer and less code.

**Q: "How does `Comparator<T>` demonstrate Strategy?"**
A: `Collections.sort(list, comparator)` — the sort algorithm is fixed (TimSort), but the comparison STRATEGY is interchangeable. `Comparator.naturalOrder()`, `Comparator.comparing(User::getName)`, custom lambda — all are strategies for "how to compare." The sort method doesn't know or care which comparator it receives.

**When to use:**
- Multiple algorithms for same task, selectable at runtime
- Class has many conditional branches selecting behavior → extract each into strategy
- Algorithms need independent testing
- Configuration-driven behavior selection

**When to avoid:**
- Only one algorithm exists (premature abstraction)
- Trivially different algorithms (lambda or parameterized function suffices)
- Strategy adds overhead beyond simple switch (2-3 options, will never grow)

<details>
<summary><strong>Code Example — Strategy (Robot Behaviors — from Low-Level-Design-Course/Lecture 08)</strong></summary>

```java
// --- Strategy Interface for Walk ---
interface WalkableRobot {
    void walk();
}

// --- Concrete Strategies for Walk ---
class NormalWalk implements WalkableRobot {
    public void walk() { System.out.println("Walking normally..."); }
}

class NoWalk implements WalkableRobot {
    public void walk() { System.out.println("Cannot walk."); }
}

// --- Strategy Interface for Talk ---
interface TalkableRobot {
    void talk();
}

// --- Concrete Strategies for Talk ---
class NormalTalk implements TalkableRobot {
    public void talk() { System.out.println("Talking normally..."); }
}

class NoTalk implements TalkableRobot {
    public void talk() { System.out.println("Cannot talk."); }
}

// --- Strategy Interface for Fly ---
interface FlyableRobot {
    void fly();
}

class NormalFly implements FlyableRobot {
    public void fly() { System.out.println("Flying normally..."); }
}

class NoFly implements FlyableRobot {
    public void fly() { System.out.println("Cannot fly."); }
}

// --- Robot Base Class (Context) — delegates to strategy objects ---
abstract class Robot {
    protected WalkableRobot walkBehavior;
    protected TalkableRobot talkBehavior;
    protected FlyableRobot flyBehavior;

    public Robot(WalkableRobot w, TalkableRobot t, FlyableRobot f) {
        this.walkBehavior = w;
        this.talkBehavior = t;
        this.flyBehavior = f;
    }

    public void walk() { walkBehavior.walk(); }
    public void talk() { talkBehavior.talk(); }
    public void fly()  { flyBehavior.fly(); }
    public abstract void projection();
}

// --- Concrete Robot Types (compose strategies differently) ---
class CompanionRobot extends Robot {
    public CompanionRobot(WalkableRobot w, TalkableRobot t, FlyableRobot f) { super(w, t, f); }
    public void projection() { System.out.println("Displaying friendly companion features..."); }
}

class WorkerRobot extends Robot {
    public WorkerRobot(WalkableRobot w, TalkableRobot t, FlyableRobot f) { super(w, t, f); }
    public void projection() { System.out.println("Displaying worker efficiency stats..."); }
}

// --- Client: composes robots with desired behaviors at creation ---
public class StrategyDesignPattern {
    public static void main(String[] args) {
        // Companion: walks, talks, can't fly
        Robot robot1 = new CompanionRobot(new NormalWalk(), new NormalTalk(), new NoFly());
        robot1.walk();
        robot1.talk();
        robot1.fly();
        robot1.projection();

        System.out.println("--------------------");

        // Worker: can't walk/talk, but flies
        Robot robot2 = new WorkerRobot(new NoWalk(), new NoTalk(), new NormalFly());
        robot2.walk();
        robot2.talk();
        robot2.fly();
        robot2.projection();
    }
}
```

</details>

---

### Observer

**Definition:** The Observer pattern defines a one-to-many dependency between objects so that when one object (the subject) changes state, all its dependents (observers) are notified and updated automatically. It promotes loose coupling between the subject and its observers.

**In Simple Terms:** Think of subscribing to a YouTube channel. When the creator uploads a new video, every subscriber gets notified automatically — you don’t have to keep checking. The creator doesn’t need to know who you are specifically. Observer is exactly this: one thing changes, everyone interested gets told.

**Intent:** Define a one-to-many dependency so that when one object changes state, all dependents are notified automatically.

**Core Mechanism:**
- Subject maintains a list of observers (subscribers)
- When subject's state changes → iterates list → calls `update()` on each
- Observers register/unregister dynamically
- Decouples the subject from its dependents (subject doesn't know concrete observer types)

**Internal Mechanics:**
```
subject.setState(newValue)
  → for each observer in observerList:
      observer.update(newValue)    [or observer.update(subject) for pull model]
  
Push model: subject sends the data IN the notification → observer passively receives
Pull model: subject says "I changed" → observer queries subject for what it needs

Push: simpler for observers, but couples them to notification format
Pull: more flexible, but observer needs access to query subject
```

**SOLID Connection:**
- **OCP:** New observers can be added without modifying the subject
- **DIP:** Subject depends on Observer interface, not concrete observers

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Mediator** | Observer: one-to-many, unidirectional (subject notifies). Mediator: many-to-many, bidirectional (mediator coordinates). Observer: subject doesn't know what observers DO with the notification. Mediator: knows all participants and orchestrates. |
| **vs. Pub/Sub** | Observer: typically in-process, synchronous, subject knows observers directly. Pub/Sub: typically cross-process, async, message broker decouples publishers from subscribers. Same concept, different scale. |
| **vs. Event Bus** | Event bus is a Mediator-flavored Observer — publishers and subscribers don't know each other; the bus routes. Pure Observer: subject directly notifies registered observers. |

**Interview Q&A:**

**Q: "What's the memory leak risk with Observer?"**
A: Subject holds strong references to observers. If an observer goes "out of scope" from the application's perspective but is still registered, it can't be garbage collected. Classic leak in UI frameworks. Solutions: (1) Always unsubscribe in destructor/`@PreDestroy`. (2) Use `WeakReference` in the observer list. (3) Use lifecycle-aware observers (Android's `LiveData`).

**Q: "How does Spring's event system relate to Observer?"**
A: `ApplicationEventPublisher.publishEvent(event)` is the subject. `@EventListener` methods are observers. Spring's `ApplicationEventMulticaster` iterates registered listeners and invokes them. Synchronous by default (same thread), but can be async with `@Async`. `@TransactionalEventListener` adds phase awareness (AFTER_COMMIT).

**Q: "Observer in distributed systems?"**
A: Kafka topics are Observer at scale. Producer publishes to topic (subject). Consumer groups subscribe (observers). Differences: persistent events, guaranteed delivery, backpressure via consumer lag, exactly-once semantics. Same pattern, infrastructure-hardened.

**When to use:**
- UI events, domain events, system events
- Reactive data binding
- When changes to one object need to trigger updates in unknown/dynamic set of dependents
- Event-driven architectures

**When to avoid:**
- When notification cascade creates infinite loops
- When order of notification matters and is hard to control
- When debugging "who reacted to what" becomes impossible

<details>
<summary><strong>Code Example — Observer (YouTube Channel — from Low-Level-Design-Course/Lecture 12)</strong></summary>

```java
import java.util.ArrayList;
import java.util.List;

// Observer interface
interface ISubscriber {
    void update();
}

// Observable interface: a YouTube channel interface
interface IChannel {
    void subscribe(ISubscriber subscriber);
    void unsubscribe(ISubscriber subscriber);
    void notifySubscribers();
}

// Concrete Subject: a YouTube channel that observers can subscribe to
class Channel implements IChannel {
    private List<ISubscriber> subscribers;
    private String name;
    private String latestVideo;

    public Channel(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
    }

    @Override
    public void subscribe(ISubscriber subscriber) {
        if (!subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }

    @Override
    public void unsubscribe(ISubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers() {
        for (ISubscriber sub : subscribers) {
            sub.update(); // Push notification to all subscribers
        }
    }

    public void uploadVideo(String title) {
        latestVideo = title;
        System.out.println("\n[" + name + " uploaded \"" + title + "\"]");
        notifySubscribers(); // Automatic notification on state change
    }

    public String getVideoData() {
        return "\nCheckout our new Video : " + latestVideo + "\n";
    }
}

// Concrete Observer: represents a subscriber to the channel
class Subscriber implements ISubscriber {
    private String name;
    private Channel channel;

    public Subscriber(String name, Channel channel) {
        this.name = name;
        this.channel = channel;
    }

    @Override
    public void update() {
        System.out.println("Hey " + name + "," + channel.getVideoData());
    }
}

public class ObserverDesignPattern {
    public static void main(String[] args) {
        Channel channel = new Channel("CoderArmy");

        Subscriber subs1 = new Subscriber("Varun", channel);
        Subscriber subs2 = new Subscriber("Tarun", channel);

        // Subscribe
        channel.subscribe(subs1);
        channel.subscribe(subs2);

        // Upload → both Varun and Tarun notified
        channel.uploadVideo("Observer Pattern Tutorial");

        // Varun unsubscribes
        channel.unsubscribe(subs1);

        // Upload → only Tarun notified
        channel.uploadVideo("Decorator Pattern Tutorial");
    }
}
```

</details>

---

### State

**Definition:** The State pattern allows an object to alter its behavior when its internal state changes. The object will appear to change its class by delegating state-specific behavior to the current state object, which can be replaced at runtime to transition between behaviors.

**In Simple Terms:** Think of a vending machine. When it’s in "no coin" state, pressing the button does nothing. Insert a coin — now it’s in "has coin" state and pressing the button dispenses a drink. Same machine, same button, but completely different behavior depending on what state it’s in. State pattern makes each state a separate object that handles behavior for that state.

**Intent:** Allow an object to alter its behavior when its internal state changes. The object appears to change its class.

**Core Mechanism:**
- Context holds a reference to a current State object
- All behavior delegated to the current state
- State objects handle transitions: `context.setState(new NextState())`
- Each state only implements behaviors VALID for that state

**Differentiation (The Critical One):**

| Aspect | State | Strategy |
|---|---|---|
| **Who selects?** | Object transitions ITSELF | CLIENT selects the algorithm |
| **Awareness** | States know about OTHER states (they trigger transitions) | Strategies are independent (don't know about each other) |
| **Semantics** | Represents a PROGRESSION (order lifecycle) | Represents ALTERNATIVES (sort algorithms) |
| **Runtime change** | Automatic (driven by events) | Manual (client explicitly switches) |
| **Example** | TCP: LISTEN → SYN_SENT → ESTABLISHED | Sort: quick vs merge vs heap |

**Interview Q&A:**

**Q: "How would you model an order lifecycle?"**
A: State pattern. States: `CreatedState`, `PaidState`, `ShippedState`, `DeliveredState`, `CancelledState`. Each state defines which operations are valid. `CreatedState.pay()` → transitions to `PaidState`. `ShippedState.pay()` → throws "already paid." This eliminates `if (status == SHIPPED && ...) else if (status == PAID && ...)` conditionals scattered throughout the code.

**Q: "State vs. enum with switch?"**
A: Enum+switch works for 2-3 simple states. State pattern wins when: (1) Each state has significantly different behavior (not just a flag check). (2) States have complex transition rules. (3) You want OCP: new states added without modifying existing ones. (4) State-specific logic is complex enough to warrant its own class.

**Q: "Who should own transitions?"**
A: Two approaches: (1) States know their successors (`PaidState.ship()` calls `context.setState(new ShippedState())`). Pro: transitions encapsulated. Con: states coupled to each other. (2) Context has a transition table. Pro: all transitions visible in one place. Con: context gets complex. For simple linear flows → option 1. For complex state machines with many transitions → option 2 or a state machine library.

**When to use:** Object behavior depends on state (order lifecycle, connection states, game states, UI wizard steps). Complex conditionals based on state. State machines with defined transitions.

**When to avoid:** 2-3 simple states (enum suffices). States don't have different behavior (just data). State transitions are trivial.

<details>
<summary><strong>Code Example — State (Order Lifecycle)</strong></summary>

```java
// State interface — all possible operations
interface OrderState {
    void pay(OrderContext ctx);
    void ship(OrderContext ctx);
    void deliver(OrderContext ctx);
    void cancel(OrderContext ctx);
    String getStatus();
}

// Context
class OrderContext {
    private OrderState state;
    private String orderId;
    
    public OrderContext(String orderId) {
        this.orderId = orderId;
        this.state = new CreatedState();
    }
    
    public void setState(OrderState state) { 
        System.out.println("  [" + orderId + "] " + this.state.getStatus() + " → " + state.getStatus());
        this.state = state; 
    }
    
    public void pay() { state.pay(this); }
    public void ship() { state.ship(this); }
    public void deliver() { state.deliver(this); }
    public void cancel() { state.cancel(this); }
    public String getStatus() { return state.getStatus(); }
}

// Concrete States
class CreatedState implements OrderState {
    public void pay(OrderContext ctx) { ctx.setState(new PaidState()); }
    public void ship(OrderContext ctx) { System.out.println("  Cannot ship — not paid yet"); }
    public void deliver(OrderContext ctx) { System.out.println("  Cannot deliver — not shipped"); }
    public void cancel(OrderContext ctx) { ctx.setState(new CancelledState()); }
    public String getStatus() { return "CREATED"; }
}

class PaidState implements OrderState {
    public void pay(OrderContext ctx) { System.out.println("  Already paid"); }
    public void ship(OrderContext ctx) { ctx.setState(new ShippedState()); }
    public void deliver(OrderContext ctx) { System.out.println("  Cannot deliver — not shipped yet"); }
    public void cancel(OrderContext ctx) {
        System.out.println("  Refund initiated...");
        ctx.setState(new CancelledState());
    }
    public String getStatus() { return "PAID"; }
}

class ShippedState implements OrderState {
    public void pay(OrderContext ctx) { System.out.println("  Already paid"); }
    public void ship(OrderContext ctx) { System.out.println("  Already shipped"); }
    public void deliver(OrderContext ctx) { ctx.setState(new DeliveredState()); }
    public void cancel(OrderContext ctx) { System.out.println("  Cannot cancel — already shipped"); }
    public String getStatus() { return "SHIPPED"; }
}

class DeliveredState implements OrderState {
    public void pay(OrderContext ctx) { System.out.println("  Already completed"); }
    public void ship(OrderContext ctx) { System.out.println("  Already delivered"); }
    public void deliver(OrderContext ctx) { System.out.println("  Already delivered"); }
    public void cancel(OrderContext ctx) { System.out.println("  Cannot cancel — already delivered"); }
    public String getStatus() { return "DELIVERED"; }
}

class CancelledState implements OrderState {
    public void pay(OrderContext ctx) { System.out.println("  Order is cancelled"); }
    public void ship(OrderContext ctx) { System.out.println("  Order is cancelled"); }
    public void deliver(OrderContext ctx) { System.out.println("  Order is cancelled"); }
    public void cancel(OrderContext ctx) { System.out.println("  Already cancelled"); }
    public String getStatus() { return "CANCELLED"; }
}

public class StateDemo {
    public static void main(String[] args) {
        OrderContext order = new OrderContext("ORD-001");
        order.ship();    // Cannot ship — not paid yet
        order.pay();     // CREATED → PAID
        order.ship();    // PAID → SHIPPED
        order.cancel();  // Cannot cancel — already shipped
        order.deliver(); // SHIPPED → DELIVERED
    }
}
```

</details>

---

### Command

**Definition:** The Command pattern encapsulates a request as an object, thereby letting you parameterize clients with different requests, queue or log requests, and support undoable operations. It decouples the object that invokes the operation from the one that knows how to perform it.

**In Simple Terms:** Think of ordering at a restaurant. You don’t walk into the kitchen yourself — you tell the waiter (invoker) what you want, they write it on an order slip (command object), and the chef (receiver) executes it. The slip can be queued, cancelled, or replayed. Command turns "do something" into a tangible object you can pass around.

**Intent:** Encapsulate a request as an object — enables queuing, logging, undo, and transactional behavior.

**Core Mechanism:**
- Command object holds: receiver + action + parameters (everything needed to execute)
- Invoker stores/triggers commands without knowing what they do
- Client creates commands, configures them with receivers, gives to invoker
- Supports: `execute()`, `undo()`, serialization, queuing

**SOLID Connection:**
- **SRP:** Command encapsulates one action. Invoker handles scheduling. Receiver handles domain logic.
- **OCP:** New commands added without modifying invoker or existing commands.

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Strategy** | Strategy: swap HOW to do something (different algorithms, same goal). Command: encapsulate WHAT to do (different actions, queueable/undoable). |
| **vs. Memento** | Command stores ACTION (what to do/undo). Memento stores STATE (what things looked like). Command undo reverses the action. Memento undo restores the snapshot. |

**Interview Q&A:**

**Q: "How does Command relate to CQRS?"**
A: CQRS = Command Query Responsibility Segregation. The "C" is literally Command pattern. Write operations are command objects dispatched through a command bus. Each command represents an intent ("TransferFunds", "CreateOrder"). Command handlers execute them. This decouples the write model from the read model.

**Q: "Command vs. direct method call — when is the overhead justified?"**
A: When you need at least ONE of: (1) Undo/redo. (2) Queuing/scheduling for later. (3) Audit logging of operations. (4) Transaction rollback (execute batch; rollback all on failure). (5) Macro recording (record commands, replay). If you need none of these → just call the method directly.

**When to use:** Undo/redo, task queues, macro recording, transactional batches, CQRS, audit trails.

**When to avoid:** Simple direct calls, no need for queuing/undo/logging, trivial one-line operations.

<details>
<summary><strong>Code Example — Command (Light & Fan Remote — from Low-Level-Design-Course/Lecture 15)</strong></summary>

```java
// Command Interface
interface Command {
    void execute();
    void undo();
}

// Receivers — know how to perform the actual work
class Light {
    public void on()  { System.out.println("Light is ON"); }
    public void off() { System.out.println("Light is OFF"); }
}

class Fan {
    public void on()  { System.out.println("Fan is ON"); }
    public void off() { System.out.println("Fan is OFF"); }
}

// Concrete Command for Light
class LightCommand implements Command {
    private Light light;
    public LightCommand(Light l) { this.light = l; }
    public void execute() { light.on(); }
    public void undo()    { light.off(); }
}

// Concrete Command for Fan
class FanCommand implements Command {
    private Fan fan;
    public FanCommand(Fan f) { this.fan = f; }
    public void execute() { fan.on(); }
    public void undo()    { fan.off(); }
}

// Invoker: Remote Controller with toggle behavior
class RemoteController {
    private static final int numButtons = 4;
    private Command[] buttons;
    private boolean[] buttonPressed;

    public RemoteController() {
        buttons = new Command[numButtons];
        buttonPressed = new boolean[numButtons];
        for (int i = 0; i < numButtons; i++) {
            buttons[i] = null;
            buttonPressed[i] = false;
        }
    }

    public void setCommand(int idx, Command cmd) {
        if (idx >= 0 && idx < numButtons) {
            buttons[idx] = cmd;
            buttonPressed[idx] = false;
        }
    }

    public void pressButton(int idx) {
        if (idx >= 0 && idx < numButtons && buttons[idx] != null) {
            if (!buttonPressed[idx]) {
                buttons[idx].execute();   // ON
            } else {
                buttons[idx].undo();      // OFF (undo)
            }
            buttonPressed[idx] = !buttonPressed[idx]; // Toggle state
        } else {
            System.out.println("No command assigned at button " + idx);
        }
    }
}

public class CommandPattern {
    public static void main(String[] args) {
        Light livingRoomLight = new Light();
        Fan ceilingFan = new Fan();

        RemoteController remote = new RemoteController();
        remote.setCommand(0, new LightCommand(livingRoomLight));
        remote.setCommand(1, new FanCommand(ceilingFan));

        System.out.println("--- Toggling Light Button 0 ---");
        remote.pressButton(0);  // Light ON  (execute)
        remote.pressButton(0);  // Light OFF (undo)

        System.out.println("--- Toggling Fan Button 1 ---");
        remote.pressButton(1);  // Fan ON
        remote.pressButton(1);  // Fan OFF

        System.out.println("--- Pressing Unassigned Button 2 ---");
        remote.pressButton(2);  // "No command assigned"
    }
}
```

</details>

---

### Template Method

**Definition:** The Template Method pattern defines the skeleton of an algorithm in a method of a base class, deferring some steps to subclasses. It lets subclasses redefine certain steps of an algorithm without changing the algorithm’s overall structure.

**In Simple Terms:** Think of a recipe template: "1) Prepare ingredients, 2) Cook, 3) Plate, 4) Serve." Every chef follows this structure, but HOW they cook (step 2) varies — one grills, another bakes. The overall sequence is locked, but specific steps are customizable by subclasses. Template Method is the recipe with blanks to fill in.

**Intent:** Define the skeleton of an algorithm in a base class, letting subclasses override specific steps without changing the structure.

**Core Mechanism:**
- Base class defines a `final` template method that calls steps in fixed order
- Steps are either abstract (MUST override) or hook methods (CAN override, have defaults)
- Subclasses customize WHAT happens in steps, not WHEN/HOW-MANY steps run
- "Hollywood Principle" — framework calls your code, not the other way around

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Strategy** | Template Method: INHERITANCE (subclass IS-A base). Strategy: COMPOSITION (context HAS-A strategy). Template locks structure; Strategy is fully swappable. Template: one fixed flow, customize steps. Strategy: entire algorithm replaceable. |
| **vs. Factory Method** | Factory Method is often USED WITHIN Template Method (one of the steps is a factory method). Template Method is broader — orchestrates multiple steps. |

**Interview Q&A:**

**Q: "Fragile Base Class Problem?"**
A: If the base class template method changes (adds steps, reorders), ALL subclasses may break. The more subclasses exist, the riskier any base class change becomes. Mitigation: keep the template method stable and well-documented. Modern alternative: prefer Strategy/composition over inheritance.

**Q: "Template Method in Spring?"**
A: `JdbcTemplate.execute()` — defines the JDBC workflow (get connection → create statement → execute → handle results → close). Your code provides just the callback steps (what SQL to run, how to map results). `AbstractController` in Spring MVC — template for request handling. `RestTemplate` — template for HTTP request lifecycle.

**When to use:** Fixed algorithm structure with customizable steps, framework design (hooks), ETL pipelines, testing frameworks (setup → test → teardown).

**When to avoid:** When the algorithm structure itself varies. When too many steps need customization. When composition (Strategy) gives more flexibility.

<details>
<summary><strong>Code Example — Template Method (ML Training Pipeline — from Low-Level-Design-Course/Lecture 20)</strong></summary>

```java
// Base class defining the template method
abstract class ModelTrainer {
    // Template method — final so subclasses can't change the sequence
    public final void trainPipeline(String dataPath) {
        loadData(dataPath);
        preprocessData();
        trainModel();      // subclass-specific (abstract)
        evaluateModel();   // subclass-specific (abstract)
        saveModel();       // hook — has default, can override
    }

    // Common steps (concrete — same for all)
    protected void loadData(String path) {
        System.out.println("[Common] Loading dataset from " + path);
    }

    protected void preprocessData() {
        System.out.println("[Common] Splitting into train/test and normalizing");
    }

    // Steps that MUST be implemented by each trainer
    protected abstract void trainModel();
    protected abstract void evaluateModel();

    // Hook method — has default, but subclasses can override
    protected void saveModel() {
        System.out.println("[Common] Saving model to disk as default format");
    }
}

// Concrete subclass: Neural Network Trainer
class NeuralNetworkTrainer extends ModelTrainer {
    @Override
    protected void trainModel() {
        System.out.println("[NeuralNet] Training Neural Network for 100 epochs");
    }

    @Override
    protected void evaluateModel() {
        System.out.println("[NeuralNet] Evaluating accuracy and loss on validation set");
    }

    @Override
    protected void saveModel() {
        System.out.println("[NeuralNet] Serializing network weights to .h5 file");
    }
}

// Concrete subclass: Decision Tree Trainer
class DecisionTreeTrainer extends ModelTrainer {
    @Override
    protected void trainModel() {
        System.out.println("[DecisionTree] Building decision tree with max_depth=5");
    }

    @Override
    protected void evaluateModel() {
        System.out.println("[DecisionTree] Computing classification report (precision/recall)");
    }
    // Uses default saveModel() — no override needed
}

// Same template (loadData → preprocess → train → evaluate → save)
// Different implementations per subclass
public class TemplateMethodPattern {
    public static void main(String[] args) {
        System.out.println("=== Neural Network Training ===");
        ModelTrainer nnTrainer = new NeuralNetworkTrainer();
        nnTrainer.trainPipeline("data/images/");

        System.out.println("\n=== Decision Tree Training ===");
        ModelTrainer dtTrainer = new DecisionTreeTrainer();
        dtTrainer.trainPipeline("data/iris.csv");
    }
}
```

</details>

---

### Chain of Responsibility

**Definition:** The Chain of Responsibility pattern avoids coupling the sender of a request to its receiver by giving more than one object a chance to handle the request. It chains the receiving objects and passes the request along the chain until an object handles it.

**In Simple Terms:** Think of an expense approval system. You submit a $5000 expense — your team lead can approve up to $1000 (passes it on), your manager can approve up to $5000 (handles it). If it were $50,000, it’d go to the VP. Each person in the chain either handles it or passes it up. No one needs to know who ultimately approves.

**Intent:** Pass a request along a chain of handlers. Each handler decides to process it or pass it to the next.

**Core Mechanism:**
- Each handler has a reference to the next handler
- On receiving a request: handle it (stop) OR pass to next (continue)
- Client sends to first handler — doesn't know which will ultimately process it
- Two variants: (1) Pure: exactly one handles. (2) Pipeline: all process and pass.

**SOLID Connection:**
- **OCP:** New handlers added without modifying existing ones
- **SRP:** Each handler has one responsibility

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Decorator** | Decorator: wraps ONE object (onion layers, always delegates). CoR: linked list, handler may STOP propagation. Decorator always reaches the core. CoR may not. |
| **vs. Observer** | Observer: subject notifies ALL observers. CoR: request passes SEQUENTIALLY until ONE handles. |

**Interview Q&A:**

**Q: "Example of CoR in Spring?"**
A: Spring Security's filter chain. Each filter (CsrfFilter, AuthenticationFilter, AuthorizationFilter) either handles the request (returns 401/403) or calls `chain.doFilter()` to pass to the next. Also: exception handler resolution — `@ExceptionHandler` → `@ControllerAdvice` → `DefaultHandlerExceptionResolver` — first one that matches handles it.

**Q: "What if no handler processes the request?"**
A: Always have a fallback handler at the end (e.g., returns 404, logs "unhandled request"). Without it, requests silently disappear — a dangerous bug in production.

**When to use:** Middleware pipelines, event bubbling, approval workflows, exception handling, request validation chains.

**When to avoid:** When exactly one handler is always known (just call it directly). When debugging "which handler ran?" is too difficult.

<details>
<summary><strong>Code Example — Chain of Responsibility (ATM Cash Dispenser — from Low-Level-Design-Course/Lecture 22)</strong></summary>

```java
// Abstract handler — each note denomination is a handler in the chain
abstract class MoneyHandler {
    protected MoneyHandler nextHandler;

    public MoneyHandler() { this.nextHandler = null; }

    public void setNextHandler(MoneyHandler next) { this.nextHandler = next; }

    public abstract void dispense(int amount);
}

class ThousandHandler extends MoneyHandler {
    private int numNotes;
    public ThousandHandler(int numNotes) { this.numNotes = numNotes; }

    @Override
    public void dispense(int amount) {
        int notesNeeded = amount / 1000;
        if (notesNeeded > numNotes) { notesNeeded = numNotes; numNotes = 0; }
        else { numNotes -= notesNeeded; }

        if (notesNeeded > 0)
            System.out.println("Dispensing " + notesNeeded + " x ₹1000 notes.");

        int remainingAmount = amount - (notesNeeded * 1000);
        if (remainingAmount > 0) {
            if (nextHandler != null) nextHandler.dispense(remainingAmount);
            else System.out.println("Remaining ₹" + remainingAmount + " cannot be fulfilled");
        }
    }
}

class FiveHundredHandler extends MoneyHandler {
    private int numNotes;
    public FiveHundredHandler(int numNotes) { this.numNotes = numNotes; }

    @Override
    public void dispense(int amount) {
        int notesNeeded = amount / 500;
        if (notesNeeded > numNotes) { notesNeeded = numNotes; numNotes = 0; }
        else { numNotes -= notesNeeded; }

        if (notesNeeded > 0)
            System.out.println("Dispensing " + notesNeeded + " x ₹500 notes.");

        int remainingAmount = amount - (notesNeeded * 500);
        if (remainingAmount > 0) {
            if (nextHandler != null) nextHandler.dispense(remainingAmount);
            else System.out.println("Remaining ₹" + remainingAmount + " cannot be fulfilled");
        }
    }
}

class TwoHundredHandler extends MoneyHandler {
    private int numNotes;
    public TwoHundredHandler(int numNotes) { this.numNotes = numNotes; }

    @Override
    public void dispense(int amount) {
        int notesNeeded = amount / 200;
        if (notesNeeded > numNotes) { notesNeeded = numNotes; numNotes = 0; }
        else { numNotes -= notesNeeded; }

        if (notesNeeded > 0)
            System.out.println("Dispensing " + notesNeeded + " x ₹200 notes.");

        int remainingAmount = amount - (notesNeeded * 200);
        if (remainingAmount > 0) {
            if (nextHandler != null) nextHandler.dispense(remainingAmount);
            else System.out.println("Remaining ₹" + remainingAmount + " cannot be fulfilled");
        }
    }
}

class HundredHandler extends MoneyHandler {
    private int numNotes;
    public HundredHandler(int numNotes) { this.numNotes = numNotes; }

    @Override
    public void dispense(int amount) {
        int notesNeeded = amount / 100;
        if (notesNeeded > numNotes) { notesNeeded = numNotes; numNotes = 0; }
        else { numNotes -= notesNeeded; }

        if (notesNeeded > 0)
            System.out.println("Dispensing " + notesNeeded + " x ₹100 notes.");

        int remainingAmount = amount - (notesNeeded * 100);
        if (remainingAmount > 0) {
            if (nextHandler != null) nextHandler.dispense(remainingAmount);
            else System.out.println("Remaining ₹" + remainingAmount + " cannot be fulfilled");
        }
    }
}

// Client sets up the chain: 1000 → 500 → 200 → 100
public class COR {
    public static void main(String[] args) {
        MoneyHandler thousandHandler = new ThousandHandler(3);
        MoneyHandler fiveHundredHandler = new FiveHundredHandler(5);
        MoneyHandler twoHundredHandler = new TwoHundredHandler(10);
        MoneyHandler hundredHandler = new HundredHandler(20);

        // Chain: largest denomination first
        thousandHandler.setNextHandler(fiveHundredHandler);
        fiveHundredHandler.setNextHandler(twoHundredHandler);
        twoHundredHandler.setNextHandler(hundredHandler);

        int amountToWithdraw = 4000;
        System.out.println("\nDispensing amount: ₹" + amountToWithdraw);
        thousandHandler.dispense(amountToWithdraw);
        // Output: 3 x ₹1000 + 2 x ₹500 = ₹4000
    }
}
```

</details>

---

### Mediator

**Definition:** The Mediator pattern defines an object that encapsulates how a set of objects interact. It promotes loose coupling by keeping objects from referring to each other explicitly, and lets you vary their interaction independently by centralizing communication logic.

**In Simple Terms:** Think of an air traffic control tower. Planes don’t talk to each other directly ("Hey Flight 302, move left!"). Instead, every plane talks only to the tower, and the tower coordinates everyone. The Mediator is that control tower — it prevents a web of direct connections and centralizes all the coordination logic.

**Intent:** Centralize complex communication between objects. Components communicate through the mediator rather than directly.

**Core Mechanism:**
- Without mediator: N components × (N-1) connections = N² coupling
- With mediator: N components × 1 mediator connection = N coupling
- Components know only the mediator interface — not each other
- Mediator orchestrates interactions based on component events

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Observer** | Observer: one-to-many, simple notification. Mediator: many-to-many, coordinated interaction. Mediator often USES Observer internally. |
| **vs. Facade** | Facade: simplifies access TO a subsystem (client → facade → subsystem). Mediator: simplifies communication WITHIN a subsystem (component ↔ mediator ↔ component). Facade is unidirectional; Mediator is bidirectional. |

**Interview Q&A:**

**Q: "Give a modern example of Mediator."**
A: Redux store. Components dispatch actions to the store (mediator). Store decides what to update and which components to re-render. Components never communicate directly. Also: Kubernetes controller manager — controllers don't talk to each other, they watch/update shared state through the API server (mediator).

**Q: "When does a Mediator become an anti-pattern?"**
A: When it becomes a "God Mediator" — containing all the business logic itself. A good mediator COORDINATES (routes messages, triggers workflows). It should NOT IMPLEMENT complex business logic. If the mediator class is 1000+ lines, it needs to delegate to services.

**When to use:** Complex inter-component communication, UI form coordination, chat rooms, event routing, reducing N² coupling to N.

**When to avoid:** Simple direct relationships. When mediator becomes god object. When 2 objects just need to call each other.

<details>
<summary><strong>Code Example — Mediator (Chat Room)</strong></summary>

```java
import java.util.ArrayList;
import java.util.List;

// Mediator interface
interface ChatMediator {
    void sendMessage(String msg, User sender);
    void addUser(User user);
}

// Concrete Mediator — routes messages between users
class ChatRoom implements ChatMediator {
    private List<User> users = new ArrayList<>();

    public void addUser(User user) { users.add(user); }

    public void sendMessage(String msg, User sender) {
        for (User user : users) {
            if (user != sender) { // Don't echo to sender
                user.receive(msg, sender.getName());
            }
        }
    }
}

// Colleague — knows only the mediator, not other users
class User {
    private String name;
    private ChatMediator mediator;

    public User(String name, ChatMediator mediator) {
        this.name = name;
        this.mediator = mediator;
        mediator.addUser(this);
    }

    public String getName() { return name; }

    public void send(String msg) {
        System.out.println(name + " sends: " + msg);
        mediator.sendMessage(msg, this);
    }

    public void receive(String msg, String from) {
        System.out.println(name + " received from " + from + ": " + msg);
    }
}

public class MediatorDemo {
    public static void main(String[] args) {
        ChatMediator room = new ChatRoom();
        User alice = new User("Alice", room);
        User bob = new User("Bob", room);
        User charlie = new User("Charlie", room);

        alice.send("Hey everyone!"); // Bob & Charlie receive
        bob.send("Hi Alice!");       // Alice & Charlie receive
    }
}
```

</details>

---

### Memento

**Definition:** The Memento pattern captures and externalizes an object’s internal state without violating encapsulation, so that the object can be restored to this state later. It involves three roles: the originator (whose state is saved), the memento (the snapshot), and the caretaker (who manages mementos).

**In Simple Terms:** Think of the "Save Game" feature in a video game. You save your progress (memento) at any checkpoint. If you die, you reload from that save point without needing to know the internal details of the game engine. Memento is Ctrl+Z for objects — snapshot now, restore later.

**Intent:** Capture an object's internal state as an opaque snapshot for later restoration, without violating encapsulation.

**Core Mechanism:**
- **Originator:** Creates mementos (snapshots of its own state). Restores from mementos.
- **Memento:** Opaque state holder. Only the originator can read its contents.
- **Caretaker:** Stores mementos (history stack). Cannot examine/modify memento contents.

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Command (for undo)** | Memento: saves ENTIRE STATE, restores by replacing state. Command: saves ACTION, undoes by reversing the action. Memento is simpler (snapshot/restore) but uses more memory. Command is lighter but requires every operation to be reversible. |
| **vs. Prototype** | Prototype: copies for creating new independent objects. Memento: copies for RESTORING a specific object's past state. |

**Interview Q&A:**

**Q: "Memento vs. Event Sourcing?"**
A: Both enable "time travel." Memento: stores full snapshots at checkpoints. Event Sourcing: stores individual events, reconstructs state by replaying. Event Sourcing uses less storage (events are smaller than full state), supports "what happened at time T?" queries, but replay can be slow. Memento: instant restore, expensive storage.

**Q: "How does Git relate to Memento?"**
A: Each commit is a memento (snapshot of repository state). The commit history is the caretaker (stores ordered mementos). `git checkout <commit>` restores from a memento. Git optimizes storage with packfiles (delta compression) — storing full snapshots for every commit would be too expensive.

**When to use:** Undo/redo, game checkpoints, transaction savepoints, editor history.

**When to avoid:** Very large state (expensive snapshots), state changes frequently (memory explosion), state is easily recomputable.

<details>
<summary><strong>Code Example — Memento (Text Editor Undo)</strong></summary>

```java
import java.util.Stack;

// Memento — opaque state snapshot
class EditorMemento {
    private final String content;
    EditorMemento(String content) { this.content = content; }
    String getContent() { return content; }
}

// Originator — creates and restores from mementos
class TextEditor {
    private StringBuilder content = new StringBuilder();

    public void type(String text) { content.append(text); }
    public String getContent() { return content.toString(); }

    public EditorMemento save() {
        return new EditorMemento(content.toString());
    }

    public void restore(EditorMemento memento) {
        content = new StringBuilder(memento.getContent());
    }
}

// Caretaker — stores mementos, cannot examine contents
class UndoManager {
    private Stack<EditorMemento> history = new Stack<>();

    public void save(TextEditor editor) { history.push(editor.save()); }

    public void undo(TextEditor editor) {
        if (!history.isEmpty()) editor.restore(history.pop());
    }
}

public class MementoDemo {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        UndoManager undo = new UndoManager();

        editor.type("Hello");
        undo.save(editor);        // Snapshot: "Hello"

        editor.type(" World");
        undo.save(editor);        // Snapshot: "Hello World"

        editor.type("!!!");
        System.out.println(editor.getContent()); // Hello World!!!

        undo.undo(editor);
        System.out.println(editor.getContent()); // Hello World

        undo.undo(editor);
        System.out.println(editor.getContent()); // Hello
    }
}
```

</details>

---

### Iterator

**Definition:** The Iterator pattern provides a way to access the elements of an aggregate object sequentially without exposing its underlying representation. It encapsulates the traversal logic in a separate iterator object, decoupling the collection from the traversal algorithm.

**In Simple Terms:** Think of a TV remote with "Next Channel" and "Previous Channel" buttons. You don’t need to know how channels are stored internally (array? linked list? database?). You just press Next and get the next channel. Iterator gives you a uniform way to go through any collection one item at a time.

**Intent:** Provide sequential access to elements of a collection without exposing its internal representation.

**Core Mechanism:** Encapsulates traversal logic in a separate object (cursor). Collection provides a factory method to create iterators. Client uses uniform interface (`hasNext()`/`next()`) regardless of collection type.

**Interview Q&A:**

**Q: "Internal vs. External Iterator?"**
A: External: client controls (`while (it.hasNext()) it.next()`). Internal: collection controls, client provides callback (`list.forEach(item -> ...)`). Modern languages prefer internal (functional style, less boilerplate, parallelizable).

**Q: "Lazy iterators?"**
A: Generators/streams that compute elements on demand. `IntStream.range(0, 1_000_000_000)` doesn't allocate a billion integers — it yields them one at a time. Essential for: large datasets, infinite sequences, pipeline transformations (filter/map/reduce).

**When to use:** Uniform collection traversal, lazy evaluation, custom traversal orders (BFS/DFS on trees), pagination.

**When to avoid:** Simple indexed access suffices. Random access needed. Trivial collections.

<details>
<summary><strong>Code Example — Iterator (Custom Collection with Internal Cursor)</strong></summary>

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

// Custom collection
class NumberRange implements Iterable<Integer> {
    private final int start;
    private final int end;

    public NumberRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new RangeIterator();
    }

    // Internal iterator — encapsulates traversal, hides data structure
    private class RangeIterator implements Iterator<Integer> {
        private int current = start;

        public boolean hasNext() { return current <= end; }

        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            return current++;
        }
    }
}

public class IteratorDemo {
    public static void main(String[] args) {
        NumberRange range = new NumberRange(1, 5);

        // Works with enhanced for-loop (Iterable interface)
        for (int n : range) {
            System.out.print(n + " "); // 1 2 3 4 5
        }

        // Multiple independent iterators on same collection
        Iterator<Integer> it1 = range.iterator();
        Iterator<Integer> it2 = range.iterator();
        it1.next(); it1.next(); // Advances to 3
        System.out.println("\nit1=" + it1.next() + ", it2=" + it2.next()); // it1=3, it2=1
    }
}
```

</details>

---

### Visitor

**Definition:** The Visitor pattern represents an operation to be performed on the elements of an object structure. It lets you define new operations without changing the classes of the elements on which they operate, using double-dispatch to invoke the correct method based on both the visitor and element types.

**In Simple Terms:** Think of a tax inspector visiting different types of businesses — restaurants, shops, factories. Each business "accepts" the inspector and shows their books. The inspector applies different tax rules to each type. You can add new inspectors (new operations) without changing how businesses work. Visitor adds operations to objects from the outside.

**Intent:** Add operations to an object structure without modifying the element classes. Separates algorithms from the objects they operate on.

**Core Mechanism:**
- Elements accept a visitor: `element.accept(visitor)` → `visitor.visit(this)` (double dispatch)
- Visitor has one `visit` method per element type
- New operations = new Visitor class (no changes to elements)
- **Double dispatch:** Both element type AND visitor type determine which code runs

**The Expression Problem:**
```
             Easy to add new types?   Easy to add new operations?
Polymorphism        ✅ (add class)           ❌ (modify all classes)
Visitor             ❌ (modify all visitors) ✅ (add visitor class)
```

Neither solves both simultaneously. Visitor trades type-extensibility for operation-extensibility.

**Differentiation:**

| vs. Pattern | Key Difference |
|---|---|
| **vs. Strategy** | Strategy: varies the algorithm for ONE operation. Visitor: adds MULTIPLE different operations to a STRUCTURE of elements. Strategy is per-object. Visitor traverses many objects. |
| **vs. Iterator** | Iterator: traversal. Visitor: operations during traversal. Often combined — iterate the structure, visit each element. |

**Interview Q&A:**

**Q: "When is Visitor the right choice?"**
A: Stable element hierarchy + frequently changing operations. Classic: compiler AST. Node types (IfStatement, Assignment, FunctionCall) are stable. Operations (type check, optimize, generate code, pretty print) are added frequently. Each new compiler pass is a new Visitor — zero changes to AST nodes.

**Q: "Modern alternative to Visitor?"**
A: Pattern matching (Java 17+ sealed interfaces + switch, Kotlin when). Achieves the same dispatch without the ceremony of accept/visit methods. `when (node) { is IfStatement -> ..., is Assignment -> ... }` — cleaner, same effect.

**When to use:** Compiler ASTs, document processing, stable structure with many operations, reporting over data structures.

**When to avoid:** Element types change frequently (each new type breaks all visitors). Few operations (just put them on elements). Language supports pattern matching.

<details>
<summary><strong>Code Example — Visitor (Shopping Cart Pricing)</strong></summary>

```java
import java.util.Arrays;
import java.util.List;

// Element interface — accepts visitor
interface CartItem {
    void accept(CartVisitor visitor);
}

class Book implements CartItem {
    private double price;
    public Book(double price) { this.price = price; }
    public double getPrice() { return price; }
    public void accept(CartVisitor visitor) { visitor.visit(this); }
}

class Electronics implements CartItem {
    private double price;
    public Electronics(double price) { this.price = price; }
    public double getPrice() { return price; }
    public void accept(CartVisitor visitor) { visitor.visit(this); }
}

// Visitor interface — one method per element type (double dispatch)
interface CartVisitor {
    void visit(Book book);
    void visit(Electronics electronics);
}

// Concrete Visitor 1: Tax calculation (different rules per type)
class TaxVisitor implements CartVisitor {
    private double total = 0;
    public void visit(Book book) { total += book.getPrice(); }           // Books: tax-free
    public void visit(Electronics e) { total += e.getPrice() * 1.18; }  // 18% GST
    public double getTotal() { return total; }
}

// Concrete Visitor 2: Invoice generation
class InvoiceVisitor implements CartVisitor {
    public void visit(Book book) {
        System.out.println("Book: $" + book.getPrice() + " (tax-free)");
    }
    public void visit(Electronics e) {
        System.out.printf("Electronics: $%.2f + 18%% GST = $%.2f%n", e.getPrice(), e.getPrice() * 1.18);
    }
}

public class VisitorDemo {
    public static void main(String[] args) {
        List<CartItem> cart = Arrays.asList(
            new Book(29.99), new Electronics(499.99), new Book(15.00));

        TaxVisitor tax = new TaxVisitor();
        InvoiceVisitor invoice = new InvoiceVisitor();

        for (CartItem item : cart) {
            item.accept(tax);
            item.accept(invoice);
        }
        System.out.printf("\nTotal with tax: $%.2f%n", tax.getTotal());
    }
}
```

</details>

---

## Part 4: Modern / Infrastructure Patterns

### Circuit Breaker

**Definition:** The Circuit Breaker pattern prevents an application from repeatedly attempting an operation that is likely to fail, by wrapping calls to an external service in a state machine that monitors failure rates and short-circuits requests when a failure threshold is breached, thereby preventing cascading failures and allowing the failing service time to recover.

**In Simple Terms:** Think of an electrical circuit breaker in your house. If too much current flows (something is wrong), the breaker "trips" and cuts off electricity to protect your appliances. You wait, fix the problem, then flip the switch back on. Similarly, if a remote service keeps failing, the circuit breaker stops sending requests (protecting your system from hanging), waits a bit, then carefully tests if the service is back.

**Intent:** Prevent cascading failures by fast-failing calls to unhealthy services.

**Core Mechanism:** State machine — CLOSED (normal) → OPEN (fast-fail) → HALF-OPEN (probe recovery).

**This is State Pattern applied to resilience.** Each state has different behavior for handling requests.

```
CLOSED: Forward calls normally. Count failures.
  → failure rate > threshold → transition to OPEN

OPEN: Reject all calls immediately (don't even try). Return fallback/error.
  → wait duration passes → transition to HALF-OPEN

HALF-OPEN: Allow limited calls (probe). If they succeed → CLOSED. If they fail → back to OPEN.
```

**Interview connection:** Always mention circuit breakers in system design when discussing inter-service communication.

---

### Saga

**Definition:** The Saga pattern manages data consistency across microservices in distributed transaction scenarios by decomposing a long-lived transaction into a sequence of local transactions, each with a corresponding compensating transaction that is executed if a subsequent step fails, achieving eventual consistency without distributed locks.

**In Simple Terms:** Think of booking a vacation — you book a flight, then a hotel, then a car rental. If the car rental fails, you don't just shrug — you cancel the hotel, then cancel the flight (undo in reverse order). Saga works the same way: each step knows how to "undo" itself, so if step 4 fails, steps 3, 2, 1 get rolled back one by one.

**Intent:** Manage distributed transactions across microservices without 2PC (two-phase commit).

**Core Mechanism:** Each step has a compensating action. If step N fails, compensate steps N-1, N-2, ..., 1.

| Type | How | Pro | Con |
|---|---|---|---|
| **Choreography** | Each service publishes events, next service reacts | Loose coupling, resilient | Hard to trace, complex debugging |
| **Orchestration** | Central coordinator drives each step | Clear flow, easy to trace | Single point of coordination |

**Key requirement:** Every step must be **idempotent** (safe to retry) and have a **compensating action** (how to undo).

---

### Event Sourcing

**Definition:** Event Sourcing is an architectural pattern that persists the state of a business entity as a sequence of immutable state-changing events rather than storing just the current state. The current state is derived by replaying the event log from the beginning (or from a snapshot), providing a complete audit trail and enabling temporal queries.

**In Simple Terms:** Instead of only keeping your bank account balance ($500), imagine keeping every transaction ever: "+$1000 salary, -$200 rent, -$50 groceries, -$250 bills." You can always recalculate the balance by replaying transactions, AND you can answer "what was my balance on March 3rd?" Event Sourcing stores the history of WHAT HAPPENED, not just WHERE YOU ARE NOW.

**Intent:** Store state as sequence of events, not current snapshot. Reconstruct state by replaying events.

**When to use:** Audit requirements (finance, healthcare), temporal queries ("state at time T?"), complex domains where history matters.

**Relationship:** Event Sourcing = Memento pattern at scale. Each event is a mini-memento. Combined with CQRS (Command writes events, Query reads projections).

---

## Part 5: Pattern Relationships & Decision Guide

> In real production systems, patterns never exist in isolation. They compose, collaborate, and complement each other. Understanding how patterns fit together is what separates a pattern-aware developer from one who merely memorizes definitions. This section maps out how patterns combine in practice and provides a decision framework for selecting the right pattern given a problem statement.

### Patterns that combine in real systems

Design patterns gain their true power when combined. Most real architectures use 3-5 patterns working together — each handling a different concern. The table below shows common production scenarios and which patterns naturally collaborate to solve them. When you see these scenarios in system design interviews, mention the combination, not just one pattern.

| Scenario | Pattern Combination |
|----------|-------------------|
| Undo/redo system | Command + Memento |
| Plugin architecture | Strategy + Factory + Observer |
| Middleware pipeline | Chain of Responsibility + Decorator |
| Event-driven microservices | Observer + Command + Saga |
| Resilient service calls | Proxy + Circuit Breaker + Strategy (retry algorithm) |
| Spring @Transactional | Proxy + Template Method (TransactionTemplate) |
| Spring Data repositories | Proxy + Factory Method + Strategy (query derivation) |
| Compiler/interpreter | Composite (AST) + Visitor (operations) + Iterator (traversal) |

### "Which pattern do I need?" — Decision Guide

When facing a design problem, start with the PROBLEM, not the pattern. Ask yourself "what force am I trying to resolve?" — then look up the appropriate pattern. This table maps problem statements to patterns. In interviews, show that you reason from requirements to pattern, not the reverse.

| Problem | Pattern |
|---------|---------|
| Create objects without specifying exact class | Factory Method / Abstract Factory |
| Complex construction with many options | Builder |
| One instance globally | Singleton (prefer DI scoping) |
| Copy existing configured objects | Prototype |
| Make incompatible interfaces work together | Adapter |
| Two independent dimensions of variation | Bridge |
| Treat single/group objects uniformly (trees) | Composite |
| Add behavior dynamically (logging, retry, cache) | Decorator |
| Simplify a complex subsystem | Facade |
| Many similar objects, memory constrained | Flyweight |
| Control access to an object | Proxy |
| Multiple handlers, unknown which processes | Chain of Responsibility |
| Queue/undo/log operations | Command |
| Traverse without exposing internals | Iterator |
| Many objects interact complexly | Mediator |
| Save/restore object state | Memento |
| Notify many objects of changes | Observer |
| Behavior changes with internal state | State |
| Interchangeable algorithms | Strategy |
| Fixed algorithm structure, customizable steps | Template Method |
| New operations on stable structure | Visitor |

---

## Part 6: Interview Power Tips

> Knowing patterns is necessary but not sufficient for interviews. How you PRESENT patterns — with judgment, tradeoff awareness, and production context — is what differentiates senior engineers from juniors. This section covers the meta-skills of discussing patterns effectively.

### The Staff-Level Mindset

Interviewers at the senior/staff level don't want you to recite pattern definitions. They want to see JUDGMENT — can you pick the right tool, articulate why, and acknowledge what you're giving up? The following principles separate memorable answers from forgettable ones.

**Patterns are vocabulary, not prescriptions.**

1. **Never just name the pattern.** Explain: (a) what problem it solves, (b) production example, (c) tradeoff accepted.
2. **Show awareness of alternatives.** "I considered Strategy but chose Template Method because the structure is fixed."
3. **Connect to SOLID.** Every pattern has SOLID connections — make them explicit.
4. **Discuss when NOT to use it.** Shows judgment > knowledge.
5. **Mention modern alternatives.** Strategy → lambdas. Observer → reactive streams. Visitor → pattern matching.

### The "Three Pattern" Rule for System Design

In any system design interview, the interviewer expects you to organically introduce patterns as you design. Rather than naming all 23 GoF patterns, aim to naturally weave in 3-5 from different categories. This shows breadth without appearing like you're pattern-shopping. Here's how to think about which categories to draw from:

Any system design will use 3-5 patterns:
- **Structural:** for data/object organization (Composite, Facade)
- **Behavioral:** for communication (Observer, Command, Chain)
- **Creational:** for object lifecycle (Factory, Builder)
- **Resilience:** for fault tolerance (Circuit Breaker, Retry/Strategy)
- **Data:** for persistence (Repository, Event Sourcing)

### Anti-Patterns to Mention

Showing awareness of anti-patterns is just as impressive as knowing patterns. When discussing a pattern, briefly mention what the WRONG approach looks like — it demonstrates that you've seen codebases go wrong and know how to steer them back. Here are the most common anti-patterns interviewers expect you to recognize:

| Anti-Pattern | Why It's Bad | Fix |
|---|---|---|
| God Object | One class does everything | SRP — split into focused classes |
| Singleton Abuse | Global state, untestable | DI container scoping |
| Golden Hammer | One pattern for everything | Match pattern to problem |
| Speculative Generality | Abstractions for hypothetical futures | YAGNI — wait for second use case |
| Service Locator | Hidden dependencies | Explicit constructor injection |

### Pattern Evolution in Modern Stacks

Design patterns were formalized in 1994 (GoF book). Modern languages and frameworks have absorbed many of these patterns into their standard features — lambdas replace Strategy, reactive streams replace Observer, AOP replaces Proxy. Showing awareness of these evolutions signals that you don't blindly apply 30-year-old patterns when simpler modern idioms exist.

| Classic | Modern Equivalent |
|---|---|
| Observer | Reactive Streams, Spring Events, Kafka |
| Strategy | Lambda / `Function<T,R>` parameter |
| Command | Message in queue (Kafka, SQS) |
| Singleton | DI singleton scope |
| Template Method | Higher-order functions, React hooks |
| Visitor | Sealed classes + pattern matching |
| Iterator | Streams, generators, async iterators |
| Proxy | AOP annotations (@Cacheable, @Transactional) |
| Chain of Responsibility | Middleware (Express, Spring Security) |
| Abstract Factory | Spring @Profile + conditional beans |

---

## Part 7: Interview Scenario Walkthroughs

> The ultimate test of pattern knowledge is applying multiple patterns to a realistic system design problem. This section walks through 4 common interview scenarios, showing how to identify which patterns to use, how they compose, and what to say to the interviewer. Practice narrating these out loud — your verbal fluency with pattern reasoning is what gets assessed.

### Scenario 1: "Design a notification system"

This is one of the most common system design questions. It tests your ability to identify multiple concerns (delivery channels, subscriber management, message flow, extensibility) and map each to the right pattern.

**Patterns used:** Strategy + Observer + Factory Method + Template Method

```
Analysis:
- Multiple notification TYPES (email, SMS, push) → Strategy or Bridge
- Multiple SUBSCRIBERS to events → Observer
- Don't know notification type at compile time → Factory Method
- All notifications follow same FLOW (validate → format → send → log) → Template Method

Architecture:
  EventBus (Observer) publishes domain events
    → NotificationService subscribes
      → NotificationFactory (Factory Method) creates correct Notification type
        → Each Notification subclass uses Template Method for send flow
          → DeliveryChannel (Strategy) handles actual delivery mechanism
```

**Interview talking points:**
- "Observer decouples event producers from notification logic"
- "Strategy lets us swap delivery channels without touching notification logic"
- "Template Method ensures consistent flow: validate → rate-limit → deliver → audit-log"
- "Factory Method means adding Push notification = new class, zero modification"

---

### Scenario 2: "Design a payment processing system"

Payment systems combine validation, strategy selection, third-party integration, and transactional safety — each mapping cleanly to a different pattern. This scenario tests your ability to handle cross-cutting concerns elegantly.

**Patterns used:** Strategy + Adapter + Chain of Responsibility + Command

```
Architecture:
  PaymentRequest comes in
    → Validation Chain (CoR): fraud check → amount limit → account check
      → PaymentStrategy selected (credit card, UPI, wallet)
        → PaymentGateway adapter wraps third-party (Stripe/Adyen)
          → Transaction stored as Command (supports undo = refund)
```

**Key pattern choices:**
- **CoR for validation:** Each validator is independent. Add new rules without modifying existing.
- **Strategy for payment method:** Runtime selection, clean OCP compliance.
- **Adapter for gateways:** Domain defines PaymentGateway interface. Adapters wrap Stripe/Adyen.
- **Command for transactions:** Queue, log, retry, undo (refund) are all Command capabilities.

---

### Scenario 3: "Design a text editor with undo/redo"

This is the classic behavioral pattern question. It tests Command (for operations), Memento (for state snapshots), Composite (for document structure), and Iterator (for traversal/cursor). Interviewers look for how you combine these without over-engineering.

**Patterns used:** Command + Memento + Composite + Iterator

```
Architecture:
  Document (Composite): paragraphs contain sentences contain words
    → Every edit is a Command (InsertText, DeleteText, FormatText)
      → Command.execute() stores Memento (state before the change)
      → Undo: pop last Command, restore Memento
      → Redo: re-execute Command
    → Traversal via Iterator (cursor position, find/replace)
```

---

### Scenario 4: "Design a rate limiter / circuit breaker"

This scenario combines resilience patterns with structural patterns. It's common in backend/infrastructure interviews at companies like Netflix, Uber, and Amazon. It tests your understanding of state machines, proxying, and algorithm interchangeability.

**Patterns used:** State + Strategy + Proxy + Decorator

```
Architecture:
  Client calls service through Proxy
    → Proxy implements circuit breaker (State pattern):
        CLOSED → counts failures → threshold exceeded → OPEN
        OPEN → rejects all calls → timeout → HALF_OPEN
        HALF_OPEN → allows probe → success → CLOSED / failure → OPEN
    → Rate limiting strategy (Strategy):
        TokenBucket / SlidingWindow / FixedWindow
    → Retry logic (Decorator): wraps the call with exponential backoff
```

---

## Part 8: Deep Confusion Resolvers

> Certain pattern pairs look structurally identical but have fundamentally different intents. Interviewers LOVE asking "What's the difference between X and Y?" because it reveals whether you understand patterns at the intent level or just the code level. This section provides definitive distinctions for the most commonly confused pairs.

### Strategy vs. State — The Definitive Distinction

Strategy and State have nearly identical class diagrams (context delegates to an interface, concrete implementations vary behavior). The difference is entirely in SEMANTICS and WHO CONTROLS THE CHANGE. This is one of the top-3 most-asked pattern comparison questions.

| Aspect | Strategy | State |
|--------|----------|-------|
| **Who changes it?** | CLIENT explicitly sets strategy | OBJECT transitions itself |
| **Awareness** | Strategies don't know about each other | States know their valid transitions |
| **Semantics** | ALTERNATIVES (pick one) | PROGRESSION (lifecycle) |
| **Number of instances** | Often stateless (reusable singleton strategies) | Often per-context (each object has its own state) |
| **When it changes** | Typically once (at construction/config) | Continuously (event-driven transitions) |
| **Remove one?** | Others still work perfectly | Breaks the state machine |

**Memory trick:** Strategy = "I chose this." State = "I became this."

---

### Decorator vs. Proxy vs. Adapter — Same Structure, Different Intent

All three patterns involve one object wrapping another. Their UML diagrams look almost identical. Yet they solve completely different problems. The distinction is purely about WHY you're wrapping — interviewers test whether you can articulate this.

All three wrap an object. The difference is WHY:

```
                 Changes interface?    Controls access?    Adds behavior?
Adapter               ✅                    ❌                  ❌
Proxy                 ❌                    ✅                  ❌*
Decorator             ❌                    ❌                  ✅

* Proxy may add cross-cutting behavior (logging), but its PRIMARY intent is access control.
```

**Decision:** "Am I changing HOW to talk to it?" → Adapter. "Am I controlling WHO/WHEN can use it?" → Proxy. "Am I adding WHAT it can do?" → Decorator.

---

### Factory Method vs. Abstract Factory vs. Builder — Creation Pattern Selection

These three creational patterns are often confused because they all "create objects." The key differentiator is WHAT DECISION each pattern encapsulates: which type to create, which family to use, or how to assemble a complex object.

```
Q: How many products are you creating?
├─ ONE product, type varies → Factory Method
├─ FAMILY of related products → Abstract Factory  
└─ ONE complex product, construction varies → Builder

Q: What varies?
├─ WHICH class gets instantiated → Factory Method
├─ Which FAMILY of classes → Abstract Factory
└─ HOW an object is configured/assembled → Builder
```

---

### Observer vs. Mediator vs. Pub/Sub — Communication Patterns

These three patterns all deal with "how objects communicate" but at different scales, coupling levels, and use cases. Understanding when to graduate from one to the next shows architectural maturity.

```
                  Coupling      Direction     Awareness        Scale
Observer          Medium        1 → N         Subject knows    In-process
                                              observer list
Mediator          Low           N ↔ N         Only mediator    In-process
                                              knows all
Pub/Sub           Very Low      N ↔ N         Nobody knows     Cross-process
                                              anybody
```

**When to graduate:**
- 2-3 observers on one subject → Observer pattern
- 5+ components interacting → Mediator
- Cross-service, async, persistent → Pub/Sub (Kafka/RabbitMQ)

---

## Part 9: Expert Interview Tips

> This section provides the tactical framework for presenting patterns during actual interviews. Even if you know everything above perfectly, how you DELIVER it determines your score. Practice these frameworks until they're second nature.

### How to Present a Pattern in an Interview (60-second formula)

Interviewers form opinions in the first 60 seconds of your answer. This formula structures your response to hit all the points they're scoring: knowledge, judgment, experience, and alternatives. Practice delivering each pattern using this structure until it's natural.

```
1. NAME + one-sentence intent                               (5 sec)
2. "The problem this solves is..."                          (10 sec)
3. Quick mechanism: "Works by [key mechanism]"              (10 sec)
4. Real production example from YOUR experience             (15 sec)
5. Key tradeoff: "The cost is... which is acceptable when..." (10 sec)
6. Alternative: "You could also use [X] but [Y] is better here because..." (10 sec)
```

### Common Interviewer Follow-ups (Be Ready)

After your initial answer, interviewers probe deeper with follow-up questions. These follow-ups test whether your knowledge is surface-level or experiential. Here are the most common follow-ups, what they're really testing, and what your answer should include:

| They ask... | They're testing... | Strong answer includes... |
|---|---|---|
| "What's the tradeoff?" | Judgment, not just knowledge | Specific cost + when it's acceptable |
| "What would you use instead?" | Breadth of options | Alternative pattern + why you'd switch |
| "How does this relate to SOLID?" | Principles understanding | Which principle it satisfies AND which it tensions |
| "Where does this break down?" | Real experience | Concrete scenario where the pattern hurt |
| "How would you test this?" | Production mindset | Mock strategy, contract tests for LSP, integration for adapters |

### Patterns That ALWAYS Come Up (Prioritize These)

Not all 23 patterns are equally likely in interviews. Prioritize your preparation time based on real interview frequency. Tier 1 patterns are asked directly ("explain Strategy pattern"), Tier 2 appear in system design discussions, and Tier 3 are rarely asked directly but show depth when you reference them.

**Tier 1 — Know cold (asked directly):**
- Strategy, Observer, Factory Method, Builder, Singleton, Decorator, Adapter

**Tier 2 — Know well (used in system design):**
- State, Command, Chain of Responsibility, Proxy, Facade, Template Method

**Tier 3 — Know conceptually (rarely asked directly):**
- Visitor, Memento, Flyweight, Bridge, Mediator, Iterator, Prototype, Abstract Factory

### The "Which Pattern" Quick Decision Matrix

This is your cheat sheet for system design rounds. When you identify a requirement ("we need to swap algorithms", "we need to undo operations"), immediately reference this table mentally to name the right pattern with confidence.

| You need to... | Use... |
|---|---|
| Swap algorithms at runtime | Strategy |
| React to state changes | Observer |
| Create objects without specifying type | Factory Method |
| Add features to objects dynamically | Decorator |
| Undo operations | Command + Memento |
| Model object lifecycle (order states) | State |
| Process requests through filters/validators | Chain of Responsibility |
| Wrap a third-party library | Adapter |
| Control expensive object creation | Proxy (virtual) |
| Define a reusable algorithm skeleton | Template Method |
| Compose tree structures | Composite |
| Simplify a complex API | Facade |
| Build objects with many optional params | Builder |
| Handle distributed transactions | Saga (Orchestration/Choreography) |
| Prevent cascading failures | Circuit Breaker (State pattern applied) |

### What Separates Senior from Staff Answers

The same question ("Tell me about Singleton") gets answered very differently depending on your target level. This table shows how depth, context, and reasoning evolve from junior to staff-level responses. Aim for the Staff column in your preparation.

| Level | How they discuss patterns |
|---|---|
| **Junior** | "Singleton has a private constructor and static getInstance()" |
| **Senior** | "I use Singleton scope via Spring DI because manual Singleton breaks testability and violates DIP" |
| **Staff** | "The question isn't 'should I use Singleton' — it's 'what lifecycle does this object need, and who manages it?' In our system, we made the config service a DI singleton but the circuit breaker is per-client because each downstream has different failure characteristics" |

The staff answer shows: architectural reasoning, production context, tradeoff awareness, and the ability to explain WHY the pattern boundary was drawn where it was.
