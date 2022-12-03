package me.aleksilassila.litematica.printer.v1_19.guides;

import me.aleksilassila.litematica.printer.v1_19.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.v1_19.PrinterPlacementContext;
import me.aleksilassila.litematica.printer.v1_19.SchematicBlockState;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * This is the placement guide that most blocks will use.
 * It will try to predict the correct player state for producing the right blockState
 * by brute forcing the correct hit vector and look direction.
 */
public class GuesserGuide extends BlockPlacementGuide {
    private PrinterPlacementContext contextCache = null;

    protected static Direction[] directionsToTry = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.EAST,
            Direction.WEST,
            Direction.UP,
            Direction.DOWN
    };
    protected static Vec3d[] hitVecsToTry = new Vec3d[]{
            new Vec3d(-0.25, -0.25, -0.25),
            new Vec3d(+0.25, -0.25, -0.25),
            new Vec3d(-0.25, +0.25, -0.25),
            new Vec3d(-0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, -0.25),
            new Vec3d(-0.25, +0.25, +0.25),
            new Vec3d(+0.25, -0.25, +0.25),
            new Vec3d(+0.25, +0.25, +0.25),
    };

    public GuesserGuide(SchematicBlockState state) {
        super(state);
    }

    @Nullable
    @Override
    public PrinterPlacementContext getPlacementContext(ClientPlayerEntity player) {
        if (contextCache != null && !LitematicaMixinMod.DEBUG) return contextCache;

        ItemStack requiredItem = getRequiredItem(player).orElse(ItemStack.EMPTY);

        for (Direction lookDirection : directionsToTry) {
            for (Direction side : directionsToTry) {
                BlockPos neighborPos = state.blockPos.offset(side);
                BlockState neighborState = state.world.getBlockState(neighborPos);
                boolean requiresShift = isInteractive(neighborState.getBlock());

                if (!canBeClicked(state.world, neighborPos) || // Handle unclickable grass for example
                        neighborState.getMaterial().isReplaceable())
                    continue;

                Vec3d hitVec = Vec3d.ofCenter(state.blockPos)
                        .add(Vec3d.of(side.getVector()).multiply(0.5));

                for (Vec3d hitVecToTry : hitVecsToTry) {
                    Vec3d multiplier = Vec3d.of(side.getVector());
                    multiplier = new Vec3d(multiplier.x == 0 ? 1 : 0, multiplier.y == 0 ? 1 : 0, multiplier.z == 0 ? 1 : 0);

                    BlockHitResult hitResult = new BlockHitResult(hitVec.add(hitVecToTry.multiply(multiplier)), side.getOpposite(), neighborPos, false);
                    PrinterPlacementContext context = new PrinterPlacementContext(player, hitResult, requiredItem, getSlotWithItem(player, requiredItem), lookDirection, requiresShift);
                    BlockState result = targetState.getBlock().getPlacementState(context);

                    if (result != null && (statesEqual(result, targetState) || correctChestPlacement(targetState, result))) {
                        contextCache = context;
                        return context;
                    }
                }
            }
        }

        return null;
    }

    @Override
    protected boolean statesEqual(BlockState resultState, BlockState targetState) {
        return super.statesEqual(resultState, targetState);
    }

    private boolean correctChestPlacement(BlockState targetState, BlockState result) {
        if (targetState.contains(ChestBlock.CHEST_TYPE) && result.contains(ChestBlock.CHEST_TYPE) && result.get(ChestBlock.FACING) == targetState.get(ChestBlock.FACING)) {
            ChestType targetChestType = targetState.get(ChestBlock.CHEST_TYPE);
            ChestType resultChestType = result.get(ChestBlock.CHEST_TYPE);

            return targetChestType != ChestType.SINGLE && resultChestType == ChestType.SINGLE;
        }

        return false;
    }
}
