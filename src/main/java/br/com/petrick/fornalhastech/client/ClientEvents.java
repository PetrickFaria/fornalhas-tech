package br.com.petrick.fornalhastech.client;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.registry.ModMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=FornalhasTech.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientEvents { @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(ModMenus.TECH_FURNACE.get(),TechFurnaceScreen::new);} }
