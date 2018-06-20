package Util;

public final class ThreadSafeBoolean {

    private boolean value;
    
    public ThreadSafeBoolean(boolean v) {
        value = v;
    }
    
    public synchronized boolean get() {
        return value;
    }
    
    public synchronized void set(boolean v) {
        value = v;
    }
    
    public synchronized void invert() {
        value = !value;
    }
    
    @Override
    public synchronized String toString() {
        return value ? "true" : "false";
    }
    
    @Override
    public synchronized boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ThreadSafeBoolean)) {
            return false;
        }
        return value == ((ThreadSafeBoolean) obj).value;
    }

    @Override
    public synchronized int hashCode() {
        return value ? 1 : 0;
    }
}