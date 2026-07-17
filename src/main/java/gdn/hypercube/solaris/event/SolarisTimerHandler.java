package gdn.hypercube.solaris.event;

import gdn.hypercube.solaris.api.TimerKey;
import gdn.hypercube.solaris.api.Triple;
import gdn.hypercube.solaris.util.MiscHelpers;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({"rawtypes", "CodeBlock2Expr"})
public class SolarisTimerHandler implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Timers");
    private static final Map<TimerKey, Triple<Long, Integer, Consumer<MinecraftServer>>> TIMERS = new LinkedHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TIMERS.forEach((key, timer) -> {
                timer.setLeft(timer.getLeft()-1);
                if (timer.getLeft() == 0L) {
                    timer.setLeft(-1L);
                    try {
                        timer.getRight().accept(server);
                        timer.setMiddle(timer.getMiddle() + 1);
                    } catch (Throwable throwable) {
                        LOGGER.warn("Timer {} failed to execute - check debug logs.", key.raw());
                        LOGGER.debug("Cause: {}: {}", MiscHelpers.prettify(throwable), throwable.getMessage());
                        Throwable root = throwable.getCause();
                        LOGGER.debug("Root cause: {}: {}", (root == null ? "N/A" : MiscHelpers.prettify(root)), (root == null ? "N/A" : root.getMessage()));
                        LOGGER.debug("Stacktrace;");
                        for (StackTraceElement element : throwable.getStackTrace()) {
                            LOGGER.debug(element.toString());
                        }
                    }
                }
            });
        });
    }

    public static void add(TimerKey key, long time, Consumer<MinecraftServer> runner) {
        TIMERS.put(key, new Triple<>(time, 0, runner));
    }

    public static void reset(TimerKey key, long time) {
        Triple<Long, Integer, Consumer<MinecraftServer>> timer = TIMERS.get(key);
        timer.setLeft(time);
    }

    public static boolean active(TimerKey key) {
        return TIMERS.get(key).getLeft() > 0L;
    }

    public static int count(TimerKey key) {
        return TIMERS.get(key).getMiddle();
    }

    public static void kill(TimerKey key) {
        TIMERS.remove(key);
    }
}
