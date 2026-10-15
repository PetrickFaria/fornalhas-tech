package br.com.petrick.fornalhastech.menu;
import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import br.com.petrick.fornalhastech.registry.ModMenus;
import br.com.petrick.fornalhastech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.ItemHandlerHelper;

public class TechFurnaceMenu extends AbstractContainerMenu {
 private final Container furnace; private final Level level; private final BlockPos pos;
 private int syncedCook, syncedCookTotal=200, syncedBurn, syncedBurnTotal;
 public TechFurnaceMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,data.readBlockPos());}
 private TechFurnaceMenu(int id,Inventory inv,BlockPos p){this(id,inv,find(inv,p),p);}
 private static Container find(Inventory inv,BlockPos p){BlockEntity be=inv.player.level.getBlockEntity(p);return be instanceof TechFurnaceEntity? (TechFurnaceEntity)be:new SimpleContainer(6);}
 public TechFurnaceMenu(int id,Inventory inv,Container container,BlockPos pos){super(ModMenus.TECH_FURNACE.get(),id);this.furnace=container;this.level=inv.player.level;this.pos=pos;
  addSlot(new Slot(container,0,48,58){public boolean mayPlace(ItemStack s){return !isComponent(s);}});
  addSlot(new Slot(container,1,48,94){public boolean mayPlace(ItemStack s){return net.minecraftforge.common.ForgeHooks.getBurnTime(s,net.minecraft.world.item.crafting.RecipeType.SMELTING)>0;}});
  addSlot(new Slot(container,2,112,76){public boolean mayPlace(ItemStack s){return false;}});
  addSlot(new Slot(container,3,39,20){public boolean mayPlace(ItemStack s){return isCore(s);}});
  addSlot(new Slot(container,4,77,20){public boolean mayPlace(ItemStack s){return isModule(s);}});
  addSlot(new Slot(container,5,115,20){public boolean mayPlace(ItemStack s){return false;}});
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,130+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,184));
  addDataSlot(dataSlot(0)); addDataSlot(dataSlot(1)); addDataSlot(dataSlot(2)); addDataSlot(dataSlot(3));
 }
 private DataSlot dataSlot(int index){
  return new DataSlot(){
   @Override public int get(){
    if(level.isClientSide)return switch(index){case 0->syncedCook;case 1->syncedCookTotal;case 2->syncedBurn;default->syncedBurnTotal;};
    if(furnace instanceof TechFurnaceEntity be)return switch(index){case 0->be.getCookTime();case 1->be.getCookTotal();case 2->be.getBurnTime();default->be.getBurnTotal();};
    return 0;
   }
   @Override public void set(int value){switch(index){case 0->syncedCook=value;case 1->syncedCookTotal=value;case 2->syncedBurn=value;default->syncedBurnTotal=value;}}
  };
 }
 public int getCookProgress(){return syncedCookTotal<=0?0:Math.min(18,syncedCook*18/syncedCookTotal);}
 public int getBurnProgress(){return syncedBurnTotal<=0?0:Math.min(14,syncedBurn*14/syncedBurnTotal);}
 private static boolean isCore(ItemStack s){return s.is(ModItems.CORE_BASE.get())||s.is(ModItems.CORE_IRON.get())||s.is(ModItems.CORE_GOLD.get())||s.is(ModItems.CORE_EMERALD.get())||s.is(ModItems.CORE_DIAMOND.get())||s.is(ModItems.CORE_NETHERITE.get());}
 private static boolean isModule(ItemStack s){return s.is(ModItems.MODULE_BLAST_FURNACE.get())||s.is(ModItems.MODULE_SMOKER.get());}
 private static boolean isComponent(ItemStack s){return isCore(s)||isModule(s);}
 @Override public boolean stillValid(Player p){return furnace.stillValid(p);}
 @Override public ItemStack quickMoveStack(Player p,int idx){ItemStack original=ItemStack.EMPTY;Slot slot=slots.get(idx);if(slot!=null&&slot.hasItem()){ItemStack stack=slot.getItem();original=stack.copy();if(idx<6){if(!moveItemStackTo(stack,6,slots.size(),true))return ItemStack.EMPTY;}else{int dest=isCore(stack)?3:isModule(stack)?4:1;if(!moveItemStackTo(stack,dest,dest+1,false)&&!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();}return original;}
}
