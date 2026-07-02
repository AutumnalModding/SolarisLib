package gdn.hypercube.solaris.generator.resource;

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
}
