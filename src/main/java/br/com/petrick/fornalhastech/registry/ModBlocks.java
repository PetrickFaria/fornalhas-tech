package br.com.petrick.fornalhastech.registry;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public class ModBlocks {
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FornalhasTech.MODID);
 public static final RegistryObject<Block> TECH_FURNACE=BLOCKS.register("tech_furnace",()->new TechFurnaceBlock(BlockBehaviour.Properties.of(Material.METAL).strength(3.5F).sound(SoundType.METAL).lightLevel(s->s.getValue(TechFurnaceBlock.CORE)?(s.getValue(TechFurnaceBlock.COOKING)?15:10):(s.getValue(TechFurnaceBlock.COOKING)?5:0))));
}

