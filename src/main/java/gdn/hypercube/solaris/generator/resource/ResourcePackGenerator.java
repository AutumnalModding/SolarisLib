package gdn.hypercube.solaris.generator.resource;

import gdn.hypercube.solaris.api.ModelGenerator;
import gdn.hypercube.solaris.core.ClasspathScanning;
import gdn.hypercube.solaris.core.SolarisTransformerLoader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ResourcePackGenerator implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Asset Generator");
    public static final List<String> TARGETS = new ArrayList<>();

    private static final List<String> TYPES = new ArrayList<>();
    private static final Map<String, ModelGenerator> GENERATORS = new HashMap<>();

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        try {
            FileUtils.deleteDirectory(Path.of("resourcepacks/solaris").toFile());
            Path.of("resourcepacks/solaris/").toFile().mkdirs();
            String PACK_METADATA = """
            {
              "pack": {
                "description": "Solaris's dynamic resource pack",
                "min_format": 80,
                "max_format": 200""" /* who cares, noone's gonna use this outside of 26.1 */ + """
              \n\s\s}
            }
            """;
            Files.writeString(Path.of("resourcepacks/solaris/pack.mcmeta"), PACK_METADATA);
        } catch (IOException ignored) {}
        TARGETS.forEach(mod -> {
            LOGGER.debug("Scanning mod '{}' for possible textures...", mod);
            ModContainer container = loader.getModContainer(mod).orElseThrow();
            TYPES.forEach(type -> Path.of("resourcepacks/solaris/assets/" + mod + "/" + type).toFile().mkdirs());
            container.getOrigin().getPaths().forEach(path -> {
                Path root = path.resolve("assets/" + mod + "/textures/solaris");
                try (FileSystem fs = (path.toString().endsWith(".jar")) ? FileSystems.newFileSystem(path, (ClassLoader) null) : null) {
                    Path textures = (fs != null) ? fs.getPath("/assets/" + mod + "/textures/solaris") : root;
                    if (!Files.isDirectory(textures)) {
                        return;
                    }
                    try (Stream<Path> kind = Files.list(textures)) {
                        kind.forEach(that -> {
                            try (Stream<Path> stream = Files.walk(that)) {
                                stream
                                .filter(sub -> sub.toString().endsWith(".png"))
                                .forEach(sub -> {
                                    String category = that.relativize(sub).toString();
                                    String where = category.substring(0, category.lastIndexOf('.'));

                                    String type = that.getFileName().toString();
                                    LOGGER.debug("Searching for candidate generator for type '{}'.", type);
                                    ModelGenerator generator = GENERATORS.get(type);

                                    if (generator != null) {
                                        Identifier target = Identifier.of(mod, where);
                                        LOGGER.debug("Generating texture for {}", target);

                                        if (generator.valid(target)) {
                                            generator.generate(target).forEach(model -> {
                                                try {
                                                    Path output = Path.of("resourcepacks/solaris/assets/" + mod + model.getLeft());
                                                    LOGGER.debug("Writing model for {} to {}", target, output);
                                                    Files.createDirectories(output.getParent());
                                                    Files.writeString(output, model.getRight());
                                                } catch (IOException exception) {
                                                    SolarisTransformerLoader.oopsie(LOGGER, "FAILED GENERATING MODELS FOR " + target, exception);
                                                }
                                            });

                                            Path there = textures.relativize(sub);
                                            Path destination = Path.of("resourcepacks/solaris/assets/" + mod + "/textures/").resolve(there.toString());
                                            try {
                                                LOGGER.debug("Writing texture for {} to {}", target, destination);
                                                Files.createDirectories(destination.getParent());
                                                Files.copy(sub, destination, StandardCopyOption.REPLACE_EXISTING);
                                            } catch (IOException exception) {
                                                SolarisTransformerLoader.oopsie(LOGGER, "FAILED COPYING TEXTURE FOR " + target, exception);
                                            }
                                        }
                                    }
                                });
                            } catch (IOException exception) {
                                SolarisTransformerLoader.oopsie(LOGGER, "FAILED GENERATING RESOURCES", exception);
                            }
                        });
                    } catch (IOException exception) {
                        SolarisTransformerLoader.oopsie(LOGGER, "FAILED GENERATING RESOURCES", exception);
                    }
                } catch (IOException exception) {
                    SolarisTransformerLoader.oopsie(LOGGER, "FAILED GENERATING RESOURCES", exception);
                }
            });
        });
    }

    static {
        TYPES.add("items");
        TYPES.add("models/item");

        List<Class<ModelGenerator>> possible = ClasspathScanning.implementations(ModelGenerator.class, false);
        possible.forEach(clazz -> {
            try {
                Constructor<ModelGenerator> constructor = clazz.getConstructor();
                ModelGenerator instance = constructor.newInstance();
                String suffix = instance.suffix();
                LOGGER.debug("Found model generator {} for type '{}'.", clazz.getCanonicalName(), suffix);
                GENERATORS.putIfAbsent(suffix, instance);
            } catch (ReflectiveOperationException exception) {
                SolarisTransformerLoader.oopsie(LOGGER, "FAILED INITIALIZING MODEL GENERATOR: " + clazz.getCanonicalName(), exception);
            }
        });
    }
}
