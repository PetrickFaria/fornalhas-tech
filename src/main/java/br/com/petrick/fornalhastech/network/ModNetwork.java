package br.com.petrick.fornalhastech.network;

import br.com.petrick.fornalhastech.FornalhasTech;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FornalhasTech.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, SetVisualColorPacket.class,
                SetVisualColorPacket::encode, SetVisualColorPacket::decode, SetVisualColorPacket::handle);
    }
}
