package gdn.hypercube.solaris.util;

public class MiscHelpers {
    public static int prioritize(Class<?> clazz) {
        Priority p = clazz.getAnnotation(Priority.class);
        return p != null ? p.value() : 0;
    }
}
