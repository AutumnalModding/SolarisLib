package gdn.hypercube.solaris.api;

import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

/**
 * Mom, can we have datagen?<br/>
 * No, we have datagen at home.<br/>
 * Datagen at home:
 */
public interface ModelGenerator {
    String suffix();
    boolean valid(Identifier texture);
    List<Pair<String, String>> generate(Identifier texture);

    static String path(String prefix, Identifier texture) {
        return prefix + texture.getPath() + ".json";
    }

    static String model(String type, Identifier texture) {
        return "\"" + texture.getNamespace() + ":" + type + "/" + texture.getPath() + "\"";
    }
}
