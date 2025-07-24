package access;

/**
 * Internal singleton holder for the {@link JpaAccess} instance.
 *
 * <p>
 * This utility class manages the global {@code JpaAccess} instance, enabling central access throughout
 * the library. Direct usage by application code is discouraged; instead, use the {@link JpaAccess}
 * class, which handles registration and retrieval as needed.
 * </p>
 *
 * <p>
 * Not intended to be instantiated or used directly by end users.
 * </p>
 *
 * @author Felix Seggebäing
 */
public final class JpaAccessHolder {
    private static JpaAccess instance;
    
    /**
     * Sets the global {@link JpaAccess} instance used by the library.
     *
     * <p>
     * Intended for internal use only. Typically called by {@link JpaAccess} during initialization.
     * </p>
     *
     * @param provider the {@code JpaAccess} instance to register
     */
    public static void set(JpaAccess provider) {
        instance = provider;
    }
    
    /**
     * Returns the globally registered {@link JpaAccess} instance.
     *
     * @return the registered {@code JpaAccess} instance
     * @throws IllegalStateException if the instance has not been initialized
     */
    public static JpaAccess get() {
        if (instance == null)
            throw new IllegalStateException("JpaAccess not initialized! Either register JpaAccess as a spring bean or call static create() in class manually!");
        return instance;
    }
}
