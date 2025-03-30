package io.gadjalin.holywrench;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

@Mod(HolywrenchMod.MODID)
public class HolywrenchMod {
    public static final String MODID = "holywrench";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static TagKey<Block> BLACKLIST = BlockTags.create(ResourceLocation.fromNamespaceAndPath(MODID, "blacklist"));

    public HolywrenchMod(IEventBus modBus) {
        modBus.addListener(HolywrenchMod::onBuildCreativeModeTabContents);
        //NeoForge.EVENT_BUS.addListener(HolywrenchMod::onBlockLeftClick);
        //NeoForge.EVENT_BUS.addListener(HolywrenchMod::onBlockRightClick);
    }

    // Add debug stick to creative inventory
    private static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(Items.DEBUG_STICK);
        }
    }
}
