package br.com.petrick.fornalhastech.menu;

import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import br.com.petrick.fornalhastech.registry.ModItems;
import br.com.petrick.fornalhastech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;

public class TechFurnaceMenu extends AbstractContainerMenu {
    private final Container furnace;
    private final Level level;
    private final BlockPos pos;
    private int syncedCook;
    private int syncedCookTotal = 200;
    private int syncedBurn;
    private int syncedBurnTotal;
    private int syncedColor;
    private int syncedTier;

    public TechFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    private TechFurnaceMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, find(inventory, pos), pos);
    }

    private static Container find(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level.getBlockEntity(pos);
        return entity instanceof TechFurnaceEntity ? (TechFurnaceEntity) entity : new SimpleContainer(6);
    }

    public TechFurnaceMenu(int id, Inventory inventory, Container container, BlockPos pos) {
        super(ModMenus.TECH_FURNACE.get(), id);
        this.furnace = container;
        this.level = inventory.player.level;
        this.pos = pos;

        // Furnace-style layout: ingredient, fuel and output; the core is kept
        // in a compact corner slot. Modules retain their existing hidden slot.
        addSlot(new Slot(container, 0, 56, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return !isComponent(stack); }
        });
        addSlot(new Slot(container, 1, 56, 53) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            }
        });
        addSlot(new Slot(container, 2, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                if (container instanceof TechFurnaceEntity entity) entity.awardPendingExperience(player);
                super.onTake(player, stack);
            }
        });
        addSlot(new Slot(container, 3, 8, 17) {
            @Override public boolean mayPlace(ItemStack stack) { return isCore(stack); }
        });
        addSlot(new Slot(container, 4, -1000, -1000) {
            @Override public boolean mayPlace(ItemStack stack) { return isModule(stack); }
        });
        addSlot(new Slot(container, 5, -1000, -1000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        for (int i = 0; i < 6; i++) addDataSlot(dataSlot(i));
    }

    private DataSlot dataSlot(int index) {
        return new DataSlot() {
            @Override public int get() {
                if (level.isClientSide) {
                    return switch (index) {
                        case 0 -> syncedCook;
                        case 1 -> syncedCookTotal;
                        case 2 -> syncedBurn;
                        case 3 -> syncedBurnTotal;
                        case 4 -> syncedColor;
                        default -> syncedTier;
                    };
                }
                if (furnace instanceof TechFurnaceEntity entity) {
                    return switch (index) {
                        case 0 -> entity.getCookTime();
                        case 1 -> entity.getCookTotal();
                        case 2 -> entity.getBurnTime();
                        case 3 -> entity.getBurnTotal();
                        case 4 -> entity.getVisualColorIndex();
                        default -> entity.coreTier();
                    };
                }
                return 0;
            }

            @Override public void set(int value) {
                switch (index) {
                    case 0 -> syncedCook = value;
                    case 1 -> syncedCookTotal = value;
                    case 2 -> syncedBurn = value;
                    case 3 -> syncedBurnTotal = value;
                    case 4 -> syncedColor = value;
                    default -> syncedTier = value;
                }
            }
        };
    }

    public int getCookProgress() { return syncedCookTotal <= 0 ? 0 : Math.min(24, syncedCook * 24 / syncedCookTotal); }
    public int getBurnProgress() { return syncedBurnTotal <= 0 ? 0 : Math.min(13, syncedBurn * 13 / syncedBurnTotal); }
    public int getVisualColorIndex() { return syncedColor; }
    public int getCoreTier() { return syncedTier; }
    public BlockPos getBlockPos() { return pos; }

    public int getAccentColor() {
        return switch (syncedColor) {
            case 1 -> 0xFFE83E3E;
            case 2 -> 0xFFE8EFF1;
            case 3 -> 0xFFFFC844;
            case 4 -> 0xFF43D878;
            case 5 -> 0xFF42D8ED;
            case 6 -> 0xFFAD704A;
            default -> 0xFF000000;
        };
    }

    public void setVisualColorIndex(int color) {
        if (furnace instanceof TechFurnaceEntity entity) entity.setVisualColorIndex(color);
    }

    private static boolean isCore(ItemStack stack) {
        return stack.is(ModItems.CORE_BASE.get()) || stack.is(ModItems.CORE_IRON.get())
                || stack.is(ModItems.CORE_GOLD.get()) || stack.is(ModItems.CORE_EMERALD.get())
                || stack.is(ModItems.CORE_DIAMOND.get()) || stack.is(ModItems.CORE_NETHERITE.get());
    }

    private static boolean isModule(ItemStack stack) {
        return stack.is(ModItems.MODULE_BLAST_FURNACE.get()) || stack.is(ModItems.MODULE_SMOKER.get());
    }

    private static boolean isComponent(ItemStack stack) { return isCore(stack) || isModule(stack); }

    @Override public boolean stillValid(Player player) { return furnace.stillValid(player); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean isProcessable(ItemStack stack) {
        if (stack.isEmpty() || isComponent(stack)) return false;
        RecipeType type = furnace.getItem(4).is(ModItems.MODULE_BLAST_FURNACE.get()) ? RecipeType.BLASTING
                : furnace.getItem(4).is(ModItems.MODULE_SMOKER.get()) ? RecipeType.SMOKING : RecipeType.SMELTING;
        SimpleContainer candidate = new SimpleContainer(6);
        candidate.setItem(0, stack);
        return level.getRecipeManager().getRecipeFor(type, candidate, level).isPresent();
    }

    private boolean isFuel(ItemStack stack) { return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (source == null || !source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();

        if (index < 6) {
            if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int start;
            int end;
            if (isCore(stack)) { start = 3; end = 4; }
            else if (isModule(stack)) { start = 4; end = 5; }
            else if (isProcessable(stack)) { start = 0; end = 1; }
            else if (isFuel(stack)) { start = 1; end = 2; }
            else return ItemStack.EMPTY;
            // A recognized item has one destination; full slots do not cause fallback or loss.
            if (!moveItemStackTo(stack, start, end, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        if (index == 2) source.onTake(player, original);
        return original;
    }
}
