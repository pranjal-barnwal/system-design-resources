public class ThreadSafeDoubleLockingSingleton {
    private static ThreadSafeDoubleLockingSingleton instance = null;

    private ThreadSafeDoubleLockingSingleton() {
        System.out.println("Singleton Constructor Called!");
    }

    /*
    :/   Double check locking..
    - the reason we check for null twice, is bcuz inspite of locking, it still could allow multiple threads to be generated, even if they'll be executed one after another
    . so the first thread that acquires the lock will create the instance, and the second thread that acquires the lock will also create another instance, because it won't check for null again after acquiring the lock
    - so we need to check for null again after acquiring the lock, to ensure that only one instance is created
     */
    public static ThreadSafeDoubleLockingSingleton getInstance() {
        if (instance == null) { // First check (no locking)
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

        System.out.println(s1 == s2);
    }
}
