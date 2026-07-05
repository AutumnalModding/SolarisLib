package gdn.hypercube.solaris.generator.content;

import gdn.hypercube.solaris.core.ClasspathScanning;
import gdn.hypercube.solaris.core.SolarisTransformerLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryInitializer implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Registry Manager");
    private static final Map<Class<?>, ReflectiveRegistry<?>> REGISTRIES = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("rawtypes")
    public void onInitialize() {
        LOGGER.info("Scanning and loading registries...");
        List<Class<ReflectiveRegistry>> registries = ClasspathScanning.implementations(ReflectiveRegistry.class, true);
        ClasspathScanning.prioritize(registries);
        registries.forEach(clazz -> {
            try {
                Constructor<ReflectiveRegistry> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                ReflectiveRegistry registrar = constructor.newInstance();
                REGISTRIES.put(registrar.type, registrar);
                LOGGER.debug("Initialized registry {} (for type {})", clazz.getCanonicalName(), registrar.type.getCanonicalName());
            } catch (InvocationTargetException exception) {
                SolarisTransformerLoader.oopsie(LOGGER, "FAILED INITIALIZING REGISTRAR: " + clazz.getSimpleName(), exception.getCause());
            } catch (ReflectiveOperationException exception) {
                SolarisTransformerLoader.oopsie(LOGGER, "FAILED INITIALIZING REGISTRAR: " + clazz.getSimpleName(), exception);
            }
        });
        REGISTRIES.forEach((_, registry) -> registry.init());
    }

    @SuppressWarnings("unchecked")
    public static <T> ReflectiveRegistry<T> get(Class<T> type) {
        return (ReflectiveRegistry<T>) REGISTRIES.get(type);
    }
}
