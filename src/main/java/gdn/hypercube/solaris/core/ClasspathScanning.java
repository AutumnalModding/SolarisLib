package gdn.hypercube.solaris.core;

import gdn.hypercube.solaris.util.MiscHelpers;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClasspathScanning {
    private static final HashMap<Class<?>, List<Class<?>>> CACHE = new HashMap<>();
    public static final Logger LOGGER = LogManager.getLogger("Solaris Classpath Scanner");

    @SuppressWarnings("unchecked")
    public static <T> List<Class<T>> implementations(Class<T> clazz, boolean concrete) {
        boolean verbose = System.getProperty("solaris.verboseClasspathScanning") != null;
        LOGGER.debug("Scanning classpath for {}implementations of {}...", (concrete ? "concrete " : ""), clazz.getCanonicalName());
        ClassLoader loader = clazz.getClassLoader();
        if (!CACHE.containsKey(clazz)) {
            ClassGraph graph = new ClassGraph()
                .enableClassInfo()
                .overrideClassLoaders(loader)
                .rejectPackages("java.*")
                .rejectPackages("javax.*")
                .rejectPackages("sun.*")
                .rejectPackages("com.sun.*")
                .rejectPackages("com.azul.*")
                .rejectPackages("jdk.*")
                .rejectPackages("org.spongepowered.*")
                .rejectPackages("org.objectweb.*")
                .rejectPackages("org.lwjgl.*")
                .rejectPackages("org.apache.*")
                .rejectPackages("gnu.trove.*")
                .rejectPackages("com.ibm.*")
                .rejectPackages("io.netty.*")
                .rejectPackages("com.google.*")
                .rejectPackages("scala.*")
                .rejectPackages("net.bytebuddy.*")
                .rejectPackages("kotlin.*");

            if (verbose) {
                graph.verbose();
                graph.enableRealtimeLogging();
            }

            ScanResult result = graph.scan();
            ClassInfoList classes = (concrete ? result.getSubclasses(clazz) : result.getClassesImplementing(clazz))
            .filter(info -> !concrete || (info.isStandardClass() && !info.isAbstract()));
            List<Class<T>> loaded = new ArrayList<>();
            for (ClassInfo target : classes) {
                LOGGER.debug("Found class {}. Loading it.", target.getName());
                try {
                    Class<T> that = (Class<T>) Class.forName(target.getName(), true, loader);
                    loaded.add(that);
                } catch (ReflectiveOperationException | IllegalArgumentException exception) {
                    SolarisTransformerLoader.oopsie(LOGGER, "FAILED LOADING CLASS: " + target.getName(), exception);
                }
            }
            LOGGER.debug("Found {} total {}implementations.", loaded.size(), (concrete ? "concrete " : ""));
            __STORE(clazz, loaded);
            result.close();
        }

        return __LOAD(clazz);
    }

    private static <T> void __STORE(Class<T> superclass, List<Class<T>> implementations) {
        CACHE.put(superclass, new ArrayList<>(implementations));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<Class<T>> __LOAD(Class<T> superclass) {
        return (List<Class<T>>) (List<?>) CACHE.get(superclass);
    }

    public static <T> void prioritize(List<Class<T>> target) {
        target.sort(Comparator.comparingInt(MiscHelpers::prioritize));
    }
}