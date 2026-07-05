package gdn.hypercube.solaris.generator.resource.type;

import gdn.hypercube.solaris.api.ModelGenerator;
import gdn.hypercube.solaris.util.ChainedList;
import gdn.hypercube.solaris.util.UsedImplicitly;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import static gdn.hypercube.solaris.api.ModelGenerator.path;
import static gdn.hypercube.solaris.api.ModelGenerator.model;

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
        ChainedList<Pair<String, String>> models = new ChainedList<>();
        return models.add(new Pair<>(path("/models/item/", texture), """
            {
              "parent": "minecraft:item/generated",
              "textures": {
                "layer0":\s""" + model("item", texture) + """
              \n\s\s}
            }
            """
        )).add(new Pair<>(path("/items/", texture), """
            {
              "model": {
                "type": "minecraft:model",
                "model":\s""" + model("item", texture) + """
              \n\s\s}
            }
            """
        )).arrayify();
    }
}
