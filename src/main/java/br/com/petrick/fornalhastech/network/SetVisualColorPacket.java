package br.com.petrick.fornalhastech.network;

import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SetVisualColorPacket {
    private final BlockPos pos;
    private final int color;

    public SetVisualColorPacket(BlockPos pos, int color) {
        this.pos = pos;
        this.color = color;
    }

    public static void encode(SetVisualColorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.color);
    }

    public static SetVisualColorPacket decode(FriendlyByteBuf buffer) {
        return new SetVisualColorPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(SetVisualColorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.color < 0 || packet.color > 6) return;
            if (player.containerMenu instanceof TechFurnaceMenu menu
                    && menu.getBlockPos().equals(packet.pos)
                    && player.level.getBlockEntity(packet.pos) instanceof TechFurnaceEntity furnace
                    && packet.color <= furnace.coreTier()
                    && player.distanceToSqr(packet.pos.getX() + .5D, packet.pos.getY() + .5D, packet.pos.getZ() + .5D) <= 64.0D) {
                menu.setVisualColorIndex(packet.color);
            }
        });
        context.setPacketHandled(true);
    }
}

