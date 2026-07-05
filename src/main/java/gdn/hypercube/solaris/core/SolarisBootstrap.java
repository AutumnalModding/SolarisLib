package gdn.hypercube.solaris.core;

import net.bytebuddy.agent.ByteBuddyAgent;
import java.lang.instrument.Instrumentation;
import de.florianreuth.asmfabricloader.api.event.PrePrePreLaunchEntrypoint;

public class SolarisBootstrap implements PrePrePreLaunchEntrypoint {
    public void onLanguageAdapterLaunch() {
        SolarisTransformerLoader.LOGGER.info("Spinning up...");
        SolarisTransformerLoader loader = new SolarisTransformerLoader();
        SolarisTransformerLoader.scan();

        if (SolarisTransformerLoader.MODE == SolarisTransformerLoader.TransformerMode.JAVA_AGENT) {
            boolean success = false;
            SolarisTransformerLoader.LOGGER.debug("Attempting to load ByteBuddy...");
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
                SolarisTransformerLoader.LOGGER.error("Failed to initialize agent with the standard method. Trying JNA. This might crash on Windows!");
                try {
                    java.lang.Class.forName("com.sun.jna.Native");
                    Instrumentation agent = ByteBuddyAgent.install(ByteBuddyAgent.AttachmentProvider.ForEmulatedAttachment.INSTANCE);
                    agent.addTransformer(loader, true);
                    success = true;
                } catch (ExceptionInInitializerError | IllegalStateException | UnsatisfiedLinkError |
                         ClassNotFoundException failure) {
                    SolarisTransformerLoader.oopsie(SolarisTransformerLoader.LOGGER, "Failed to initialize agent - here be dragons!", failure);
                }
            } catch (NoClassDefFoundError ignored) {
                SolarisTransformerLoader.LOGGER.warn("Failed to actually load ByteBuddy. Falling back to mixin-mode.");
                SolarisTransformerLoader.MODE = SolarisTransformerLoader.TransformerMode.MIXIN_ONLY;
            }
            if (success) SolarisTransformerLoader.LOGGER.info("Solaris initialized successfully!");
        }
    }
}
