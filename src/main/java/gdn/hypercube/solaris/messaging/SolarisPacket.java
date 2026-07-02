package gdn.hypercube.solaris.messaging;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public interface SolarisPacket extends CustomPayload {
    Direction direction();
    Handler<? extends SolarisPacket> handler();
    PacketCodec<RegistryByteBuf, ? extends SolarisPacket> codec();

    enum Direction {
        server,
        client
    }

    @FunctionalInterface
    interface Handler<T extends SolarisPacket> {
        void receive(T payload, Context context);
    }

    record Context(Object target, PlayerEntity player, PacketSender sender) {}
}