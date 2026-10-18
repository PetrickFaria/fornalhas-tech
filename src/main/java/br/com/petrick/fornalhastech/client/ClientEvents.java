package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.registry.ModMenus;
import br.com.petrick.fornalhastech.registry.ModBlocks;
import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

@Mod.EventBusSubscriber(modid = FornalhasTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(br.com.petrick.fornalhastech.registry.ModBlockEntities.TECH_FURNACE.get(),
                TechFurnaceEnergyRenderer::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            return switch (state.getValue(TechFurnaceBlock.VISUAL_COLOR)) {
                case 1 -> 0xFFE83E3E;
                case 2 -> 0xFFE8EFF1;
                case 3 -> 0xFFFFC844;
                case 4 -> 0xFF43D878;
                case 5 -> 0xFF42D8ED;
                case 6 -> 0xFFAD704A;
                default -> 0xFF9AA6AA;
            };
        }, ModBlocks.TECH_FURNACE.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.TECH_FURNACE.get(), TechFurnaceScreen::new));
    }
}
