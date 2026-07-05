package gdn.hypercube.solaris.api;

import java.util.Map;
import java.util.function.Supplier;

public interface Registrar<T> {
    Class<T> type();
    Map<String, T> contents();
    T create(String name, Supplier<T> input);
    default void init() {}
}
