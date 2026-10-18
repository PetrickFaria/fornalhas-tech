package br.com.petrick.fornalhastech;

import br.com.petrick.fornalhastech.registry.ModBlockEntities;
import br.com.petrick.fornalhastech.registry.ModBlocks;
import br.com.petrick.fornalhastech.registry.ModItems;
import br.com.petrick.fornalhastech.registry.ModMenus;
import br.com.petrick.fornalhastech.registry.ModParticles;
import br.com.petrick.fornalhastech.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FornalhasTech.MODID)
public class FornalhasTech {
    public static final String MODID = "fornalhastech";
    public FornalhasTech() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus); ModItems.ITEMS.register(bus);
        ModBlockEntities.TYPES.register(bus); ModMenus.TYPES.register(bus);
        ModParticles.TYPES.register(bus);
        ModNetwork.register();
    }
}

