package gdn.hypercube.solaris.generator.content;

import gdn.hypercube.solaris.core.ClasspathScanning;
import gdn.hypercube.solaris.core.SolarisBootstrap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryInitializer implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Registry Manager");
    private static final Map<Class<?>, ReflectiveRegistry<?>> REGISTRIES = new HashMap<>();

    @Override
    @SuppressWarnings("rawtypes")
    public void onInitialize() {
        LOGGER.info("Scanning and loading registries...");
        List<Class<ReflectiveRegistry>> registries = ClasspathScanning.implementations(ReflectiveRegistry.class, true);
        registries.forEach(clazz -> {
            try {
                LOGGER.debug("Initializing registrar {}", clazz.getSimpleName());
                Constructor<ReflectiveRegistry> constructor = clazz.getConstructor();
                constructor.setAccessible(true);
                ReflectiveRegistry registrar = constructor.newInstance();
                registrar.init();
                REGISTRIES.put(registrar.type, registrar);
            } catch (InvocationTargetException exception) {
                SolarisBootstrap.oopsie(LOGGER, "FAILED INITIALIZING REGISTRAR: " + clazz.getSimpleName(), exception.getCause());
            } catch (ReflectiveOperationException exception) {
                SolarisBootstrap.oopsie(LOGGER, "FAILED INITIALIZING REGISTRAR: " + clazz.getSimpleName(), exception);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> ReflectiveRegistry<T> get(Class<T> type) {
        return (ReflectiveRegistry<T>) REGISTRIES.get(type);
    }
}
