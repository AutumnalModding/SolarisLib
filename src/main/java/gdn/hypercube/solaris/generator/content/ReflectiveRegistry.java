package gdn.hypercube.solaris.generator.content;

import gdn.hypercube.solaris.api.Registrar;
import gdn.hypercube.solaris.core.SolarisTransformerLoader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public abstract class ReflectiveRegistry<T> implements Registrar<T> {
    protected final Class<T> type; // for caches
    public final Registry<T> registry;
    protected final String mod;
    protected final Map<String, T> contents = new LinkedHashMap<>();
    protected BiConsumer<String, T> registrar;

    @SuppressWarnings("unchecked")
    protected ReflectiveRegistry(String mod) {
        this.mod = mod;
        Registry<T> target = null;
        ParameterizedType parameterized = (ParameterizedType) getClass().getGenericSuperclass();
        this.type = (Class<T>) parameterized.getActualTypeArguments()[0];
        try {
            Field registry = Registries.class.getField(this.type.getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
            target = (Registry<T>) registry.get(null);
        } catch (ReflectiveOperationException exception) {
            SolarisTransformerLoader.oopsie(RegistryInitializer.LOGGER, "FAILED INITIALIZING REGISTRY FOR CLASS: " + this.type.getCanonicalName(), exception);
        }

        this.registry = target;
        this.registrar = (name, obj) -> {
            Registry.register(registry, Identifier.of(this.mod, name), obj);
            RegistryInitializer.LOGGER.debug("Registered {} {}", this.type.getCanonicalName(), this.mod + ":" + name);
        };
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
            this.registrar.accept(name, obj);
            RegistryInitializer.LOGGER.debug("Registered {} {}", this.type.getCanonicalName(), this.mod + ":" + name);
        });
    }

    @Override
    public Class<T> type() {
        return this.type;
    }
}

