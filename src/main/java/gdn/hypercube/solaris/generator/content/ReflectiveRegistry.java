package gdn.hypercube.solaris.generator.content;

import gdn.hypercube.solaris.core.SolarisBootstrap;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public abstract class ReflectiveRegistry<T> implements Registrar<T> {
    final Class<T> type; // for caches
    public final Registry<T> registry;
    protected final String mod;
    protected final Map<String, T> contents = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected ReflectiveRegistry(String mod) {
        this.mod = mod;
        Registry<T> target = null;
        ParameterizedType parameterized = (ParameterizedType) getClass().getGenericSuperclass();
        this.type = (Class<T>) parameterized.getActualTypeArguments()[0];
        try {
            Field registry = Registries.class.getField(this.type.getSimpleName().toUpperCase());
            target = (Registry<T>) registry.get(null);
        } catch (ReflectiveOperationException exception) {
            SolarisBootstrap.oopsie(RegistryInitializer.LOGGER, "FAILED INITIALIZING REGISTRY FOR CLASS: " + this.type.getCanonicalName(), exception);
        }

        this.registry = target;
    }

    @Override
    public Map<String, T> contents() {
        return new HashMap<>(this.contents);
    }

    @Override
    public T create(String name, Supplier<T> input) {
        T result = input.get();
        this.contents.put(name, result);
        return result;
    }

    @Override
    public void init() {
        this.contents.forEach((name, obj) -> {
            Registry.register(registry, Identifier.of(this.mod, name), obj);
            RegistryInitializer.LOGGER.debug("Registered {} {}", this.registry.getClass().getCanonicalName(), this.mod + ":" + name);
        });
    }
}

