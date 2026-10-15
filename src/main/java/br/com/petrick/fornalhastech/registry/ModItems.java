package br.com.petrick.fornalhastech.registry;
import br.com.petrick.fornalhastech.FornalhasTech;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class ModItems {
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FornalhasTech.MODID);
 public static final RegistryObject<Item> TECH_FURNACE_ITEM=ITEMS.register("tech_furnace",()->new BlockItem(ModBlocks.TECH_FURNACE.get(),new Item.Properties().tab(net.minecraft.world.item.CreativeModeTab.TAB_DECORATIONS)));
 public static final RegistryObject<Item> CORE_BASE=item("core_base"); public static final RegistryObject<Item> CORE_IRON=item("core_iron");
 public static final RegistryObject<Item> CORE_GOLD=item("core_gold"); public static final RegistryObject<Item> CORE_EMERALD=item("core_emerald");
 public static final RegistryObject<Item> CORE_DIAMOND=item("core_diamond"); public static final RegistryObject<Item> CORE_NETHERITE=item("core_netherite");
 public static final RegistryObject<Item> MODULE_BLAST_FURNACE=item("module_blast_furnace"); public static final RegistryObject<Item> MODULE_SMOKER=item("module_smoker");
 private static RegistryObject<Item> item(String name){return ITEMS.register(name,()->new Item(new Item.Properties().tab(net.minecraft.world.item.CreativeModeTab.TAB_MISC)));}
}
