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
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import br.com.petrick.fornalhastech.registry.ModParticles;

@Mod.EventBusSubscriber(modid = FornalhasTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(br.com.petrick.fornalhastech.registry.ModBlockEntities.TECH_FURNACE.get(),
                TechFurnaceEnergyRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.register(ModParticles.SMALL_FIRE.get(), SmallFlameParticleProvider::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return 0xFFFFFFFF;
            return TechFurnaceBlock.getTechnologyColor(state.getValue(TechFurnaceBlock.VISUAL_COLOR));
        }, ModBlocks.TECH_FURNACE.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.TECH_FURNACE.get(), TechFurnaceScreen::new));
    }
}


