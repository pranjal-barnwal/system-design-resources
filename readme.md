# System Design

## Design-Patterns reference matrix

| Pattern | Category | One-Line Purpose | Core Mechanism | SOLID Principle | Confused With | Killer Example | Key Tradeoff | Modern Alternative | Freq |
|---------|----------|-----------------|---------------|----------------|---------------|---------------|-------------|-------------------|------|
| [Singleton](#singleton) | Creational | One instance, global access | Private ctor + static accessor | — | Service Locator | Spring bean scope | Global access ↔ Hidden coupling, testability | DI container scoping | Very High |
| [Factory Method](#factory-method) | Creational | Delegate instantiation to subclasses | Inheritance + polymorphic create | OCP, DIP | Abstract Factory, Static Factory | `Collection.iterator()` | Extensibility ↔ Parallel class hierarchies | Lambda/Supplier | Very High |
| [Abstract Factory](#abstract-factory) | Creational | Create families of related objects | Composition of factory methods | OCP, DIP | Factory Method | Cross-platform UI widgets | Family consistency ↔ Costly to add new product | Config-based factory map | High |
| [Builder](#builder) | Creational | Construct complex objects step-by-step | Fluent method chaining | SRP | Factory | `HttpRequest.newBuilder()` | Readable construction ↔ Verbose boilerplate | Kotlin DSL, records | Very High |
| [Prototype](#prototype) | Creational | Clone existing objects | Clone/copy method | — | Factory | Game engine prefabs | Cheap duplication ↔ Deep vs shallow copy | `copy()`, serialize-deserialize | Medium |
| [Adapter](#adapter-wrapper) | Structural | Make incompatible interfaces work together | Wraps adaptee, implements target | DIP, OCP | Bridge, Facade | `Arrays.asList()` | Integration ↔ One more layer to maintain | — | Very High |
| [Bridge](#bridge) | Structural | Separate abstraction from implementation | Two hierarchies + composition | OCP, SRP | Strategy, Adapter | JDBC Driver/Connection | M+N classes ↔ Upfront design complexity | — | Medium |
| [Composite](#composite) | Structural | Treat individual and group objects uniformly | Recursive tree + shared interface | OCP | Decorator | File system (File + Directory) | Uniform API ↔ Type safety loss | — | High |
| [Decorator](#decorator-wrapper) | Structural | Add behavior dynamically without subclassing | Wraps same interface, delegates | OCP, SRP | Proxy, Chain of Resp | Java I/O streams | Flexible composition ↔ Many small objects, order matters | AOP, annotations | Very High |
| [Facade](#facade) | Structural | Simplified interface to a complex subsystem | Delegates to subsystem classes | — | Adapter, Mediator | `JdbcTemplate` | Simplicity ↔ Hides power-user access | — | High |
| [Flyweight](#flyweight) | Structural | Share state to support large numbers of objects | Intrinsic (shared) + extrinsic (passed) | — | Singleton, Cache | `Integer.valueOf(127)` | Memory savings ↔ Complexity of state separation | Object pooling | Medium |
| [Proxy](#proxy) | Structural | Controlled access to another object | Same interface, controls delegation | SRP | Decorator | Spring `@Transactional` | Transparent indirection ↔ Hidden latency | Dynamic proxy, AOP | High |
| [Chain of Responsibility](#chain-of-responsibility) | Behavioral | Pass request along a chain of handlers | Linked handlers, pass-or-handle | OCP, SRP | Decorator | Express.js middleware | Decoupled handlers ↔ No guarantee of handling | Middleware pipelines | High |
| [Command](#command) | Behavioral | Encapsulate a request as an object | Action as first-class object | SRP, OCP | Strategy | DB migrations (`up`/`down`) | Undo/queue/log ↔ Indirection overhead | Lambdas, CQRS | High |
| [Iterator](#iterator) | Behavioral | Sequential access without exposing internals | Cursor + hasNext/next | SRP | — | Java enhanced for-loop | Uniform traversal ↔ Sequential only | Streams, generators | Medium |
| [Mediator](#mediator) | Behavioral | Centralize complex communication | Central hub, N connections | SRP | Observer, Facade | Redux store | Decoupled components ↔ God mediator risk | Event bus | Medium |
| [Memento](#memento) | Behavioral | Capture and restore object state | Opaque state snapshot | — | Command | Game save/checkpoint | Simple undo ↔ Memory cost of snapshots | Event sourcing | Medium |
| [Observer](#observer-publish-subscribe) | Behavioral | Notify dependents of state changes | Subject maintains subscriber list | OCP | Mediator, Pub/Sub | DOM `addEventListener` | Loose coupling ↔ Cascade/leak risks | Reactive Streams (RxJava) | Very High |
| [State](#state) | Behavioral | Alter behavior when internal state changes | Context delegates to state object | OCP, SRP | Strategy | TCP connection states | Clean state logic ↔ Class explosion per state | State machine libs (XState) | High |
| [Strategy](#strategy) | Behavioral | Swap algorithms at runtime | Context delegates to strategy | OCP, DIP | State, Template Method | `Comparator<T>` | Runtime flexibility ↔ Client must know strategies | Lambdas/functions | Very High |
| [Template Method](#template-method) | Behavioral | Define algorithm skeleton, defer steps | Inheritance + hook methods | OCP, DIP | Strategy | JUnit setUp/test/tearDown | Fixed structure ↔ Fragile base class | Strategy + composition | High |
| [Visitor](#visitor) | Behavioral | Add operations without modifying elements | Double dispatch (accept + visit) | OCP, SRP | Strategy | Compiler AST visitors | Easy new ops ↔ Hard new element types | Pattern matching | Medium |
