package access;

public final class JpaAccessHolder {
    private static JpaAccess instance;
    
    public static void set(JpaAccess provider) {
        instance = provider;
    }
    
    public static JpaAccess get() {
        if (instance == null)
            throw new IllegalStateException("JpaAccess not initialized! Either register JpaAccess as a spring bean or call static create() in class manually!");
        return instance;
    }
}
