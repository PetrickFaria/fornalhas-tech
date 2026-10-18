package br.com.petrick.fornalhastech.blockentity;

import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import br.com.petrick.fornalhastech.registry.ModBlockEntities;
import br.com.petrick.fornalhastech.registry.ModItems;
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
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;

public class TechFurnaceEntity extends BlockEntity implements Container, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int burn;
    private int burnTotal;
    private int cook;
    private int cookTotal = 200;
    private int color;
    private boolean colorCustomized;
    private float pendingExperience;
    // Legacy stacks are retained in NBT until safely returned to the world on a server tick.
    private final java.util.List<ItemStack> overflow = new java.util.ArrayList<>();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemCapability =
            net.minecraftforge.common.util.LazyOptional.of(this::createItemHandler);

    // Existing core progression values are retained as cooking-speed multipliers.
    private static final int[] SPEED = {1, 2, 3, 5, 7, 9, 11};

    public TechFurnaceEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TECH_FURNACE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TechFurnaceEntity furnace) {
        if (level.isClientSide) return;

        furnace.normalizeSpecialStacks();
        furnace.returnOverflow();
        int oldBurn = furnace.burn;
        int oldCook = furnace.cook;
        boolean oldLit = state.getValue(TechFurnaceBlock.LIT);
        boolean oldCore = state.getValue(TechFurnaceBlock.CORE);
        int mode = furnace.mode();
        RecipeType<?> recipeType = mode == 1 ? RecipeType.BLASTING
                : mode == 2 ? RecipeType.SMOKING : RecipeType.SMELTING;
        AbstractCookingRecipe recipe = furnace.findRecipe(recipeType);
        boolean canCook = recipe != null && !furnace.items.get(0).isEmpty() && furnace.canOutput(recipe);

        int speed = SPEED[furnace.coreTier()];
        boolean cooking = false;
        if (canCook) {
            furnace.cookTotal = Math.max(1, recipe.getCookingTime());
            // Use the same native recipe-work units for fuel and cooking. The last
            // step is capped to remaining recipe work, avoiding integer-rounding loss.
            int budget = FuelWork.step(speed, furnace.cook, furnace.cookTotal);
            int progressed = 0;
            while (budget > 0) {
                if (furnace.burn <= 0) furnace.startBurning();
                if (furnace.burn <= 0) break;
                int used = Math.min(budget, furnace.burn);
                furnace.burn -= used;
                furnace.cook += used;
                progressed += used;
                budget -= used;
            }
            cooking = progressed > 0;
            if (furnace.cook >= furnace.cookTotal) {
                furnace.smelt(recipe);
                furnace.cook = 0;
                furnace.pendingExperience += recipe.getExperience();
            } else if (!cooking && furnace.cook > 0) {
                furnace.cook = Math.max(0, furnace.cook - 2);
            }
        } else {
            // Like vanilla, already burning fuel also expires while output/input is blocked.
            furnace.burn = Math.max(0, furnace.burn - speed);
            furnace.cook = 0;
        }
        // Decide LIT only after recipe completion and fuel transitions. If work can
        // continue and the prior unit ended exactly on this tick boundary, begin the
        // next unit now; its burn clock is first charged on the next server tick.
        if (furnace.burn <= 0
                && FuelWork.shouldPrimeNextTick(furnace.burn, furnace.hasRecipeWork(recipeType))) {
            furnace.startBurning();
        }
        boolean lit = furnace.burn > 0;

        boolean core = furnace.coreTier() > 0;
        BlockState visual = state.setValue(TechFurnaceBlock.LIT, lit)
                .setValue(TechFurnaceBlock.CORE, core)
                .setValue(TechFurnaceBlock.COOKING, cooking)
                .setValue(TechFurnaceBlock.MODE, mode)
                .setValue(TechFurnaceBlock.VISUAL_COLOR, furnace.effectiveVisualColor());
        if (!visual.equals(state)) level.setBlock(pos, visual, 3);
        if (oldLit != lit || oldCore != core || oldBurn != furnace.burn || oldCook != furnace.cook || level.getGameTime() % 20 == 0) furnace.setChanged();
    }

    private boolean hasRecipeWork(RecipeType<?> type) {
        if (items.get(0).isEmpty()) return false;
        AbstractCookingRecipe nextRecipe = findRecipe(type);
        return nextRecipe != null && canOutput(nextRecipe);
    }

    private void startBurning() {
        ItemStack fuel = items.get(1);
        int duration = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
        if (duration <= 0) return;

        burn = duration;
        burnTotal = duration;
        if (fuel.hasCraftingRemainingItem()) {
            ItemStack remainder = fuel.getCraftingRemainingItem();
            items.set(1, remainder);
        } else {
            fuel.shrink(1);
        }
        setChanged();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AbstractCookingRecipe findRecipe(RecipeType<?> type) {
        if (level == null || items.get(0).isEmpty()) return null;
        return (AbstractCookingRecipe) level.getRecipeManager().getRecipeFor((RecipeType) type, this, level).orElse(null);
    }

    private boolean canOutput(AbstractCookingRecipe recipe) {
        ItemStack result = recipe.getResultItem();
        ItemStack output = items.get(2);
        return output.isEmpty() || ItemHandlerHelper.canItemStacksStack(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void smelt(AbstractCookingRecipe recipe) {
        if (!canOutput(recipe)) return;
        ItemStack result = recipe.assemble(this);
        if (items.get(2).isEmpty()) items.set(2, result.copy());
        else items.get(2).grow(result.getCount());
        items.get(0).shrink(1);
        setChanged();
    }

    /** Awards the recipe experience accumulated since the previous output collection. */
    public void awardPendingExperience(Player player) {
        if (pendingExperience <= 0) return;
        int points = (int) pendingExperience;
        float fraction = pendingExperience - points;
        if (fraction > 0 && player.getRandom().nextFloat() < fraction) points++;
        if (points > 0) player.giveExperiencePoints(points);
        pendingExperience = 0;
        setChanged();
    }

    private int mode() {
        return items.get(4).is(ModItems.MODULE_BLAST_FURNACE.get()) ? 1
                : items.get(4).is(ModItems.MODULE_SMOKER.get()) ? 2 : 0;
    }

    public int coreTier() {
        ItemStack stack = items.get(3);
        if (stack.is(ModItems.CORE_BASE.get())) return 1;
        if (stack.is(ModItems.CORE_IRON.get())) return 2;
        if (stack.is(ModItems.CORE_GOLD.get())) return 3;
        if (stack.is(ModItems.CORE_EMERALD.get())) return 4;
        if (stack.is(ModItems.CORE_DIAMOND.get())) return 5;
        if (stack.is(ModItems.CORE_NETHERITE.get())) return 6;
        return 0;
    }

    public int getVisualColorIndex() { return color; }
    private int effectiveVisualColor() { return color; }

    private void refreshVisualState() {
        if (level == null || level.isClientSide) return;
        BlockState old = getBlockState();
        BlockState next = old.setValue(TechFurnaceBlock.MODE, mode())
                .setValue(TechFurnaceBlock.CORE, coreTier() > 0)
                .setValue(TechFurnaceBlock.VISUAL_COLOR, effectiveVisualColor());
        if (!next.equals(old)) level.setBlock(worldPosition, next, 3);
    }

    public void setVisualColorIndex(int value) {
        int allowed = coreTier();
        if (value >= 0 && value <= allowed) {
            color = value;
            colorCustomized = allowed > 0;
        } else {
            color = 0;
            colorCustomized = false;
        }
        setChanged();
        refreshVisualState();
    }

    public int getCookTime() { return cook; }
    public int getCookTotal() { return cookTotal; }
    public int getBurnTime() { return burn; }
    public int getBurnTotal() { return burnTotal; }
    public boolean isBurning() { return burn > 0; }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : items) if (!stack.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, worldPosition, stack);
        returnOverflow();
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
        overflow.clear();
        net.minecraft.nbt.ListTag savedOverflow = tag.getList("SpecialOverflow", 10);
        for (int i = 0; i < savedOverflow.size(); i++) {
            ItemStack stack = ItemStack.of(savedOverflow.getCompound(i));
            if (!stack.isEmpty()) overflow.add(stack);
        }
        normalizeSpecialStacks();
        burn = Math.max(0, tag.getInt("Burn"));
        burnTotal = Math.max(0, tag.getInt("BurnTotal"));
        cook = Math.max(0, tag.getInt("Cook"));
        cookTotal = Math.max(1, tag.getInt("CookTotal"));
        color = tag.getInt("Color");
        colorCustomized = tag.getBoolean("ColorCustomized");
        pendingExperience = Math.max(0, tag.getFloat("PendingExperience"));
        if (color < 0 || color > coreTier()) {
            color = 0;
            colorCustomized = false;
        } else if (color == 0 && coreTier() > 0 && !colorCustomized) {
            color = coreTier();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        net.minecraft.nbt.ListTag savedOverflow = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : overflow) savedOverflow.add(stack.save(new CompoundTag()));
        tag.put("SpecialOverflow", savedOverflow);
        tag.putInt("Burn", burn);
        tag.putInt("BurnTotal", burnTotal);
        tag.putInt("Cook", cook);
        tag.putInt("CookTotal", cookTotal);
        tag.putInt("Color", color);
        tag.putBoolean("ColorCustomized", colorCustomized);
        tag.putFloat("PendingExperience", pendingExperience);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public Component getDisplayName() { return Component.translatable("block.fornalhastech.tech_furnace"); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TechFurnaceMenu(id, inventory, this, worldPosition);
    }
    @Override public int getContainerSize() { return 6; }
    @Override public boolean isEmpty() { for (ItemStack stack : items) if (!stack.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int index) { return items.get(index); }

    private void normalizeColorAfterCoreChange(int previousTier) {
        int newTier = coreTier();
        if (newTier == 0) { color = 0; colorCustomized = false; }
        else if (previousTier == 0 && !colorCustomized) color = newTier;
        else if (color > newTier) { color = 0; colorCustomized = false; }
    }

    @Override public ItemStack removeItem(int index, int count) {
        int oldTier = index == 3 ? coreTier() : -1;
        ItemStack removed = net.minecraft.world.ContainerHelper.removeItem(items, index, count);
        if (!removed.isEmpty()) {
            setChanged();
            if (index == 3 || index == 4) {
                if (index == 3) normalizeColorAfterCoreChange(oldTier);
                refreshVisualState();
            }
        }
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int index) {
        int oldTier = index == 3 ? coreTier() : -1;
        ItemStack removed = net.minecraft.world.ContainerHelper.takeItem(items, index);
        if (index == 3 || index == 4) {
            if (index == 3) normalizeColorAfterCoreChange(oldTier);
            refreshVisualState();
        }
        return removed;
    }

    @Override public void setItem(int index, ItemStack stack) {
        int oldTier = index == 3 ? coreTier() : -1;
        items.set(index, stack);
        if (index >= 3) normalizeSpecialStacks();
        else stack.setCount(Math.min(stack.getCount(), getMaxStackSize()));
        setChanged();
        if (index == 3 || index == 4) {
            if (index == 3) normalizeColorAfterCoreChange(oldTier);
            refreshVisualState();
        }
    }

    public int getSlotLimit(int slot) { return slot >= 3 && slot < 6 ? 1 : getMaxStackSize(); }

    private void normalizeSpecialStacks() {
        for (int slot = 3; slot < 6; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.getCount() > 1) {
                ItemStack excess = stack.copy();
                excess.setCount(stack.getCount() - 1);
                overflow.add(excess);
                stack.setCount(1);
                setChanged();
            }
        }
    }

    private void returnOverflow() {
        if (level == null || level.isClientSide || overflow.isEmpty()) return;
        java.util.Iterator<ItemStack> pending = overflow.iterator();
        while (pending.hasNext()) {
            ItemStack stack = pending.next();
            net.minecraft.world.entity.item.ItemEntity dropped = new net.minecraft.world.entity.item.ItemEntity(
                    level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, stack.copy());
            dropped.setDefaultPickUpDelay();
            // Remove from the saved recovery queue only after the world accepts the item.
            if (level.addFreshEntity(dropped)) { pending.remove(); setChanged(); }
        }
    }

    private net.minecraftforge.items.IItemHandler createItemHandler() {
        return new net.minecraftforge.items.wrapper.InvWrapper(this) {
            @Override public int getSlotLimit(int slot) { return TechFurnaceEntity.this.getSlotLimit(slot); }
        };
    }

    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.core.Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
            return itemCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = net.minecraftforge.common.util.LazyOptional.of(this::createItemHandler);
    }

    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case 0 -> !isComponent(stack);
            case 1 -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            case 3 -> items.get(3).isEmpty() && isCore(stack);
            case 4 -> items.get(4).isEmpty() && isModule(stack);
            default -> false;
        };
    }

    private boolean isComponent(ItemStack stack) { return isCore(stack) || isModule(stack); }
    private boolean isCore(ItemStack stack) {
        return stack.is(ModItems.CORE_BASE.get()) || stack.is(ModItems.CORE_IRON.get())
                || stack.is(ModItems.CORE_GOLD.get()) || stack.is(ModItems.CORE_EMERALD.get())
                || stack.is(ModItems.CORE_DIAMOND.get()) || stack.is(ModItems.CORE_NETHERITE.get());
    }
    private boolean isModule(ItemStack stack) {
        return stack.is(ModItems.MODULE_BLAST_FURNACE.get()) || stack.is(ModItems.MODULE_SMOKER.get());
    }

    public ItemStack removeModule() {
        ItemStack module = items.get(4);
        if (module.isEmpty()) return ItemStack.EMPTY;
        items.set(4, ItemStack.EMPTY);
        setChanged();
        refreshVisualState();
        return module;
    }

    public boolean installModule(ItemStack held) {
        if (!items.get(4).isEmpty() || !isModule(held)) return false;
        ItemStack installed = held.copy();
        installed.setCount(1);
        items.set(4, installed);
        held.shrink(1);
        setChanged();
        refreshVisualState();
        return true;
    }

    @Override public void clearContent() {
        items.clear();
        color = 0;
        colorCustomized = false;
        pendingExperience = 0;
        setChanged();
        refreshVisualState();
    }
    @Override public boolean stillValid(Player player) { return worldPosition.distToCenterSqr(player.position()) <= 64.0D; }
}
