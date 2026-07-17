package gdn.hypercube.solaris.util;

import gdn.hypercube.solaris.data.Priority;
import java.lang.reflect.InvocationTargetException;
import org.apache.logging.log4j.Logger;

public class MiscHelpers {
    public static int prioritize(Class<?> clazz) {
        Priority p = clazz.getAnnotation(Priority.class);
        return p != null ? p.value() : 0;
    }

    public static void oopsie(Logger logger, String message, Throwable cause) {
        Throwable actual = cause instanceof InvocationTargetException ? cause.getCause() : cause;
        logger.fatal("/// SOMEBODY SET US UP THE BOMB ///");
        logger.fatal(message);
        if (cause != null) {
            logger.fatal("CAUSE: {}: {}", prettify(actual), actual.getMessage());
            Throwable root = actual.getCause();
            logger.fatal("ROOT CAUSE: {}: {}", (root == null ? "N/A" : prettify(root)), (root == null ? "N/A" : root.getMessage()));
            logger.fatal("DUMPING STACKTRACE...");
            for (StackTraceElement element : actual.getStackTrace()) {
                logger.fatal(element.toString());
            }
        }
        logger.fatal("/// SOMEBODY SET US UP THE BOMB ///");
    }

    public static String prettify(Throwable target) {
        return target.getClass().getName().replaceAll("\\[", "");
    }
}
