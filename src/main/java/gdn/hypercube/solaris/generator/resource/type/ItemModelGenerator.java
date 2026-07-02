package gdn.hypercube.solaris.generator.resource.type;

import gdn.hypercube.solaris.generator.resource.ModelGenerator;
import gdn.hypercube.solaris.util.UsedImplicitly;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

@UsedImplicitly
public class ItemModelGenerator implements ModelGenerator {
    @Override
    public String suffix() {
        return "item";
    }

    @Override
    public boolean valid(Identifier texture) {
        return true;
    }

    @Override
    public List<Pair<String, String>> generate(Identifier texture) {
        List<Pair<String, String>> models = new ArrayList<>();
        models.add(new Pair<>("/models/item/" + texture.getPath() + ".json", """
            {
              "parent": "minecraft:item/generated",
              "textures": {
                "layer0":\s""" + "\"" + texture.getNamespace() + ":item/" + texture.getPath() + "\"" + """
              \n\s\s}
            }
            """
        ));
        models.add(new Pair<>("/items/" + texture.getPath() + ".json", """
            {
              "model": {
                "type": "minecraft:model",
                "model":\s""" + "\"" + texture.getNamespace() + ":item/" + texture.getPath() + "\"" + """
              \n\s\s}
            }
            """
        ));
        return models;
    }
}
