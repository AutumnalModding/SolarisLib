package gdn.hypercube.solaris.core;

import net.bytebuddy.agent.ByteBuddyAgent;
import java.lang.instrument.Instrumentation;
import de.florianreuth.asmfabricloader.api.event.PrePrePreLaunchEntrypoint;

public class SolarisBootstrap implements PrePrePreLaunchEntrypoint {
    public void onLanguageAdapterLaunch() {
        SolarisTransformerLoader loader = new SolarisTransformerLoader();
        SolarisTransformerLoader.scan();

        if (SolarisTransformerLoader.MODE == SolarisTransformerLoader.TransformerMode.JAVA_AGENT) {
            boolean success = false;
            SolarisTransformerLoader.LOGGER.debug("Attempting to load ByteBuddy...");
            try {
                ByteBuddyAgent.AttachmentProvider.Compound provider = new ByteBuddyAgent.AttachmentProvider.Compound(
                    ByteBuddyAgent.AttachmentProvider.ForModularizedVm.INSTANCE,
                    ByteBuddyAgent.AttachmentProvider.ForJ9Vm.INSTANCE
                );
                Instrumentation agent = ByteBuddyAgent.install(provider);
                agent.addTransformer(loader, true);
                success = true;
            } catch (LinkageError | IllegalStateException ignored) {
                SolarisTransformerLoader.LOGGER.warn("Failed to actually load ByteBuddy. Falling back to mixin-mode.");
                SolarisTransformerLoader.MODE = SolarisTransformerLoader.TransformerMode.MIXIN_ONLY;
            }
            if (success) SolarisTransformerLoader.LOGGER.info("Solaris initialized successfully!");
        }
    }
}
