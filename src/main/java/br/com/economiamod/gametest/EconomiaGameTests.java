package br.com.economiamod.gametest;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.common.menu.PlayerShopMenu;
import br.com.economiamod.common.menu.SellShopMenu;
import br.com.economiamod.registry.ModBlocks;
import br.com.economiamod.registry.ModItems;
import br.com.economiamod.server.cash.CashInventoryService;
import br.com.economiamod.server.commercial.CommercialBlockSqlService;
import br.com.economiamod.server.mail.MailInventoryService;
import br.com.economiamod.server.persistence.EconomyDatabaseState;
import com.mojang.authlib.GameProfile;
import java.sql.SQLException;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The 0.5.2 fixes on a live server with the real database (SQLite by default, or whatever economia-server.toml points
 * to): exact cash payment never stalls the server, a mailbox owner taking items never erases a shipment that arrived
 * meanwhile, and two customers can never take the same shop stock.
 */
@GameTestHolder(EconomiaMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EconomiaGameTests {
    private EconomiaGameTests() {
    }

    /** Paying 1001 with only even notes is impossible: it must say so at once, and an exact payment must be exact. */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "economia_dinheiro")
    public static void exactCashNeverStallsAndPaysExactly(GameTestHelper helper) {
        FakePlayer player = fakePlayer(helper.getLevel(), "economia_caixa");
        player.getInventory().clearContent();
        Item[] evenNotes = {ModItems.BANKNOTE_2.get(), ModItems.BANKNOTE_10.get(), ModItems.BANKNOTE_20.get(),
                ModItems.BANKNOTE_50.get(), ModItems.BANKNOTE_100.get(), ModItems.BANKNOTE_200.get()};
        for (Item note : evenNotes) {
            player.getInventory().add(new ItemStack(note, 64));
        }
        CashInventoryService cash = new CashInventoryService();
        long total = cash.totalBanknotes(player);

        long started = System.nanoTime();
        boolean paid = cash.removeExactValue(player, 1001L);
        double millis = (System.nanoTime() - started) / 1.0e6D;
        require(!paid, "1001 cannot be paid exactly with even notes, but it was paid");
        require(millis < 50.0D, "An impossible exact payment must fail at once, it took " + millis + " ms");
        require(cash.totalBanknotes(player) == total, "A refused payment must not take any note");

        player.getInventory().add(new ItemStack(ModItems.BANKNOTE_1.get(), 1));
        require(cash.removeExactValue(player, 1001L), "With one 1-note, 1001 must be payable exactly");
        require(cash.totalBanknotes(player) == total + 1L - 1001L,
                "Exactly 1001 must leave the inventory, left " + cash.totalBanknotes(player) + " of " + (total + 1L));
        player.getInventory().clearContent();
        helper.succeed();
    }

    /** The owner takes 4 of 10 while 5 more and 3 stone arrive: the mailbox must keep 11 and the 3 stone. */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "economia_correio")
    public static void takingFromTheMailboxKeepsShipmentsThatArrived(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        requireDatabase();
        var registries = level.registryAccess();
        UUID mailbox = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        MailInventoryService mail = new MailInventoryService();
        try {
            new CommercialBlockSqlService().registerPlacedBlock(mailbox, CommercialBlockType.MAIL, owner, owner,
                    level.dimension().location(), helper.absolutePos(BlockPos.ZERO));
            require(mail.insertShipment(mailbox, container(new ItemStack(Items.DIRT, 10)), registries), "First shipment refused");

            SimpleContainer openedByOwner = mail.loadReceived(mailbox, registries);
            SimpleContainer afterTaking = copy(openedByOwner);
            afterTaking.getItem(0).shrink(4);

            require(mail.insertShipment(mailbox, container(new ItemStack(Items.DIRT, 5), new ItemStack(Items.STONE, 3)), registries),
                    "Second shipment refused");
            mail.saveReceivedRemovals(mailbox, openedByOwner, afterTaking, registries);

            SimpleContainer stored = mail.loadReceived(mailbox, registries);
            int dirt = count(stored, Items.DIRT);
            int stone = count(stored, Items.STONE);
            require(dirt == 11 && stone == 3, "The mailbox must hold 11 dirt and 3 stone, it holds " + dirt + " dirt and " + stone + " stone");
        } catch (SQLException exception) {
            throw new GameTestAssertException("Database error: " + exception.getMessage());
        }
        helper.succeed();
    }

    /** One diamond in stock and two customers who opened the shop before either bought: only one gets it. */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "economia_loja")
    public static void twoCustomersCannotTakeTheSameStock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        requireDatabase();
        BlockPos shopPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.SELL_SHOP.get());
        UUID shop = UUID.randomUUID();
        FakePlayer owner = fakePlayer(level, "economia_dono");
        FakePlayer customerA = fakePlayer(level, "economia_cliente_a");
        FakePlayer customerB = fakePlayer(level, "economia_cliente_b");
        for (FakePlayer player : new FakePlayer[]{owner, customerA, customerB}) {
            player.getInventory().clearContent();
        }
        try {
            new CommercialBlockSqlService().registerPlacedBlock(shop, CommercialBlockType.SELL_SHOP, owner.getUUID(), owner.getUUID(),
                    level.dimension().location(), shopPos);
        } catch (SQLException exception) {
            throw new GameTestAssertException("Database error: " + exception.getMessage());
        }

        SellShopMenu ownerMenu = new SellShopMenu(1, owner.getInventory(), shop, owner.getUUID(), shopPos, ModBlocks.SELL_SHOP.get(), true);
        ownerMenu.setCarried(new ItemStack(Items.DIAMOND));
        ownerMenu.clicked(PlayerShopMenu.REFERENCE_SLOT, 0, ClickType.PICKUP, owner);
        ownerMenu.setCarried(ItemStack.EMPTY);
        ownerMenu.getSlot(PlayerShopMenu.STOCK_START).set(new ItemStack(Items.DIAMOND, 1));
        ownerMenu.saveOwnerConfig(owner, 10L, 1, true);
        require(ownerMenu.active(), "The shop must be open for sale with one diamond in stock");
        ownerMenu.removed(owner);

        SellShopMenu menuA = new SellShopMenu(2, customerA.getInventory(), shop, owner.getUUID(), shopPos, ModBlocks.SELL_SHOP.get(), false);
        SellShopMenu menuB = new SellShopMenu(3, customerB.getInventory(), shop, owner.getUUID(), shopPos, ModBlocks.SELL_SHOP.get(), false);
        require(menuA.availableStock() == 1 && menuB.availableStock() == 1, "Both customers must see the one diamond");
        menuA.getSlot(PlayerShopMenu.CARD_SLOT).set(new ItemStack(ModItems.BANKNOTE_10.get(), 1));
        menuB.getSlot(PlayerShopMenu.CARD_SLOT).set(new ItemStack(ModItems.BANKNOTE_10.get(), 1));

        menuA.trade(customerA, UUID.randomUUID(), 1, false);
        menuB.trade(customerB, UUID.randomUUID(), 1, false);
        int diamondsA = customerA.getInventory().countItem(Items.DIAMOND);
        int diamondsB = customerB.getInventory().countItem(Items.DIAMOND);
        menuA.removed(customerA);
        menuB.removed(customerB);

        SellShopMenu reopened = new SellShopMenu(4, owner.getInventory(), shop, owner.getUUID(), shopPos, ModBlocks.SELL_SHOP.get(), true);
        int stockLeft = reopened.availableStock();
        reopened.removed(owner);
        require(diamondsA + diamondsB == 1, "One diamond in stock, but the customers got " + diamondsA + " + " + diamondsB);
        require(stockLeft == 0, "The sold diamond must leave the stock, " + stockLeft + " left");
        int notesBack = customerA.getInventory().countItem(ModItems.BANKNOTE_10.get()) + customerB.getInventory().countItem(ModItems.BANKNOTE_10.get());
        require(notesBack == 1, "The customer who got nothing must get the payment back, notes returned " + notesBack);
        helper.succeed();
    }

    /**
     * A buying shop's reference is a copy the client chose (any item, any components). Breaking the shop must drop the
     * stock only: before 0.5.2 the reference dropped too, an item created from nothing on every break.
     */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "economia_referencia")
    public static void breakingAShopNeverDropsItsReference(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        requireDatabase();
        BlockPos shopPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.BUY_SHOP.get());
        UUID shop = UUID.randomUUID();
        FakePlayer owner = fakePlayer(level, "economia_dono_compra");
        try {
            new CommercialBlockSqlService().registerPlacedBlock(shop, CommercialBlockType.BUY_SHOP, owner.getUUID(), owner.getUUID(),
                    level.dimension().location(), shopPos);
        } catch (SQLException exception) {
            throw new GameTestAssertException("Database error: " + exception.getMessage());
        }
        br.com.economiamod.common.menu.BuyShopMenu ownerMenu = new br.com.economiamod.common.menu.BuyShopMenu(
                1, owner.getInventory(), shop, owner.getUUID(), shopPos, ModBlocks.BUY_SHOP.get(), true);
        ownerMenu.setBuyReferenceFromClient(owner, new ItemStack(Items.NETHERITE_INGOT));
        ownerMenu.getSlot(PlayerShopMenu.STOCK_START).set(new ItemStack(Items.DIAMOND, 2));
        ownerMenu.removed(owner);

        try {
            new br.com.economiamod.server.commercial.CommercialBlockDropService().dropStoredItems(level, shopPos, shop);
        } catch (SQLException exception) {
            throw new GameTestAssertException("Database error: " + exception.getMessage());
        }
        int netherite = droppedCount(level, shopPos, Items.NETHERITE_INGOT);
        int diamonds = droppedCount(level, shopPos, Items.DIAMOND);
        require(netherite == 0, "The reference must not drop, " + netherite + " netherite ingot(s) appeared");
        require(diamonds == 2, "The 2 diamonds of stock must drop, " + diamonds + " did");
        helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
        helper.succeed();
    }

    /** Outside any claim a mailbox has no protection of its own: another player may break it and its mail drops. */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "economia_sem_claim")
    public static void outsideAClaimAnyoneMayBreakAMailboxAndItsMailDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        requireDatabase();
        BlockPos mailPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MAIL.get());
        if (!(level.getBlockEntity(mailPos) instanceof br.com.economiamod.common.block.CommercialBlockEntity blockEntity)) {
            throw new GameTestAssertException("The mailbox has no block entity");
        }
        UUID mailbox = blockEntity.commercialBlockId();
        FakePlayer owner = fakePlayer(level, "economia_dono_correio");
        FakePlayer stranger = fakePlayer(level, "economia_estranho");
        var registries = level.registryAccess();
        try {
            new CommercialBlockSqlService().registerPlacedBlock(mailbox, CommercialBlockType.MAIL, owner.getUUID(), owner.getUUID(),
                    level.dimension().location(), mailPos);
            require(new MailInventoryService().insertShipment(mailbox, container(new ItemStack(Items.EMERALD, 7)), registries),
                    "Shipment refused");
        } catch (SQLException exception) {
            throw new GameTestAssertException("Database error: " + exception.getMessage());
        }
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, mailPos, level.getBlockState(mailPos), stranger);
        new br.com.economiamod.server.commercial.CommercialBlockLifecycleService().onBlockBroken(event);
        require(!event.isCanceled(), "Outside a claim another player must be able to break the mailbox");
        int emeralds = droppedCount(level, mailPos, Items.EMERALD);
        require(emeralds == 7, "The 7 emeralds in the mailbox must drop, " + emeralds + " did");
        helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
        helper.succeed();
    }

    private static int droppedCount(ServerLevel level, BlockPos pos, Item item) {
        int total = 0;
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3.0D))) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }

    private static void requireDatabase() {
        require(EconomyDatabaseState.isAvailable(), "The economy database did not start");
    }

    private static FakePlayer fakePlayer(ServerLevel level, String name) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
    }

    private static SimpleContainer container(ItemStack... stacks) {
        SimpleContainer container = new SimpleContainer(9);
        for (int slot = 0; slot < stacks.length; slot++) {
            container.setItem(slot, stacks[slot]);
        }
        return container;
    }

    private static SimpleContainer copy(Container source) {
        SimpleContainer copy = new SimpleContainer(source.getContainerSize());
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            copy.setItem(slot, source.getItem(slot).copy());
        }
        return copy;
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) {
                total += container.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }
}
