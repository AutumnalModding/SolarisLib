package gdn.hypercube.solaris.generator.content;

import java.lang.reflect.ParameterizedType;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class DualRegistry<T, C> extends ReflectiveRegistry<T> {
    protected final Class<C> companion;
    protected final BiFunction<String, T, C> izer;

    @SuppressWarnings("unchecked")
    protected DualRegistry(String mod, BiFunction<String, T, C> izer) {
        super(mod);
        this.izer = izer;
        ParameterizedType parameterized = (ParameterizedType) getClass().getGenericSuperclass();
        this.companion = (Class<C>) parameterized.getActualTypeArguments()[1];
    }

    @Override
    public T create(String name, Supplier<T> input) {
        T result = input.get();
        this.contents.put(name, result);
        RegistryInitializer.get(this.companion).create(name, () -> this.izer.apply(name, result));
        return result;
    }

    public T create(String name, Supplier<T> input, BiFunction<String, T, C> izer) {
        T result = input.get();
        this.contents.put(name, result);
        RegistryInitializer.get(this.companion).create(name, () -> izer.apply(name, result));
        return result;
    }
}
