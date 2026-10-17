package br.com.petrick.fornalhastech.blockentity;

import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import br.com.petrick.fornalhastech.registry.ModBlockEntities;
import br.com.petrick.fornalhastech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import javax.annotation.Nullable;

public class TechFurnaceEntity extends BlockEntity implements Container, MenuProvider {
    // Initial balance values: one vanilla fuel tick generates 10 FE; 10 FE are used per cooking-progress tick.
    public static final int ENERGY_CAPACITY = 32000;
    public static final int FE_PER_FUEL_TICK = 10;
    public static final int FE_PER_PROGRESS = 10;
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int burn=0, burnTotal=0, cook=0, cookTotal=200, color=0;
    private boolean colorCustomized=false;
    private boolean generating=false;
    private static final int[] SPEED={1,2,3,5,7,9,11};
    private static final class PersistentEnergyStorage extends EnergyStorage {
        PersistentEnergyStorage() { super(ENERGY_CAPACITY, 0, 0); }
        void loadEnergy(int amount) { this.energy = Math.max(0, Math.min(ENERGY_CAPACITY, amount)); }
        boolean consumeInternal(int amount) {
            if (amount <= 0 || energy < amount) return false;
            energy -= amount;
            return true;
        }
        boolean canGenerate(int amount) { return amount > 0 && energy <= capacity - amount; }
        void generateInternal(int amount) {
            if (amount > 0) energy = Math.min(capacity, energy + amount);
        }
    }
    private final PersistentEnergyStorage energy = new PersistentEnergyStorage();
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->energy);
    public TechFurnaceEntity(BlockPos p,BlockState s){super(ModBlockEntities.TECH_FURNACE.get(),p,s);}

    public static void tick(Level level,BlockPos pos,BlockState state,TechFurnaceEntity be){
        if(level.isClientSide)return;
        boolean oldLit=state.getValue(TechFurnaceBlock.LIT), oldCore=state.getValue(TechFurnaceBlock.CORE);
        be.generating=false;
        int mode=be.mode(); RecipeType<?> type=mode==1?RecipeType.BLASTING:mode==2?RecipeType.SMOKING:RecipeType.SMELTING;
        AbstractCookingRecipe recipe=be.findRecipe(type);
        boolean can=recipe!=null&&be.canOutput(recipe)&&!be.items.get(0).isEmpty();
        int speed=SPEED[be.coreTier()];
        boolean processing=can && be.energy.consumeInternal(FE_PER_PROGRESS*speed);
        if(processing){
            be.cook+=speed; be.cookTotal=Math.max(1,recipe.getCookingTime());
            if(be.cook>=be.cookTotal){be.cook-=be.cookTotal;be.smelt(recipe);}
        } else if(!can) be.cook=0;
        // Burn independently of a recipe. When storage is full, pause the active fuel timer without consuming energy.
        if(be.energy.canGenerate(FE_PER_FUEL_TICK)){
            if(be.burn<=0){int fuel=ForgeHooks.getBurnTime(be.items.get(1),RecipeType.SMELTING);if(fuel>0){be.burn=be.burnTotal=fuel;ItemStack stack=be.items.get(1);if(stack.hasCraftingRemainingItem())be.items.set(1,stack.getCraftingRemainingItem());else stack.shrink(1);}}
            if(be.burn>0){be.energy.generateInternal(FE_PER_FUEL_TICK);be.burn--;be.generating=true;}
        }
        boolean lit=processing||be.generating;
        boolean core=be.coreTier()>0;
        BlockState visual=state.setValue(TechFurnaceBlock.LIT,lit).setValue(TechFurnaceBlock.CORE,core).setValue(TechFurnaceBlock.COOKING,processing).setValue(TechFurnaceBlock.MODE,be.mode()).setValue(TechFurnaceBlock.VISUAL_COLOR,be.effectiveVisualColor());
        if(!visual.equals(state))level.setBlock(pos,visual,3);
        if(oldLit!=lit||oldCore!=core||level.getGameTime()%20==0)be.setChanged();
    }
    private AbstractCookingRecipe findRecipe(RecipeType<?> t){if(level==null||items.get(0).isEmpty())return null;return (AbstractCookingRecipe)level.getRecipeManager().getRecipeFor((RecipeType)t,this,level).orElse(null);}
    private boolean canOutput(AbstractCookingRecipe r){ItemStack result=r.getResultItem();ItemStack out=items.get(2);return out.isEmpty()||ItemHandlerHelper.canItemStacksStack(out,result)&&out.getCount()+result.getCount()<=out.getMaxStackSize();}
    private void smelt(AbstractCookingRecipe r){if(!canOutput(r))return;ItemStack result=r.assemble(this);if(items.get(2).isEmpty())items.set(2,result.copy());else items.get(2).grow(result.getCount());items.get(0).shrink(1);setChanged();}
    private int mode(){return items.get(4).is(ModItems.MODULE_BLAST_FURNACE.get())?1:items.get(4).is(ModItems.MODULE_SMOKER.get())?2:0;}
    public int coreTier(){ItemStack s=items.get(3);if(s.is(ModItems.CORE_BASE.get()))return 1;if(s.is(ModItems.CORE_IRON.get()))return 2;if(s.is(ModItems.CORE_GOLD.get()))return 3;if(s.is(ModItems.CORE_EMERALD.get()))return 4;if(s.is(ModItems.CORE_DIAMOND.get()))return 5;if(s.is(ModItems.CORE_NETHERITE.get()))return 6;return 0;}
    public int light(){return coreTier()==0?(getActive()?5:0):(getActive()?10:5);}
    private boolean getActive(){return generating || cook>0;}
    public int getVisualColorIndex(){return color;} public int getCoreVisualColorIndex(){return coreTier();}
    private int effectiveVisualColor(){return color;}
    private void refreshVisualState(){if(level==null||level.isClientSide)return;BlockState old=getBlockState();BlockState next=old.setValue(TechFurnaceBlock.MODE,mode()).setValue(TechFurnaceBlock.CORE,coreTier()>0).setValue(TechFurnaceBlock.VISUAL_COLOR,effectiveVisualColor());if(!next.equals(old))level.setBlock(worldPosition,next,3);}
    public void setVisualColorIndex(int value){int allowed=coreTier();if(value>=0&&value<=allowed){color=value;colorCustomized=allowed>0;}else{color=0;colorCustomized=false;}setChanged();refreshVisualState();}
    public int getCookTime(){return cook;} public int getCookTotal(){return cookTotal;} public int getBurnTime(){return burn;} public int getBurnTotal(){return burnTotal;}
    public int getEnergyStored(){return energy.getEnergyStored();} public int getMaxEnergyStored(){return ENERGY_CAPACITY;} public boolean isGenerating(){return generating;}
    public void dropContents(){if(level==null||level.isClientSide)return;for(ItemStack s:items)if(!s.isEmpty())net.minecraft.world.level.block.Block.popResource(level,worldPosition,s);}
    @Override public void load(CompoundTag t){super.load(t);net.minecraft.world.ContainerHelper.loadAllItems(t,items);burn=t.getInt("Burn");burnTotal=t.getInt("BurnTotal");cook=t.getInt("Cook");cookTotal=Math.max(1,t.getInt("CookTotal"));color=t.getInt("Color");colorCustomized=t.getBoolean("ColorCustomized");energy.loadEnergy(t.getInt("Energy"));if(color<0||color>coreTier()){color=0;colorCustomized=false;}else if(color==0&&coreTier()>0&&!colorCustomized){color=coreTier();}}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);net.minecraft.world.ContainerHelper.saveAllItems(t,items);t.putInt("Burn",burn);t.putInt("BurnTotal",burnTotal);t.putInt("Cook",cook);t.putInt("CookTotal",cookTotal);t.putInt("Color",color);t.putBoolean("ColorCustomized",colorCustomized);t.putInt("Energy",energy.getEnergyStored());}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public Component getDisplayName(){return Component.translatable("block.fornalhastech.tech_furnace");}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new TechFurnaceMenu(id,inv,this,worldPosition);}
    @Override public int getContainerSize(){return 6;} @Override public boolean isEmpty(){for(ItemStack s:items)if(!s.isEmpty())return false;return true;}
    @Override public ItemStack getItem(int i){return items.get(i);}
    private void normalizeColorAfterCoreChange(int previousTier){int newTier=coreTier();if(newTier==0){color=0;colorCustomized=false;}else if(previousTier==0&&!colorCustomized){color=newTier;}else if(color>newTier){color=0;colorCustomized=false;}}
    @Override public ItemStack removeItem(int i,int n){int oldTier=i==3?coreTier():-1;ItemStack s=net.minecraft.world.ContainerHelper.removeItem(items,i,n);if(!s.isEmpty()){setChanged();if(i==3||i==4){if(i==3)normalizeColorAfterCoreChange(oldTier);refreshVisualState();}}return s;}
    @Override public ItemStack removeItemNoUpdate(int i){int oldTier=i==3?coreTier():-1;ItemStack s=net.minecraft.world.ContainerHelper.takeItem(items,i);if(i==3||i==4){if(i==3)normalizeColorAfterCoreChange(oldTier);refreshVisualState();}return s;}
    @Override public void setItem(int i,ItemStack s){int oldTier=i==3?coreTier():-1;items.set(i,s);s.setCount(Math.min(s.getCount(),getMaxStackSize()));setChanged();if(i==3||i==4){if(i==3)normalizeColorAfterCoreChange(oldTier);refreshVisualState();}}
    @Override public boolean canPlaceItem(int i,ItemStack s){return switch(i){case 0->!isComponent(s);case 1->ForgeHooks.getBurnTime(s,RecipeType.SMELTING)>0;case 3->isCore(s);case 4->isModule(s);default->false;};}
    private boolean isComponent(ItemStack s){return isCore(s)||isModule(s);}
    private boolean isCore(ItemStack s){return s.is(ModItems.CORE_BASE.get())||s.is(ModItems.CORE_IRON.get())||s.is(ModItems.CORE_GOLD.get())||s.is(ModItems.CORE_EMERALD.get())||s.is(ModItems.CORE_DIAMOND.get())||s.is(ModItems.CORE_NETHERITE.get());}
    private boolean isModule(ItemStack s){return s.is(ModItems.MODULE_BLAST_FURNACE.get())||s.is(ModItems.MODULE_SMOKER.get());}
    public ItemStack removeModule(){ItemStack module=items.get(4);if(module.isEmpty())return ItemStack.EMPTY;items.set(4,ItemStack.EMPTY);setChanged();refreshVisualState();return module;}
    public boolean installModule(ItemStack held){if(!items.get(4).isEmpty()||!isModule(held))return false;ItemStack installed=held.copy();installed.setCount(1);items.set(4,installed);held.shrink(1);setChanged();refreshVisualState();return true;}
    @Override public void clearContent(){items.clear();color=0;colorCustomized=false;setChanged();refreshVisualState();} @Override public boolean stillValid(Player p){return worldPosition.distToCenterSqr(p.position())<=64.0D;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){if(cap==ForgeCapabilities.ENERGY)return energyCap.cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();}
}
