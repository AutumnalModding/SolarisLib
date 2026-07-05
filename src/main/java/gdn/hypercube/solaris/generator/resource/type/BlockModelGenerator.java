package gdn.hypercube.solaris.generator.resource.type;

import gdn.hypercube.solaris.api.ModelGenerator;
import gdn.hypercube.solaris.util.ChainedList;
import java.util.List;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import static gdn.hypercube.solaris.api.ModelGenerator.path;
import static gdn.hypercube.solaris.api.ModelGenerator.model;

public class BlockModelGenerator implements ModelGenerator {
    @Override
    public String suffix() {
        return "block";
    }

    @Override
    public boolean valid(Identifier texture) {
        return true;
    }

    /*
    TODO: Encode parent instead of hardcoding
    TODO: Mark for registration after parsing
     */
    @Override
    public List<Pair<String, String>> generate(Identifier texture) {
        ChainedList<Pair<String, String>> models = new ChainedList<>();
        return models.add(new Pair<>(path("/blockstates/", texture), """
            {
              "variants": {
                "": {
                  "model":\s""" + model("block", texture) + """
                \n\s\s}
              \n\s\s}
            }
            """
        )).add(new Pair<>(path("/items/", texture), """
            {
              "model": {
                "type": "minecraft:model",
                "model":\s""" + model("block", texture) + """
              \n\s\s}
            }
            """
        )).add(new Pair<>(path("/models/block/", texture), """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all":\s""" + model("block", texture) + """
              \n\s\s}
            }
        """)).arrayify();
    }
}
