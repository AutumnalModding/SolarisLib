package de.florianreuth.asmfabricloader.api.event;

// mom can we've AFL? no, we've AFL at home
public interface PrePrePreLaunchEntrypoint {
    void onLanguageAdapterLaunch();

    static String getEntrypointName() {
        return "afl:prePrePreLaunch";
    }
}
