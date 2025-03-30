package io.gadjalin.holywrench.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mixin({DebugStickItem.class})
public abstract class MixinDebugStickItem extends Item {
    private static final Set<Property<?>> allowedProperties;
    private static final Map<Property<?>, Predicate<BlockState>> specialStates;
    private static final Map<Property<?>, Predicate<Object>> specialProperties;

    public MixinDebugStickItem(Properties properties) { super(properties); }

    @Shadow
    private static void message(Player player, Component messageComponent) {}

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("HolyWrench");
    }

    @Overwrite
    private boolean handleInteraction(Player player, BlockState stateClicked, LevelAccessor accessor, BlockPos pos, boolean shouldCycleState, ItemStack debugStack) {
        Holder<Block> holder = stateClicked.getBlockHolder();
        StateDefinition<Block, BlockState> stateDefinition = holder.value().getStateDefinition();
        Collection<Property<?>> collection;
        // Filter allowed properties if in survival
        if (player.isCreative())
            collection = stateDefinition.getProperties();
        else
            collection = filterStateProperties(stateDefinition.getProperties(), stateClicked);

        if (collection.isEmpty()) {
            message(player, Component.translatable(this.getDescriptionId() + ".empty", holder.getRegisteredName()));
            return false;
        } else {
            DebugStickState debugstickstate = debugStack.get(DataComponents.DEBUG_STICK_STATE);
            if (debugstickstate == null) {
                return false;
            } else {
                Property<?> property = debugstickstate.properties().get(holder);
                if (shouldCycleState) {
                    // Edge-case: If select a creative-only property (e.g. waterlogged) and then switch back to survival
                    if (property == null || !collection.contains(property)) {
                        property = collection.iterator().next();
                    }

                    // Filter allowed states in survival
                    BlockState blockstate;
                    if (player.isCreative())
                        blockstate = cycleState(stateClicked, property, player.isSecondaryUseActive());
                    else
                        blockstate = cycleFilteredState(stateClicked, property, player.isSecondaryUseActive());
                    accessor.setBlock(pos, blockstate, 18);
                    message(player, Component.translatable(this.getDescriptionId() + ".update", property.getName(), getNameHelper(blockstate, property)));
                } else {
                    // Edge-case: If select a creative-only property (e.g. waterlogged) and then switch back to survival
                    // In practice, this locks the current thread in an infinite loop, looking for a property that is not in the collection
                    if (!collection.contains(property)) {
                        property = collection.iterator().next();
                    }

                    property = getRelative(collection, property, player.isSecondaryUseActive());
                    debugStack.set(DataComponents.DEBUG_STICK_STATE, debugstickstate.withProperty(holder, property));
                    message(player, Component.translatable(this.getDescriptionId() + ".select", property.getName(), getNameHelper(stateClicked, property)));
                }

                return true;
            }
        }
    }

    private static <T extends Comparable<T>> BlockState cycleFilteredState(BlockState state, Property<T> property, boolean backwards) {
        return state.setValue(property, getRelative(filterPropertyValues(property.getPossibleValues(), property), state.getValue(property), backwards));
    }

    @Shadow
    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean backwards) { return null; }

    @Shadow
    private static <T> T getRelative(Iterable<T> allowedValues, @Nullable T currentValue, boolean backwards) { return null; }

    @Shadow
    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) { return null; }

    private static Collection<Property<?>> filterStateProperties(Collection<Property<?>> properties, BlockState stateClicked) {
        return properties.stream()
                .filter(property -> (allowedProperties.contains(property) &&
                        specialStates.getOrDefault(property, state -> true).test(stateClicked)))
                .collect(Collectors.toList());
    }

    private static <T> Collection<T> filterPropertyValues(Collection<T> values, Property<?> property) {
        // Someday I will try to make this a bit safer
        return values.stream()
                .filter(value -> specialProperties.getOrDefault(property, obj -> true).test(value))
                .collect(Collectors.toList());
    }

    static {
        allowedProperties = Set.of(
                BlockStateProperties.ATTACHED,
                BlockStateProperties.IN_WALL,
                BlockStateProperties.LIT,
                BlockStateProperties.OPEN,
                BlockStateProperties.AXIS,
                BlockStateProperties.SNOWY,
                BlockStateProperties.UP,
                BlockStateProperties.DOWN,
                BlockStateProperties.NORTH,
                BlockStateProperties.EAST,
                BlockStateProperties.SOUTH,
                BlockStateProperties.WEST,
                BlockStateProperties.FACING,
                BlockStateProperties.FACING_HOPPER,
                BlockStateProperties.HORIZONTAL_FACING,
                BlockStateProperties.ORIENTATION,
                BlockStateProperties.ATTACH_FACE,
                BlockStateProperties.BELL_ATTACHMENT,
                BlockStateProperties.NORTH_WALL,
                BlockStateProperties.EAST_WALL,
                BlockStateProperties.SOUTH_WALL,
                BlockStateProperties.WEST_WALL,
                BlockStateProperties.HALF,
                BlockStateProperties.RAIL_SHAPE,
                BlockStateProperties.RAIL_SHAPE_STRAIGHT,
                BlockStateProperties.AGE_1,
                BlockStateProperties.LEVEL,
                BlockStateProperties.ROTATION_16,
                BlockStateProperties.DOOR_HINGE,
                BlockStateProperties.SLAB_TYPE,
                BlockStateProperties.STAIRS_SHAPE,
                BlockStateProperties.BAMBOO_LEAVES,
                BlockStateProperties.TILT,
                BlockStateProperties.DRIPSTONE_THICKNESS,
                BlockStateProperties.CRAFTING
        );

        specialStates = Map.of(
                BlockStateProperties.FACING, state -> {
                    if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))
                        return state.getValue(ChestBlock.TYPE) != ChestType.LEFT || state.getValue(ChestBlock.TYPE) != ChestType.RIGHT;
                    else if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON))
                        return !state.getValue(PistonBaseBlock.EXTENDED);
                    else
                        return true;
                },
                BlockStateProperties.UP, state -> {
                    if (state.getBlock() instanceof WallBlock)
                        return false;
                    else
                        return true;
                },
                BlockStateProperties.SLAB_TYPE, state -> {
                    return state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
                },
                BlockStateProperties.AGE_1, state -> {
                    return state.is(Blocks.BAMBOO);
                },
                BlockStateProperties.LEVEL, state -> {
                    return state.is(Blocks.LIGHT);
                }
        );

        specialProperties = Map.of(
                BlockStateProperties.SLAB_TYPE, value -> {
                    return !value.equals(SlabType.DOUBLE);
                }
        );
    }
}
