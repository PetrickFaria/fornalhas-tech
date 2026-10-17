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

