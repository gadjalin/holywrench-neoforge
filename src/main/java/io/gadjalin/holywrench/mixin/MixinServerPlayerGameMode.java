package io.gadjalin.holywrench.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayerGameMode.class)
public abstract class MixinServerPlayerGameMode {
    @Shadow
    protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;

    private int wrenchCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo info) {
        if (wrenchCooldown > 0)
            wrenchCooldown--;
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)Z"), cancellable = true)
    public void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face, int maxBuildHeight, int sequence, CallbackInfo info) {
        ItemStack stack = this.player.getMainHandItem();
        if (wrenchCooldown == 0 && stack.is(Items.DEBUG_STICK)) {
            stack.getItem().canAttackBlock(this.level.getBlockState(pos), this.level, pos, this.player);
            wrenchCooldown = 4;
            info.cancel();
        }
    }

    @Redirect(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"))
    public boolean handleBlockBreakAction_mismatchfix(Object a, Object b) {
        return this.player.getMainHandItem().is(Items.DEBUG_STICK) || Objects.equals(a, b);
    }
}
