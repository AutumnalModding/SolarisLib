package gdn.hypercube.solaris.core;

import gdn.hypercube.solaris.util.ChainedList;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class SolarisTransformerLoader implements ClassFileTransformer, IMixinConfigPlugin {
    private static final Map<String, byte[]> CACHE = new HashMap<>();
    private static boolean CASCADING = false;
    static final ClassLoader LOADER = SolarisTransformerLoader.class.getClassLoader();
    static final Map<String, ChainedList<Class<? extends SolarisTransformer>>> TRANSFORMERS = new LinkedHashMap<>();
    static final Map<String, ChainedList<Class<? extends SolarisTransformer>>> SUPERPATCHERS = new LinkedHashMap<>();

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] bytes) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(node, 0);
        name = reader.getClassName();

        if ((node.access & Opcodes.ACC_INTERFACE) != 0) return bytes;
        if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Loading: {}", name);

        if (TRANSFORMERS.containsKey(name)) {
            ArrayList<Class<? extends SolarisTransformer>> transformers = TRANSFORMERS.get(name).arrayify();
            bytes = transform(name, transformers, bytes, false);
        }

        if (SUPERPATCHERS.containsKey(node.superName)) { // TODO: resolve chain
            ArrayList<Class<? extends SolarisTransformer>> transformers = SUPERPATCHERS.get(name).arrayify();
            bytes = transform(node.superName, transformers, bytes, true);
        }

        List<String> interfaces = new ArrayList<>(node.interfaces);
        for (String iface : interfaces) {
            if (TRANSFORMERS.containsKey(iface)) {
                ArrayList<Class<? extends SolarisTransformer>> transformers = TRANSFORMERS.get(name).arrayify();
                bytes = transform(iface, transformers, bytes, true);
            }
        }

        if (SolarisBootstrap.DEBUG) {
            Path root = Paths.get(".classes");
            Path dump = root.resolve(name + ".class").normalize();
            if (!dump.normalize().startsWith(root.normalize())) {
                SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "REFUSING TO DUMP CLASS OUTSIDE ROOT: " + name, null);
            }
            try {
                Files.createDirectories(dump.getParent());
                Files.write(dump, bytes);
            } catch (IOException exception) {
                SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "FAILED DUMPING CLASS: " + name, exception);
            }
        }

        if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.trace("Storing class {} in cache.", name);
        CACHE.put(name, bytes);
        return bytes;
    }

    private byte[] transform(String name, List<Class<? extends SolarisTransformer>> transformers, byte[] bytes, boolean overrides) {
        try {
            boolean modified = false;
            for (Class<? extends SolarisTransformer> transformer : transformers) {
                SolarisBootstrap.LOGGER.debug("Transforming {} with transformer {}", name, transformer.getSimpleName());
                SolarisTransformer instance = transformer.getDeclaredConstructor().newInstance();
                ArrayList<String> methods = new ArrayList<>();
                Arrays.stream(transformer.getDeclaredMethods()).iterator().forEachRemaining(method -> methods.add(method.getName()));

                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(bytes);
                reader.accept(node, 0);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

                Set<String> existing = new HashSet<>();
                for (MethodNode method : node.methods) {
                    existing.add(method.name + method.desc);
                }

                if (methods.contains("solaris$metadata")) modified |= invoke(transformer, instance, node, null, "solaris$metadata");

                if (overrides) {
                    modified |= superpatch(node, methods, existing);
                }

                for (MethodNode method : node.methods) {
                    String target = sanitize(method.name);
                    if (methods.contains(target) && (method.access & Opcodes.ACC_ABSTRACT) == 0) {
                        if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Found method {}{}", method.name, method.desc);
                        modified |= invoke(transformer, instance, node, method, target);
                    }
                }

                if (modified) {
                    if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Modified class {}", name);
                    node.accept(writer);
                    bytes = writer.toByteArray();
                }
            }
        } catch (Throwable exception) { // this is bad practice but fuck it
            if (!CASCADING) {
                CASCADING = true;
                String target = exception.getMessage().replace("/", ".");
                switch (exception) {
                    case NoClassDefFoundError _ ->
                            bytes = retransform(target, name, transformers, bytes, overrides);
                    case RuntimeException runtime when runtime.getMessage().contains("ClassNotFoundException") -> {
                        target = runtime.getMessage().replace("/", ".").replace("java.lang.ClassNotFoundException: ", "");
                        bytes = retransform(target, name, transformers, bytes, overrides);
                    }
                    default -> {
                    }
                }
            } else {
                SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "FAILED TRANSFORMING CLASS: " + name, exception);
            }
        }

        return bytes;
    }

    private String sanitize(String name) {
        String target = switch(name) {
            case "for", "int", "do", "void", "float", "double",
                 "switch", "case", "default", "if", "boolean", "else",
                 "interface", "class", "enum", "while", "true", "false",
                 "public", "private", "protected", "try", "catch", "finally",
                 "long", "byte", "short", "char", "abstract", "import",
                 "package", "static", "null", "new", "throw", "return",
                 "break", "continue", "throws", "extends", "implements",
                 "super", "this", "final", "native", "strictfp",
                 "synchronized", "transient", "volatile", "instanceof",
                 "assert", "goto", "const", "var", "yield", "record",
                 "sealed", "permits" -> "__" + name;
            default -> name;
        };

        target = target
                .replace("<", "__")
                .replace(">", "__");

        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            target = "__" + name;
        }

        if (name.isEmpty()) {
            target = "__empty";
        }

        return target;
    }

    private boolean superpatch(ClassNode node, List<String> targets, Set<String> existing) {
        boolean modified = false;

        try {
            String current = node.superName;

            while (current != null && !current.equals("java/lang/Object")) {
                ClassNode that = new ClassNode();
                byte[] target = CACHE.get(current);
                ClassReader there = new ClassReader(target);
                there.accept(that, 0);

                for (MethodNode override : that.methods) {
                    String signature = override.name + override.desc;

                    if (existing.contains(signature)) {
                        continue;
                    }

                    if (override.name.equals("<init>") ||
                            override.name.equals("<clinit>") ||
                            (override.access & Opcodes.ACC_PRIVATE) != 0 ||
                            (override.access & Opcodes.ACC_STATIC) != 0 ||
                            (override.access & Opcodes.ACC_FINAL) != 0
                    ) {
                        continue;
                    }

                    String normalized = sanitize(override.name);

                    if (targets.contains(normalized)) {
                        if (SolarisBootstrap.DEBUG) {
                            SolarisBootstrap.LOGGER.debug("Adding override for superclass method {}{} from {}",
                                    override.name, override.desc, current);
                        }

                        MethodNode method = new MethodNode(
                                override.access & ~Opcodes.ACC_ABSTRACT,
                                override.name,
                                override.desc,
                                override.signature,
                                override.exceptions.toArray(new String[0])
                        );

                        node.methods.add(method);
                        existing.add(signature);
                    }
                }

                current = that.superName;
            }

        } catch (Exception e) {
            SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER,
                    "Failed to process superclass methods for " + node.name, e);
        }

        return modified;
    }

    private byte[] retransform(String target, String name, List<Class<? extends SolarisTransformer>> transformers, byte[] bytes, boolean overrides) {
        try {
            LOADER.loadClass(target);
            CASCADING = false;
            bytes = transform(name, transformers, bytes, overrides);
        } catch (ClassNotFoundException ignored) {
            CASCADING = false;
        }
        return bytes;
    }

    /** Log Transformer Registration */
    static void LTR(String target, ChainedList<Class<? extends SolarisTransformer>> transformers) {
        for (Class<? extends SolarisTransformer> clazz : transformers.arrayify()) {
            SolarisBootstrap.LOGGER.debug("Registered class transformer {} targeting {}!", clazz.getSimpleName(), target);
        }
    }

    static void parseTransformer(Class<? extends SolarisTransformer> clazz) {
        try {
            Constructor<? extends SolarisTransformer> constructor = clazz.getConstructor();
            SolarisTransformer transformer = constructor.newInstance();
            String target = transformer.internal$transformerTarget();
            ChainedList<Class<? extends SolarisTransformer>> transformers = clazz.isAssignableFrom(SolarisTransformer.Global.class) ? SUPERPATCHERS.getOrDefault(target, new ChainedList<>()) : TRANSFORMERS.getOrDefault(target, new ChainedList<>());
            if (transformer instanceof SolarisTransformer.Global) SUPERPATCHERS.put(target, transformers.add(clazz));
            else TRANSFORMERS.put(target, transformers.add(clazz));
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException exception) {
            SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "FAILED LOADING CLASS TRANSFORMER: " + clazz.getSimpleName(), exception);
        }
    }

    private static boolean invoke(Class<? extends SolarisTransformer> transformer, Object instance, ClassNode clazz, @Nullable MethodNode method, String target) {
        try {
            Method patcher = transformer.getDeclaredMethod(target, method == null ? ClassNode.class : SolarisTransformer.TargetData.class);
            patcher.setAccessible(true);

            Long hash = null;
            Long node = null;
            if (method != null) {
                hash = compute(method.instructions);
                if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Modfiying target method {}{}", method.name, method.desc);
            } else {
                node = compute(clazz);
                if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Modfiying class metadata");
            }

            patcher.invoke(instance, method != null ? new SolarisTransformer.TargetData(clazz, method) : clazz);
            boolean modified;
            if (hash != null) {
                modified = hash != compute(method.instructions);
                if (modified && SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Target method {}{} modified successfully", method.name, method.desc);
                else if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Did not modify {}{}.", method.name, method.desc);
            } else {
                modified = node != compute(clazz);
                if (modified && SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Class metadata modified successfully");
                else if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Did not modify class metadata.");
            }
            return modified;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException exception) {
            SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "FAILED TRANSFORMING " + (method == null ? "CLASS METADATA" : "METHOD: " + method.name), exception);
        }

        return false;
    }

    private static long compute(InsnList list) {
        long hash = list.size();
        for (AbstractInsnNode node : list) {
            hash = 31 * hash + node.getType();
            hash = 31 * hash + node.getOpcode();

            switch (node) {
                case MethodInsnNode method -> {
                    hash = 31 * hash + method.owner.hashCode();
                    hash = 31 * hash + method.name.hashCode();
                    hash = 31 * hash + method.desc.hashCode();
                }

                case JumpInsnNode jump -> hash = 31 * hash + list.indexOf(jump.label);
                case VarInsnNode var -> hash = 31 * hash + var.var;
                case TypeInsnNode type -> hash = 31 * hash + type.desc.hashCode();

                case FieldInsnNode field -> {
                    hash = 31 * hash + field.owner.hashCode();
                    hash = 31 * hash + field.name.hashCode();
                    hash = 31 * hash + field.desc.hashCode();
                }

                case LdcInsnNode ldc -> hash = 31 * hash + (ldc.cst != null ? ldc.cst.hashCode() : 0);

                default -> {}
            }
        }
        return hash;
    }

    private static long compute(ClassNode node) {
        long hash = node.access;
        hash = 31 * hash + node.name.hashCode();
        hash = 31 * hash + (node.superName != null ? node.superName.hashCode() : 0);
        hash = 31 * hash + node.interfaces.hashCode();

        for (MethodNode method : node.methods) {
            hash = 31 * hash + method.name.hashCode();
            hash = 31 * hash + method.desc.hashCode();
            hash = 31 * hash + compute(method.instructions);
        }

        for (FieldNode field : node.fields) {
            hash = 31 * hash + field.name.hashCode();
            hash = 31 * hash + field.desc.hashCode();
        }

        return hash;
    }

    @Override public void onLoad(String location) {
        if (SolarisBootstrap.MODE == SolarisBootstrap.TransformerMode.MIXIN_ONLY) SolarisBootstrap.LOGGER.info("Loading Solaris in mixin-mode...");
    }

    @Override public List<String> getMixins() { return null; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> possible, Set<String> additional) {}
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }

    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (SolarisBootstrap.MODE == SolarisBootstrap.TransformerMode.MIXIN_ONLY) {
            if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Found potential target: {}", node.name);
            if (TRANSFORMERS.containsKey(node.name)) {
                try {
                    ArrayList<Class<? extends SolarisTransformer>> transformers = TRANSFORMERS.get(node.name).arrayify();
                    for (Class<? extends SolarisTransformer> transformer : transformers) {
                        SolarisTransformer instance = transformer.getDeclaredConstructor().newInstance();
                        ArrayList<String> methods = new ArrayList<>();
                        Arrays.stream(transformer.getDeclaredMethods()).iterator().forEachRemaining(method -> methods.add(method.getName()));

                        if (methods.contains("solaris$metadata")) {
                            Method patcher = transformer.getDeclaredMethod("solaris$metadata", ClassNode.class);
                            patcher.setAccessible(true);
                            patcher.invoke(instance, node);
                            if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Class metadata modified");
                        }

                        for (MethodNode method : node.methods) {
                            String sanitized = sanitize(method.name);
                            if (methods.contains(sanitized) && (method.access & Opcodes.ACC_ABSTRACT) == 0) {
                                if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Found method {}{}", method.name, method.desc);
                                Method patcher = transformer.getDeclaredMethod(sanitized, SolarisTransformer.TargetData.class);
                                patcher.setAccessible(true);
                                patcher.invoke(instance, new SolarisTransformer.TargetData(node, method));
                                if (SolarisBootstrap.DEBUG) SolarisBootstrap.LOGGER.debug("Target method {}{}", method.name, method.desc);
                            }
                        }
                    }
                } catch (ReflectiveOperationException exception) {
                    SolarisBootstrap.oopsie(SolarisBootstrap.LOGGER, "FAILED TRANSFORMING CLASS IN MIXIN MODE: " + target, exception);
                }
            }
        }
    }

    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}