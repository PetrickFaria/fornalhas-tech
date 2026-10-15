package br.com.petrick.fornalhastech.registry;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class ModMenus {
 public static final DeferredRegister<MenuType<?>> TYPES=DeferredRegister.create(ForgeRegistries.MENU_TYPES,FornalhasTech.MODID);
 public static final RegistryObject<MenuType<TechFurnaceMenu>> TECH_FURNACE=TYPES.register("tech_furnace",()->IForgeMenuType.create(TechFurnaceMenu::new));
}
