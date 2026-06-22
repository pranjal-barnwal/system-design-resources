// --- Null Object Design Pattern ---
// Intent: Avoid null checks by providing a do-nothing object that implements the same interface.
// Instead of keeping null references, we use a NullObject with empty/default implementations.

// --- Abstract Class ---
abstract class AbstractClass {
    abstract void m1();
}

// --- Concrete Class (Real Implementation) ---
class ConcreteClass extends AbstractClass {
    void m1() {
        System.out.println("ConcreteClass: executing m1() with real logic");
    }
}

// --- Null Object (Empty / Default Implementation) ---
class NullObject extends AbstractClass {
    // empty or default value — safe to call, won't throw NullPointerException
    void m1() {
        // no-op
    }
}

// --- Client ---
class Client {
    private AbstractClass ac;

    public Client(AbstractClass ac) {
        this.ac = ac;
    }

    public void fun() {
        // No null check needed! ac is either ConcreteClass or NullObject
        ac.m1();
    }
}

// ============================================================
// Real-world Example: Strategy Pattern + Null Object
// "Instead of keeping null pointer for robots that can't fly,
//  we keep instance of NoFly object as IFlyStrategy in Robot."
// ============================================================

// --- IFlyStrategy (Abstract) ---
interface IFlyStrategy {
    void fly();
}

// --- Concrete Strategy: FlyWithWings ---
class FlyWithWings implements IFlyStrategy {
    public void fly() {
        System.out.println("Flying with wings!");
    }
}

// --- Null Object Strategy: NoFly ---
class NoFly implements IFlyStrategy {
    public void fly() {
        // empty — robot simply can't fly, but no NullPointerException
    }
}

// --- Robot using IFlyStrategy ---
class Robot {
    private String name;
    private IFlyStrategy flyStrategy;

    public Robot(String name, IFlyStrategy flyStrategy) {
        this.name = name;
        this.flyStrategy = flyStrategy; // Never null — use NoFly instead
    }

    public void performFly() {
        System.out.print(name + ": ");
        flyStrategy.fly();
    }
}

// --- Main ---
public class NullObjectPattern {
    public static void main(String[] args) {
        // Example 1: Basic Null Object Pattern
        System.out.println("=== Basic Null Object Pattern ===");

        AbstractClass real = new ConcreteClass();
        AbstractClass nullObj = new NullObject();

        Client client1 = new Client(real);
        Client client2 = new Client(nullObj); // Instead of passing null

        client1.fun(); // executes real logic
        client2.fun(); // does nothing, but NO NullPointerException

        // Without Null Object pattern, we'd have to do:
        // if (ac != null) { ac.m1(); }  <-- fragile, repetitive

        System.out.println();

        // Example 2: Strategy Pattern with Null Object (NoFly)
        System.out.println("=== Strategy + Null Object (Robot Example) ===");

        Robot eagle = new Robot("EagleBot", new FlyWithWings());
        Robot walker = new Robot("WalkerBot", new NoFly()); // NoFly instead of null

        eagle.performFly();   // EagleBot: Flying with wings!
        walker.performFly();  // WalkerBot: (nothing, but safe)
    }
}
