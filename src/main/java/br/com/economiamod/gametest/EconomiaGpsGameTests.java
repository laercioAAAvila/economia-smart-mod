package br.com.economiamod.gametest;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.gps.GpsPresence;
import br.com.economiamod.compat.curios.CuriosGpsBridge;
import br.com.economiamod.registry.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 0.5.4 GPS: its recipe, and that it counts in the Curios GPS slot and in a hand (what turns the minimap on). */
@GameTestHolder(EconomiaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EconomiaGpsGameTests {
    private static final TagKey<Item> CURIOS_GPS_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", GpsPresence.CURIOS_SLOT));

    private EconomiaGpsGameTests() {
    }

    /** Compass + iron ingot + redstone dust, in any place of the grid, make one GPS; missing one makes nothing. */
    @GameTest(template = "empty", timeoutTicks = 20, batch = "economia_gps")
    public static void compassIronAndRedstoneCraftTheGps(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack crafted = craft(level, Items.REDSTONE, Items.COMPASS, Items.IRON_INGOT);
        require(crafted.is(ModItems.GPS.get()), "compass + iron + redstone must craft the GPS, got " + crafted);
        require(crafted.getCount() == 1, "one GPS per craft, got " + crafted.getCount());
        require(crafted.getMaxStackSize() == 1, "the GPS does not stack");
        require(!craft(level, Items.COMPASS, Items.IRON_INGOT).is(ModItems.GPS.get()), "without redstone: no GPS");
        require(!craft(level, Items.COMPASS, Items.REDSTONE).is(ModItems.GPS.get()), "without iron: no GPS");
        require(!craft(level, Items.IRON_INGOT, Items.REDSTONE).is(ModItems.GPS.get()), "without compass: no GPS");
        helper.succeed();
    }

    /** The minimap shows for a GPS in the Curios GPS slot or in either hand, never for a plain compass. */
    @GameTest(template = "empty", timeoutTicks = 20, batch = "economia_gps")
    public static void theGpsCountsInTheCuriosSlotAndInTheHand(GameTestHelper helper) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.nameUUIDFromBytes("economia_gps".getBytes()), "economia_gps"));
        player.getInventory().clearContent();
        require(new ItemStack(ModItems.GPS.get()).is(CURIOS_GPS_TAG), "the GPS must be valid for the curios:gps slot");
        require(!GpsPresence.inHand(player) && !GpsPresence.inCuriosSlot(player), "empty-handed player has no GPS");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.GPS.get()));
        require(GpsPresence.inHand(player), "a GPS in the off hand counts");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COMPASS));
        require(!GpsPresence.inHand(player), "a compass is not a GPS");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (GpsPresence.curiosAvailable()) {
            require(CuriosGpsBridge.placeInGpsSlot(player, new ItemStack(Items.COMPASS)),
                    "players must have the Curios GPS slot");
            require(!GpsPresence.inCuriosSlot(player), "a compass in the GPS slot is not a GPS");
            CuriosGpsBridge.placeInGpsSlot(player, new ItemStack(ModItems.GPS.get()));
            require(GpsPresence.inCuriosSlot(player), "a GPS in the Curios GPS slot must turn the minimap on");
            require(!GpsPresence.inHand(player), "the slot is not the hand");
            CuriosGpsBridge.placeInGpsSlot(player, ItemStack.EMPTY);
            require(!GpsPresence.inCuriosSlot(player), "taking the GPS out of the slot turns the minimap off");
        } else {
            EconomiaMod.LOGGER.warn("Curios ausente: a parte do slot GPS do GameTest foi pulada.");
        }
        helper.succeed();
    }

    private static ItemStack craft(ServerLevel level, Item... items) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        int[] places = {4, 0, 8};
        for (int i = 0; i < items.length; i++) grid.set(places[i], new ItemStack(items[i]));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
