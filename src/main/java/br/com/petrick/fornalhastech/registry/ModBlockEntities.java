package br.com.petrick.fornalhastech.registry;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class ModBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,FornalhasTech.MODID);
 public static final RegistryObject<BlockEntityType<TechFurnaceEntity>> TECH_FURNACE=TYPES.register("tech_furnace",()->BlockEntityType.Builder.of(TechFurnaceEntity::new,ModBlocks.TECH_FURNACE.get()).build(null));
}
