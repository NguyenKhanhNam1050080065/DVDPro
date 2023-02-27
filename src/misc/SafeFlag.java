package misc;

import java.util.concurrent.atomic.AtomicBoolean;

public class SafeFlag {
    private final AtomicBoolean atomic_bool;

    public SafeFlag(){
        atomic_bool = new AtomicBoolean();
        atomic_bool.set(false);
    }
    public SafeFlag(boolean starting_value){
        atomic_bool = new AtomicBoolean();
        atomic_bool.set(starting_value);
    }
    public void set(){
        atomic_bool.set(true);
    }
    public void clear(){
        atomic_bool.set(false);
    }
    public boolean get()  {
        return atomic_bool.get();
    }
    public void customize(boolean new_value){
        atomic_bool.set(new_value);
    }
}
