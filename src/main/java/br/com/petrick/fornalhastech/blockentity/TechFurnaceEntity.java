package br.com.petrick.fornalhastech.blockentity;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import br.com.petrick.fornalhastech.registry.ModBlockEntities;
import br.com.petrick.fornalhastech.registry.ModItems;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.Objects;

public class TechFurnaceEntity extends BlockEntity implements Container, MenuProvider {
 private final NonNullList<ItemStack> items=NonNullList.withSize(6,ItemStack.EMPTY);
 private int burn=0, burnTotal=0, cook=0, cookTotal=200, color=0;
 private static final int[] SPEED={1,2,3,5,7,9,11};
 public TechFurnaceEntity(BlockPos p,BlockState s){super(ModBlockEntities.TECH_FURNACE.get(),p,s);}
 public static void tick(Level level,BlockPos pos,BlockState state,TechFurnaceEntity be){if(level.isClientSide)return;boolean wasLit=be.burn>0;if(be.burn>0)be.burn--;int mode=be.mode();RecipeType<?> type=mode==1?RecipeType.BLASTING:mode==2?RecipeType.SMOKING:RecipeType.SMELTING;
  AbstractCookingRecipe recipe=be.findRecipe(type);
  boolean can=recipe!=null&&be.canOutput(recipe)&&!be.items.get(0).isEmpty();
  if(can&&be.burn<=0){int fuel=ForgeHooks.getBurnTime(be.items.get(1),type);if(fuel>0){be.burn=be.burnTotal=fuel;if(be.items.get(1).hasCraftingRemainingItem())be.items.set(1,be.items.get(1).getCraftingRemainingItem());else be.items.get(1).shrink(1);}}
  if(can&&be.burn>0){int core=be.coreTier();int multiplier=SPEED[core];int speed=multiplier;be.cook+=speed;be.cookTotal=recipe.getCookingTime();if(be.cook>=be.cookTotal){be.cook-=be.cookTotal;be.smelt(recipe);}}
  else if(be.cook>0)be.cook=Math.max(0,be.cook-2);
  boolean lit=be.burn>0;boolean core=be.coreTier()>0;boolean cooking=can&&be.burn>0;if(state.getValue(TechFurnaceBlock.LIT)!=lit||state.getValue(TechFurnaceBlock.CORE)!=core||state.getValue(TechFurnaceBlock.COOKING)!=cooking){level.setBlock(pos,state.setValue(TechFurnaceBlock.LIT,lit).setValue(TechFurnaceBlock.CORE,core).setValue(TechFurnaceBlock.COOKING,cooking),3);be.setChanged();}if(level.getGameTime()%20==0)be.setChanged();
 }
 private AbstractCookingRecipe findRecipe(RecipeType<?> t){if(level==null||items.get(0).isEmpty())return null;return (AbstractCookingRecipe)level.getRecipeManager().getRecipeFor((RecipeType)t,this,level).orElse(null);}
 private boolean canOutput(AbstractCookingRecipe r){ItemStack result=r.getResultItem();ItemStack out=items.get(2);return out.isEmpty()||ItemHandlerHelper.canItemStacksStack(out,result)&&out.getCount()+result.getCount()<=out.getMaxStackSize();}
 private void smelt(AbstractCookingRecipe r){if(!canOutput(r))return;ItemStack result=r.assemble(this);if(items.get(2).isEmpty())items.set(2,result.copy());else items.get(2).grow(result.getCount());items.get(0).shrink(1);setChanged();}
 private int mode(){return items.get(4).is(ModItems.MODULE_BLAST_FURNACE.get())?1:items.get(4).is(ModItems.MODULE_SMOKER.get())?2:0;}
 private int coreTier(){ItemStack s=items.get(3);if(s.is(ModItems.CORE_BASE.get()))return 1;if(s.is(ModItems.CORE_IRON.get()))return 2;if(s.is(ModItems.CORE_GOLD.get()))return 3;if(s.is(ModItems.CORE_EMERALD.get()))return 4;if(s.is(ModItems.CORE_DIAMOND.get()))return 5;if(s.is(ModItems.CORE_NETHERITE.get()))return 6;return 0;}
 public int light(){return coreTier()==0?(burn>0?5:0):(cook>0?15:10);}
 public int getCookTime(){return cook;} public int getCookTotal(){return cookTotal;} public int getBurnTime(){return burn;} public int getBurnTotal(){return burnTotal;}
 public void dropContents(){if(level==null||level.isClientSide)return;for(ItemStack s:items)if(!s.isEmpty()){net.minecraft.world.level.block.Block.popResource(level,worldPosition,s);}}
 @Override public void load(CompoundTag t){super.load(t);net.minecraft.world.ContainerHelper.loadAllItems(t,items);burn=t.getInt("Burn");burnTotal=t.getInt("BurnTotal");cook=t.getInt("Cook");cookTotal=t.getInt("CookTotal");color=t.getInt("Color");}
 @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);net.minecraft.world.ContainerHelper.saveAllItems(t,items);t.putInt("Burn",burn);t.putInt("BurnTotal",burnTotal);t.putInt("Cook",cook);t.putInt("CookTotal",cookTotal);t.putInt("Color",color);}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
 @Override public Component getDisplayName(){return Component.translatable("block.fornalhastech.tech_furnace");}
 @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new TechFurnaceMenu(id,inv,this,worldPosition);}
 @Override public int getContainerSize(){return 6;} @Override public boolean isEmpty(){for(ItemStack s:items)if(!s.isEmpty())return false;return true;}
 @Override public ItemStack getItem(int i){return items.get(i);}
 @Override public ItemStack removeItem(int i,int n){ItemStack s=net.minecraft.world.ContainerHelper.removeItem(items,i,n);if(!s.isEmpty())setChanged();return s;}
 @Override public ItemStack removeItemNoUpdate(int i){return net.minecraft.world.ContainerHelper.takeItem(items,i);}
 @Override public void setItem(int i,ItemStack s){items.set(i,s);s.setCount(Math.min(s.getCount(),getMaxStackSize()));setChanged();}
 @Override public boolean canPlaceItem(int i,ItemStack s){return switch(i){case 0->!s.is(ModItems.CORE_BASE.get())&&!s.is(ModItems.CORE_IRON.get())&&!s.is(ModItems.CORE_GOLD.get())&&!s.is(ModItems.CORE_EMERALD.get())&&!s.is(ModItems.CORE_DIAMOND.get())&&!s.is(ModItems.CORE_NETHERITE.get())&&!s.is(ModItems.MODULE_BLAST_FURNACE.get())&&!s.is(ModItems.MODULE_SMOKER.get());case 1->ForgeHooks.getBurnTime(s,RecipeType.SMELTING)>0;case 3->s.is(ModItems.CORE_BASE.get())||s.is(ModItems.CORE_IRON.get())||s.is(ModItems.CORE_GOLD.get())||s.is(ModItems.CORE_EMERALD.get())||s.is(ModItems.CORE_DIAMOND.get())||s.is(ModItems.CORE_NETHERITE.get());case 4->s.is(ModItems.MODULE_BLAST_FURNACE.get())||s.is(ModItems.MODULE_SMOKER.get());default->false;};}
 @Override public void clearContent(){items.clear();setChanged();} @Override public boolean stillValid(Player p){return worldPosition.distToCenterSqr(p.position()) <= 64.0D;}
}
