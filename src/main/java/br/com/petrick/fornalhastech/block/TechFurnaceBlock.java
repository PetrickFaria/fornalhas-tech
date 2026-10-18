package br.com.petrick.fornalhastech.block;
import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.particles.DustParticleOptions;
import br.com.petrick.fornalhastech.registry.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import br.com.petrick.fornalhastech.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;
public class TechFurnaceBlock extends BaseEntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public static final BooleanProperty LIT=BlockStateProperties.LIT;
 public static final BooleanProperty CORE=BooleanProperty.create("core");
 public static final BooleanProperty COOKING=BooleanProperty.create("cooking");
 public static final IntegerProperty MODE=IntegerProperty.create("mode",0,2);
 public static final IntegerProperty VISUAL_COLOR=IntegerProperty.create("visual_color",0,6);
 public TechFurnaceBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false).setValue(CORE,false).setValue(COOKING,false).setValue(MODE,0).setValue(VISUAL_COLOR,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FACING,LIT,CORE,COOKING,MODE,VISUAL_COLOR);}
 @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 public static int getTechnologyColor(int color) {
  return switch (color) {
   case 1 -> 0xFFE83E3E;
   case 2 -> 0xFFE8EFF1;
   case 3 -> 0xFFFFC844;
   case 4 -> 0xFF43D878;
   case 5 -> 0xFF42D8ED;
   case 6 -> 0xFFAD704A;
   default -> 0xFF9AA6AA;
  };
 }
 @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
  if (state.getValue(MODE) == 0 && state.getValue(LIT) && random.nextFloat() < 0.35F) spawnFireParticle(state, level, pos, random);
  if (!state.getValue(COOKING) || random.nextFloat() >= 0.525F) return;
  int color = getTechnologyColor(state.getValue(VISUAL_COLOR));
  float red = ((color >> 16) & 255) / 255F;
  float green = ((color >> 8) & 255) / 255F;
  float blue = (color & 255) / 255F;
  DustParticleOptions particle = new DustParticleOptions(new com.mojang.math.Vector3f(red, green, blue), 0.55F);
  double x = pos.getX() + 0.28 + random.nextDouble() * 0.44;
  double y = pos.getY() + 0.96 + random.nextDouble() * 0.12;
  double z = pos.getZ() + 0.28 + random.nextDouble() * 0.44;
  level.addParticle(particle, x, y, z,
      (random.nextDouble() - 0.5) * 0.008, 0.012 + random.nextDouble() * 0.015,
      (random.nextDouble() - 0.5) * 0.008);
 }
 private static void spawnFireParticle(BlockState state, Level level, BlockPos pos, RandomSource random) {
  Direction facing = state.getValue(FACING);
  double across = 0.28125 + random.nextDouble() * 0.4375;
  double y = pos.getY() + 0.24 + random.nextDouble() * 0.14;
  double x, z, vx, vz;
  double drift = (random.nextDouble() - 0.5) * 0.012;
  switch (facing) {
   // Screen-left to screen-right across each physical front face, with a slight
   // outward offset so the sparks emerge from the same recessed fire opening.
   case SOUTH -> { x = pos.getX() + 1.0 - across; z = pos.getZ() + 1.012; vx = -drift; vz = 0.006; }
   case EAST -> { x = pos.getX() + 1.012; z = pos.getZ() + across; vx = 0.006; vz = drift; }
   case WEST -> { x = pos.getX() - 0.012; z = pos.getZ() + 1.0 - across; vx = -0.006; vz = -drift; }
   default -> { x = pos.getX() + across; z = pos.getZ() - 0.012; vx = drift; vz = -0.006; }
  }
  level.addParticle(ModParticles.SMALL_FIRE.get(), x, y, z, vx, 0.018 + random.nextDouble() * 0.018, vz);
 }
 @Override public boolean isSignalSource(BlockState state) { return true; }
 @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
  return state.getValue(COOKING) ? 15 : 0;
 }
 @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
  return state.getValue(COOKING) ? 15 : 0;
 }
 @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
 @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
  BlockEntity blockEntity = level.getBlockEntity(pos);
  return blockEntity instanceof Container container
      ? AbstractContainerMenu.getRedstoneSignalFromContainer(container) : 0;
 }
 @Nullable @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TechFurnaceEntity(p,s);}
 @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.TECH_FURNACE.get(),TechFurnaceEntity::tick);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){
  if(player.isShiftKeyDown() && !l.isClientSide && l.getBlockEntity(p) instanceof TechFurnaceEntity te){
   net.minecraft.world.item.ItemStack held=player.getItemInHand(h);
   if(te.installModule(held)) return InteractionResult.CONSUME;
   if(held.isEmpty()) { net.minecraft.world.item.ItemStack module=te.removeModule(); if(!module.isEmpty()){ if(!player.getInventory().add(module)) net.minecraft.world.level.block.Block.popResource(l,p,module); return InteractionResult.CONSUME; } }
  }
  if(!l.isClientSide&&player instanceof ServerPlayer sp){BlockEntity be=l.getBlockEntity(p);if(be instanceof TechFurnaceEntity)NetworkHooks.openScreen(sp,(TechFurnaceEntity)be,p);}return InteractionResult.sidedSuccess(l.isClientSide);
 }
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState ns,boolean moving){if(!s.is(ns.getBlock())){BlockEntity be=l.getBlockEntity(p);if(be instanceof TechFurnaceEntity te)te.dropContents();super.onRemove(s,l,p,ns,moving);}else super.onRemove(s,l,p,ns,moving);}
}



