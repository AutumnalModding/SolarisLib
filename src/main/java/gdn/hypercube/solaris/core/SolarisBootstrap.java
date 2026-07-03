package gdn.hypercube.solaris.core;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.Logger;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.apache.logging.log4j.LogManager;
import java.lang.instrument.Instrumentation;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import de.florianreuth.asmfabricloader.api.event.PrePrePreLaunchEntrypoint;

public class SolarisBootstrap implements PrePrePreLaunchEntrypoint, PreLaunchEntrypoint {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Transformer System");
    static boolean SCANNED_CLASSES = false;
    public static final boolean DEBUG = Files.exists(Paths.get(".classes/")) || System.getProperty("solaris.debug") != null;
    public static final TransformerMode MODE;

    private static String pp(Throwable target) {
        return target.getClass().getName().replaceAll("\\[", "");
    }

    public static void oopsie(Logger logger, String message, Throwable cause) {
        logger.fatal("/// SOMEBODY SET US UP THE BOMB ///");
        logger.fatal(message);
        logger.fatal("CAUSE: {}: {}", pp(cause), cause.getMessage());
        Throwable root = cause.getCause();
        logger.fatal("ROOT CAUSE: {}: {}", (root == null ? "N/A" : pp(root)), (root == null ? "N/A" : root.getMessage()));
        logger.fatal("DUMPING STACKTRACE...");
        for (StackTraceElement element : cause.getStackTrace()) {
            logger.fatal(element.toString());
        }
        logger.fatal("/// SOMEBODY SET US UP THE BOMB ///");
    }

    private void scanClasspath() {
        if (!SCANNED_CLASSES) {
            LOGGER.debug("Scanning class transformers...");
            List<Class<SolarisTransformer.Class>> transformers = ClasspathScanning.implementations(SolarisTransformer.Class.class, false);
            List<Class<SolarisTransformer.Global>> globals = ClasspathScanning.implementations(SolarisTransformer.Global.class, false);
            transformers.forEach(SolarisTransformerLoader::parseTransformer);
            globals.forEach(SolarisTransformerLoader::parseTransformer);
            SolarisTransformerLoader.TRANSFORMERS.forEach(SolarisTransformerLoader::LTR);
            SolarisTransformerLoader.SUPERPATCHERS.forEach(SolarisTransformerLoader::LTR);
            SCANNED_CLASSES = true;
        }
    }

    public void onPreLaunch() {
        scanClasspath();
    }

    public void onLanguageAdapterLaunch() {
        scanClasspath();
    }

    static {
        LOGGER.debug("Bootstrapping mode...");
        MODE = switch (System.getProperty("solaris.patcher")) {
            case "mixin" -> TransformerMode.MIXIN_ONLY;
            case "none" -> TransformerMode.NONE;
            case null, default -> {
                try {
                    java.lang.Class.forName("net.bytebuddy.agent.ByteBuddyAgent");
                    yield TransformerMode.JAVA_AGENT;
                } catch (ClassNotFoundException ignored) {
                    LOGGER.warn("ByteBuddy not found, falling back to mixin-mode.");
                    yield TransformerMode.MIXIN_ONLY;
                }
            }
        };

        LOGGER.info("Spinning up...");
        SolarisTransformerLoader loader = new SolarisTransformerLoader();

        if (MODE == TransformerMode.JAVA_AGENT) {
            boolean success = false;
            try {
                ByteBuddyAgent.AttachmentProvider.Compound provider = new ByteBuddyAgent.AttachmentProvider.Compound(
                        ByteBuddyAgent.AttachmentProvider.ForModularizedVm.INSTANCE,
                        ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.JVM_ROOT,
                        ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.JDK_ROOT,
                        ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.MACINTOSH,
                        ByteBuddyAgent.AttachmentProvider.ForUserDefinedToolsJar.INSTANCE
                ); // <-- DEFAULT, minus JNA.
                Instrumentation agent = ByteBuddyAgent.install(provider);
                agent.addTransformer(loader, true);
                success = true;
            } catch (ExceptionInInitializerError | IllegalStateException error) {
                LOGGER.error("Failed to initialize agent with the standard method. Trying JNA. This might crash on Windows!");
                try {
                    java.lang.Class.forName("com.sun.jna.Native");
                    Instrumentation agent = ByteBuddyAgent.install(ByteBuddyAgent.AttachmentProvider.ForEmulatedAttachment.INSTANCE);
                    agent.addTransformer(loader, true);
                    success = true;
                } catch (ExceptionInInitializerError | IllegalStateException | UnsatisfiedLinkError |
                         ClassNotFoundException failure) {
                    oopsie(LOGGER, "Failed to initialize agent - here be dragons!", failure);
                }
            }
            if (success) LOGGER.info("Solaris initialized successfully!");
        }
    }

    public enum TransformerMode {
        NONE,
        MIXIN_ONLY,
        JAVA_AGENT
    }
}
