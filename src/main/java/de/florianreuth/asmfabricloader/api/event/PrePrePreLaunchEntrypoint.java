package de.florianreuth.asmfabricloader.api.event;

import gdn.hypercube.solaris.data.UsedImplicitly;

// mom can we've AFL? no, we've AFL at home
public interface PrePrePreLaunchEntrypoint {
    @UsedImplicitly void onLanguageAdapterLaunch();
    @UsedImplicitly static String getEntrypointName() { return "afl:prePrePreLaunch"; }
}
