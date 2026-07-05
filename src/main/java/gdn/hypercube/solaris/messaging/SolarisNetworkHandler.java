package gdn.hypercube.solaris.messaging;

import gdn.hypercube.solaris.core.ClasspathScanning;
import gdn.hypercube.solaris.core.SolarisTransformerLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SolarisNetworkHandler implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Solaris Network Handler");

    @SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
    public void onInitialize() {
        LOGGER.info("Scanning for packets...");
        List<Class<SolarisPacket>> packets = ClasspathScanning.implementations(SolarisPacket.class, false);
        packets.forEach(clazz -> {
            try {
                LOGGER.debug("Registering packet {}", clazz.getSimpleName());
                Constructor<? extends SolarisPacket> constructor = clazz.getConstructor();
                SolarisPacket packet = constructor.newInstance();
                CustomPayload.Id<? extends SolarisPacket> id = (CustomPayload.Id<? extends SolarisPacket>) clazz.getDeclaredMethod("ident").invoke(packet);
                PacketCodec<RegistryByteBuf, ? extends SolarisPacket> codec = (PacketCodec<RegistryByteBuf, ? extends SolarisPacket>) clazz.getDeclaredMethod("codec").invoke(packet);
                SolarisPacket.Direction direction = (SolarisPacket.Direction) clazz.getDeclaredMethod("direction").invoke(packet);
                SolarisPacket.Handler<? extends SolarisPacket> handler = (SolarisPacket.Handler<? extends SolarisPacket>) clazz.getDeclaredMethod("handler").invoke(packet);

                String side = "net.fabricmc.fabric.api."
                + (direction.name().equals("client") ? direction.name() + "." : "") + "networking.v1."
                + WordUtils.capitalizeFully(direction.name()) + "PlayNetworking";

                PayloadTypeRegistry<RegistryByteBuf> registry = (PayloadTypeRegistry<RegistryByteBuf>)
                PayloadTypeRegistry.class.getMethod(direction.name() + "boundPlay").invoke(null);
                registry.register(id, (PacketCodec) codec);

                Class<?> networker = Class.forName(side);

                Method registrar = Arrays.stream(networker.getMethods())
                        .filter(m -> m.getName().equals("registerGlobalReceiver") && m.getParameterCount() == 2)
                        .findFirst()
                        .orElseThrow(() -> new NoSuchMethodException("registerGlobalReceiver"));

                Class<?> iface = registrar.getParameterTypes()[1];
                Method receiver = Arrays.stream(iface.getMethods())
                        .filter(m -> !m.isDefault())
                        .findFirst()
                        .orElseThrow(() -> new NoSuchMethodException("SAM on " + iface));

                Class<?> context = receiver.getParameterTypes()[1];
                Method player = context.getMethod("player");
                Method sender = context.getMethod("responseSender");
                Method resolved;
                try {
                    resolved = context.getMethod("client");
                } catch (NoSuchMethodException exception) {
                    resolved = context.getMethod("server");
                }

                Method target = resolved;
                Object instance = Proxy.newProxyInstance(
                    iface.getClassLoader(),
                    new Class<?>[]{iface},
                    (proxy, method, args) -> {
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, args);
                        }

                        SolarisPacket payload = (SolarisPacket) args[0];
                        Object raw = args[1];

                        SolarisPacket.Context converted = new SolarisPacket.Context(target.invoke(raw), (PlayerEntity) player.invoke(raw), (PacketSender) sender.invoke(raw));
                        ((SolarisPacket.Handler) handler).receive(payload, converted);
                        return null;
                    }
                );

                registrar.invoke(null, id, instance);
            } catch (ReflectiveOperationException exception) {
                SolarisTransformerLoader.oopsie(LOGGER, "FAILED REGISTERING PACKET: " + clazz.getSimpleName(), exception);
            }
        });
    }
}
