package gdn.hypercube.solaris.generator.content;

import java.util.Map;
import java.util.function.Supplier;

public interface Registrar<T> {
    Map<String, T> contents();
    T create(String name, Supplier<T> input);
    default void init() {}
}
