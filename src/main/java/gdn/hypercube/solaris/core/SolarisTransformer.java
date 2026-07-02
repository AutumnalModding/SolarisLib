package gdn.hypercube.solaris.core;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface SolarisTransformer {
    String internal$transformerTarget();
    record TargetData(ClassNode node, MethodNode method) {}

    interface Class extends SolarisTransformer {}
    interface Global extends SolarisTransformer {}
}
