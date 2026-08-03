package com.exiledradio.rlcraftdeathoverhaul;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Settings are split into four categories so the list stays readable — there are enough
 * of them now that one flat block was hard to scan.
 *
 * <p>Health values are expressed in <em>whole hearts</em>, because that is the unit
 * players think in. Scaling Health's own config is in half-hearts, so anything crossing
 * into {@link ScalingHealthBridge} goes through the {@code ...Hp()} accessors at the
 * bottom of this class.
 */
@Mod.EventBusSubscriber(modid = RLCraftDeathOverhaul.MODID)
public class ModConfig {

    /** The heart cost of dying. */
    public static final String CATEGORY_HEARTS = "hearts";
    /** What survives a death, and what it costs to keep it. */
    public static final String CATEGORY_ITEMS = "items";
    /** Deaths that are ignored entirely. */
    public static final String CATEGORY_EXEMPTIONS = "exemptions";
    /** What the player is told, and who hears it. */
    public static final String CATEGORY_MESSAGES = "messages";

    /** Display order in the config screen. Without this the GUI sorts alphabetically. */
    private static final String[] CATEGORIES = {
            CATEGORY_HEARTS, CATEGORY_ITEMS, CATEGORY_EXEMPTIONS, CATEGORY_MESSAGES,
    };

    private static final List<String> ORDER_HEARTS = Arrays.asList(
            "HEARTS_LOST_PER_PENALTY", "DEATHS_PER_PENALTY", "MIN_HEARTS",
            "RESET_COUNTER_ON_PENALTY", "RESET_COUNTER_ON_SLEEP");

    private static final List<String> ORDER_ITEMS = Arrays.asList(
            "ENABLE_ITEM_KEEPING", "KEEP_ARMOR", "KEEP_HOTBAR", "KEEP_MAINHAND",
            "KEEP_OFFHAND", "KEEP_BAUBLES", "KEEP_WEARABLE_BACKPACK",
            "KEEP_MAIN_INVENTORY", "KEEP_XP",
            "DURABILITY_LOSS_ON_KEPT_ITEMS", "DROP_DESPAWN_MINUTES");

    private static final List<String> ORDER_EXEMPTIONS = Arrays.asList(
            "COUNT_CREATIVE_DEATHS", "EXEMPT_DIMENSIONS", "EXEMPT_DAMAGE_TYPES");

    private static final List<String> ORDER_MESSAGES = Arrays.asList(
            "ANNOUNCE_PENALTY", "ANNOUNCE_PROGRESS", "BROADCAST_PENALTY_TO_SERVER");

    /**
     * Hands Forge its own mutable copy of an order list.
     *
     * <p>{@code ConfigCategory.setPropertyOrder} keeps the list it is given and then
     * appends any property already in the category that the list does not mention:
     *
     * <pre>
     *   this.propertyOrder = propOrder;
     *   for (String s : properties.keySet())
     *       if (!propOrder.contains(s)) propOrder.add(s);
     * </pre>
     *
     * <p>So the list has to be growable — {@code Arrays.asList} is fixed-size and throws
     * {@code UnsupportedOperationException} the moment a stale key exists in someone's
     * config. It also has to be a copy, or Forge would append into the shared constants
     * below and corrupt the key-to-category map they feed.
     */
    private static List<String> mutableOrder(List<String> order) {
        return new ArrayList<String>(order);
    }

    /** Which category each setting belongs to, used to move settings out of the old block. */
    private static final Map<String, String> CATEGORY_OF_KEY = new HashMap<String, String>();

    static {
        for (String key : ORDER_HEARTS) CATEGORY_OF_KEY.put(key, CATEGORY_HEARTS);
        for (String key : ORDER_ITEMS) CATEGORY_OF_KEY.put(key, CATEGORY_ITEMS);
        for (String key : ORDER_EXEMPTIONS) CATEGORY_OF_KEY.put(key, CATEGORY_EXEMPTIONS);
        for (String key : ORDER_MESSAGES) CATEGORY_OF_KEY.put(key, CATEGORY_MESSAGES);
    }

    /** The single flat category everything used to live in before this split. */
    private static final String LEGACY_CATEGORY = Configuration.CATEGORY_GENERAL;

    public static Configuration config;

    // hearts
    public static float HEARTS_LOST_PER_PENALTY = 1.0F;
    public static int DEATHS_PER_PENALTY = 1;
    public static float MIN_HEARTS = 10.0F;
    public static boolean RESET_COUNTER_ON_PENALTY = true;
    public static boolean RESET_COUNTER_ON_SLEEP = false;

    // items
    public static boolean ENABLE_ITEM_KEEPING = true;
    public static boolean KEEP_ARMOR = true;
    public static boolean KEEP_HOTBAR = true;
    public static boolean KEEP_MAINHAND = true;
    public static boolean KEEP_OFFHAND = true;
    public static boolean KEEP_MAIN_INVENTORY = false;
    public static boolean KEEP_BAUBLES = true;
    public static boolean KEEP_WEARABLE_BACKPACK = true;
    public static boolean KEEP_XP = false;
    public static float DURABILITY_LOSS_ON_KEPT_ITEMS = 0.10F;
    public static int DROP_DESPAWN_MINUTES = 15;

    /** Longest despawn time the config accepts, in minutes. A week. */
    private static final int MAX_DESPAWN_MINUTES = 10080;

    // exemptions
    public static boolean COUNT_CREATIVE_DEATHS = false;
    public static int[] EXEMPT_DIMENSIONS = new int[0];
    public static String[] EXEMPT_DAMAGE_TYPES = new String[0];

    // messages
    public static boolean ANNOUNCE_PENALTY = true;
    public static boolean ANNOUNCE_PROGRESS = true;
    public static boolean BROADCAST_PENALTY_TO_SERVER = false;

    /** Lower-cased {@link #EXEMPT_DAMAGE_TYPES}, rebuilt on every load so lookups stay cheap. */
    private static Set<String> exemptDamageTypes = new HashSet<String>();

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    public static void loadConfig() {
        migrateLegacyCategory();
        migrateDropDespawn();
        // Must run before the loaders: they call setCategoryPropertyOrder, which appends
        // any key the order list does not mention, and a stale key reaching that point is
        // what crashed 1.1.0 on startup.
        pruneUnknownKeys();

        loadHearts();
        loadItems();
        loadExemptions();
        loadMessages();

        clampAndDerive();

        if (config.hasChanged()) {
            config.save();
        }

        RLCraftDeathOverhaul.LOGGER.info(
                "Config loaded - {} heart(s) every {} death(s), floor {} hearts, item keeping {}",
                HEARTS_LOST_PER_PENALTY, DEATHS_PER_PENALTY, MIN_HEARTS,
                ENABLE_ITEM_KEEPING ? "on" : "off");
    }

    /**
     * Moves settings out of the flat {@code general} block they used to share, into the
     * category each one now belongs to, then deletes the empty block.
     *
     * <p>Two things make this necessary. Forge never removes properties it has stopped
     * reading, so the old block would otherwise sit in the file forever — including
     * {@code KEEP_INVENTORY}, renamed to {@code ENABLE_ITEM_KEEPING}, which would look
     * like a working option while doing nothing at all. And a plain delete would throw
     * away whatever the player had customised, which is a worse outcome than the
     * clutter.
     *
     * <p>Properties are carried over rather than re-read, so values survive while the
     * comments and defaults are still rewritten from the code below. Keys that are no
     * longer recognised are dropped, which is exactly what should happen to the renamed
     * one.
     */
    private static void migrateLegacyCategory() {
        if (!config.hasCategory(LEGACY_CATEGORY)) {
            return;
        }
        ConfigCategory legacy = config.getCategory(LEGACY_CATEGORY);
        int moved = 0;
        int dropped = 0;

        for (Map.Entry<String, Property> entry : legacy.entrySet()) {
            String key = entry.getKey();
            String category = CATEGORY_OF_KEY.get(key);
            if (category == null) {
                dropped++;
                continue;
            }
            ConfigCategory target = config.getCategory(category);
            if (!target.containsKey(key)) {
                target.put(key, entry.getValue());
                moved++;
            }
        }

        config.removeCategory(legacy);
        RLCraftDeathOverhaul.LOGGER.info(
                "Split the old flat '{}' config block into hearts / items / exemptions / messages "
                        + "- {} setting(s) kept their value, {} obsolete one(s) removed.",
                LEGACY_CATEGORY, moved, dropped);
    }

    /**
     * Carries the old {@code NO_DROP_DESPAWN} boolean onto the timer that replaced it.
     *
     * <p>Only the {@code false} case needs handling. Someone who turned it off was asking
     * this mod not to touch their drops, and the timer spells that as {@code 0}; leaving
     * them to pick up the new 15 minute default would quietly start changing something
     * they had opted out of. {@code true} was the default and simply becomes the new
     * default, which is the behaviour change this release is for.
     */
    private static void migrateDropDespawn() {
        if (!config.hasCategory(CATEGORY_ITEMS)) {
            return;
        }
        ConfigCategory items = config.getCategory(CATEGORY_ITEMS);
        if (!items.containsKey("NO_DROP_DESPAWN") || items.containsKey("DROP_DESPAWN_MINUTES")) {
            return;
        }
        if (!items.get("NO_DROP_DESPAWN").getBoolean(true)) {
            items.put("DROP_DESPAWN_MINUTES",
                    new Property("DROP_DESPAWN_MINUTES", "0", Property.Type.INTEGER));
            RLCraftDeathOverhaul.LOGGER.info(
                    "NO_DROP_DESPAWN was off, so DROP_DESPAWN_MINUTES has been set to 0 - this mod "
                            + "will keep leaving your death drops alone.");
        }
        // The obsolete key itself is cleared by pruneUnknownKeys once loading is done.
    }

    /**
     * Drops any property sitting in one of our categories that the mod no longer reads.
     *
     * <p>Forge never removes a property once it has stopped asking for it, so without
     * this every renamed setting would linger forever, looking editable while doing
     * nothing. The order lists double as the definition of what is still real.
     */
    private static void pruneUnknownKeys() {
        int removed = 0;
        for (String category : CATEGORIES) {
            if (!config.hasCategory(category)) {
                continue;
            }
            ConfigCategory cat = config.getCategory(category);
            for (String key : new ArrayList<String>(cat.keySet())) {
                if (!category.equals(CATEGORY_OF_KEY.get(key))) {
                    cat.remove(key);
                    removed++;
                }
            }
        }
        if (removed > 0) {
            RLCraftDeathOverhaul.LOGGER.info("Removed {} config setting(s) this version no longer "
                    + "uses.", removed);
        }
    }

    // ------------------------------------------------------------------ hearts

    private static void loadHearts() {
        config.setCategoryComment(CATEGORY_HEARTS,
                "What dying costs you in health.\n"
                        + "All values are in whole hearts: 1.0 is a full heart, 0.5 is a half.");
        config.setCategoryPropertyOrder(CATEGORY_HEARTS, mutableOrder(ORDER_HEARTS));

        HEARTS_LOST_PER_PENALTY = config.getFloat(
                "HEARTS_LOST_PER_PENALTY", CATEGORY_HEARTS, 1.0F, 0.0F, 100.0F,
                "How many hearts to remove each time the penalty is charged.\n"
                        + "1.0 (default) is one full heart. 0.5 removes a single half-heart.\n"
                        + "This is charged once per penalty, NOT once per death - see\n"
                        + "DEATHS_PER_PENALTY.\n"
                        + "Set to 0 to disable health loss entirely while still tracking deaths."
        );

        DEATHS_PER_PENALTY = config.getInt(
                "DEATHS_PER_PENALTY", CATEGORY_HEARTS, 1, 1, 1000,
                "How many deaths it takes to be charged one penalty.\n"
                        + "1 (default) means every death costs you HEARTS_LOST_PER_PENALTY hearts.\n"
                        + "3 means you can die twice for free, and the third death takes the hearts.\n"
                        + "The counter resets after each penalty unless RESET_COUNTER_ON_PENALTY\n"
                        + "is false."
        );

        MIN_HEARTS = config.getFloat(
                "MIN_HEARTS", CATEGORY_HEARTS, 10.0F, 1.0F, 100.0F,
                "The lowest maximum health a player can ever be reduced to by dying, in hearts.\n"
                        + "Deaths at or below this floor still count and are still announced, but\n"
                        + "cost no health - you cannot be penalised into oblivion.\n"
                        + "\n"
                        + "The default of 10 deliberately matches Scaling Health's \"Starting\n"
                        + "Health\" of 20 half-hearts, and that is the whole design: the hearts you\n"
                        + "were born with are untouchable, so dying while you are learning the pack\n"
                        + "costs you nothing. Only hearts you chose to add with heart containers can\n"
                        + "be taken away, which turns every heart container into a decision.\n"
                        + "If you change Scaling Health's Starting Health, change this to match.\n"
                        + "\n"
                        + "Cannot go below 1.0: Scaling Health itself refuses to set a max health\n"
                        + "under one heart, so a lower value here would silently have no effect.\n"
                        + "This is separate from Scaling Health's own \"Min Health\" setting, which\n"
                        + "this mod ignores - set the floor here."
        );

        RESET_COUNTER_ON_PENALTY = config.getBoolean(
                "RESET_COUNTER_ON_PENALTY", CATEGORY_HEARTS, true,
                "If true (default), the death counter resets to zero after a penalty is\n"
                        + "charged, so the grace period from DEATHS_PER_PENALTY applies again from\n"
                        + "scratch.\n"
                        + "If false, the counter keeps climbing and every death past the threshold\n"
                        + "is charged - the grace period becomes one-time-only per world."
        );

        RESET_COUNTER_ON_SLEEP = config.getBoolean(
                "RESET_COUNTER_ON_SLEEP", CATEGORY_HEARTS, false,
                "If true, sleeping through a full night clears any accumulated deaths that\n"
                        + "have not yet been charged as a penalty.\n"
                        + "This does NOT give hearts back - hearts already lost stay lost, and are\n"
                        + "only recovered with Scaling Health heart containers.\n"
                        + "Pointless when DEATHS_PER_PENALTY is 1, since the counter never sits\n"
                        + "above zero."
        );
    }

    // ------------------------------------------------------------------- items

    private static void loadItems() {
        config.setCategoryComment(CATEGORY_ITEMS,
                "What survives a death, and what keeping it costs.\n"
                        + "\n"
                        + "By default you keep your equipped gear and drop your main inventory. That\n"
                        + "split is the point of the mod: dropping your loot gives you a reason to go\n"
                        + "back for it, and keeping your gear is what makes going back survivable.\n"
                        + "\n"
                        + "The vanilla keepInventory gamerule always takes precedence over everything\n"
                        + "in this category. With it on, this mod does not touch your inventory.");
        config.setCategoryPropertyOrder(CATEGORY_ITEMS, mutableOrder(ORDER_ITEMS));

        ENABLE_ITEM_KEEPING = config.getBoolean(
                "ENABLE_ITEM_KEEPING", CATEGORY_ITEMS, true,
                "Master on/off switch for this mod's item handling. ON by default, so the mod\n"
                        + "works as intended straight out of the box with no setup.\n"
                        + "\n"
                        + "This does NOT mean \"keep everything\". It only turns the feature on; what\n"
                        + "actually survives is decided by the KEEP_* settings below. By default that\n"
                        + "is your equipped gear - armour, hotbar, both hands, Baubles, backpack -\n"
                        + "while your main inventory still drops. See KEEP_MAIN_INVENTORY.\n"
                        + "\n"
                        + "Set to false to leave your inventory completely untouched and hand death\n"
                        + "drops back to your pack - Corpse Complex, a gravestone mod, or nothing.\n"
                        + "The heart cost still applies either way.\n"
                        + "\n"
                        + "IMPORTANT: if another mod is also set to keep items, turn one of them off.\n"
                        + "Two mods saving the same inventory can duplicate or lose items."
        );

        KEEP_ARMOR = config.getBoolean(
                "KEEP_ARMOR", CATEGORY_ITEMS, true,
                "Keep equipped armour on death."
        );

        KEEP_HOTBAR = config.getBoolean(
                "KEEP_HOTBAR", CATEGORY_ITEMS, true,
                "Keep hotbar items on death, not counting whatever you were holding -\n"
                        + "that one is KEEP_MAINHAND."
        );

        KEEP_MAINHAND = config.getBoolean(
                "KEEP_MAINHAND", CATEGORY_ITEMS, true,
                "Keep the item you were holding when you died."
        );

        KEEP_OFFHAND = config.getBoolean(
                "KEEP_OFFHAND", CATEGORY_ITEMS, true,
                "Keep the offhand item on death."
        );

        KEEP_BAUBLES = config.getBoolean(
                "KEEP_BAUBLES", CATEGORY_ITEMS, true,
                "Keep equipped Baubles - rings, amulets, belts and the rest - on death.\n"
                        + "Ignored when Baubles is not installed.\n"
                        + "This also covers the Tool Belt, which sits in a Baubles slot whenever\n"
                        + "Baubles is present, and anything else that equips through Baubles."
        );

        KEEP_WEARABLE_BACKPACK = config.getBoolean(
                "KEEP_WEARABLE_BACKPACK", CATEGORY_ITEMS, true,
                "Keep your equipped Wearable Backpack, and everything inside it, on death.\n"
                        + "Ignored when Wearable Backpacks is not installed.\n"
                        + "When that mod is configured to wear backpacks in the chest armour slot,\n"
                        + "KEEP_ARMOR covers it instead and this setting does nothing."
        );

        KEEP_MAIN_INVENTORY = config.getBoolean(
                "KEEP_MAIN_INVENTORY", CATEGORY_ITEMS, false,
                "Keep the main inventory - the 27 slots that are not the hotbar - on death.\n"
                        + "\n"
                        + "OFF by default, and this is the setting that makes the mod work. Dropping\n"
                        + "your loot and materials is what gives you a reason to go back for your\n"
                        + "death pile; your gear surviving is what makes going back possible. Turn\n"
                        + "this on and you keep everything, nothing is left on the ground, and there\n"
                        + "is no trip to make.\n"
                        + "\n"
                        + "Whatever does drop never despawns - see NO_DROP_DESPAWN - so the trip is\n"
                        + "on your schedule."
        );

        KEEP_XP = config.getBoolean(
                "KEEP_XP", CATEGORY_ITEMS, false,
                "Keep your experience on death instead of dropping it.\n"
                        + "OFF by default: losing levels is a normal part of dying, and RLCraft's own\n"
                        + "Corpse Complex already lets you recover some of it."
        );

        DURABILITY_LOSS_ON_KEPT_ITEMS = config.getFloat(
                "DURABILITY_LOSS_ON_KEPT_ITEMS", CATEGORY_ITEMS, 0.10F, 0.0F, 1.0F,
                "Fraction of maximum durability knocked off every damageable item you keep.\n"
                        + "0.10 (default) is 10%. 0 disables it.\n"
                        + "This is what stops keeping your gear from being free - a death costs you\n"
                        + "hearts in the long run and repair materials right now.\n"
                        + "Items are never destroyed by this: anything that would break is left at\n"
                        + "one point of durability instead."
        );

        DROP_DESPAWN_MINUTES = config.getInt(
                "DROP_DESPAWN_MINUTES", CATEGORY_ITEMS, 15, -1, MAX_DESPAWN_MINUTES,
                "How long items dropped on death survive before despawning, in minutes.\n"
                        + "\n"
                        + "  15  default. Vanilla gives you five, which in a pack this large is\n"
                        + "      rarely long enough to fight your way back to where you died.\n"
                        + "   0  leave drops alone entirely - vanilla timing, or whatever another\n"
                        + "      mod has already decided. Use this if something else manages drops.\n"
                        + "  -1  never despawn. Your death pile waits forever.\n"
                        + "      Be careful with this one on a busy server: piles nobody collects\n"
                        + "      accumulate as loaded entities and will cost you performance.\n"
                        + "\n"
                        + "Applies to everything you dropped whether or not ENABLE_ITEM_KEEPING is\n"
                        + "on, since it is about the pile you lost rather than the gear you kept.\n"
                        + "Items destroyed by lava or cactus are still gone - this only changes the\n"
                        + "despawn timer, it does not make drops indestructible."
        );
    }

    // -------------------------------------------------------------- exemptions

    private static void loadExemptions() {
        config.setCategoryComment(CATEGORY_EXEMPTIONS,
                "Deaths that do not count at all.\n"
                        + "An exempt death never increments the counter and never costs hearts.");
        config.setCategoryPropertyOrder(CATEGORY_EXEMPTIONS, mutableOrder(ORDER_EXEMPTIONS));

        COUNT_CREATIVE_DEATHS = config.getBoolean(
                "COUNT_CREATIVE_DEATHS", CATEGORY_EXEMPTIONS, false,
                "If false (default), deaths in creative or spectator mode are ignored\n"
                        + "completely - they do not increment the counter and never cost hearts.\n"
                        + "Set to true to penalise them anyway."
        );

        EXEMPT_DIMENSIONS = config.get(
                CATEGORY_EXEMPTIONS, "EXEMPT_DIMENSIONS", new int[0],
                "Dimension IDs in which dying costs nothing - list one per line.\n"
                        + "Vanilla IDs are 0 (Overworld), -1 (Nether), 1 (The End).\n"
                        + "Empty (default) exempts nothing.\n"
                        + "Note that returning from The End through the exit portal is never treated\n"
                        + "as a death regardless of this setting."
        ).getIntList();

        EXEMPT_DAMAGE_TYPES = config.getStringList(
                "EXEMPT_DAMAGE_TYPES", CATEGORY_EXEMPTIONS, new String[0],
                "Damage types that do not count as a death - one per line, case-insensitive.\n"
                        + "Matched against Minecraft's internal damage type name, which is NOT the\n"
                        + "death message you see in chat. Common vanilla ones: fall, lava, inFire,\n"
                        + "onFire, drown, cactus, starve, fallingBlock, outOfWorld, mob, player,\n"
                        + "arrow, explosion, magic.\n"
                        + "Modded sources use their own names - if you are unsure of one, set the log\n"
                        + "to debug and this mod prints the damage type of every death it sees.\n"
                        + "Empty (default) exempts nothing."
        );
    }

    // ---------------------------------------------------------------- messages

    private static void loadMessages() {
        config.setCategoryComment(CATEGORY_MESSAGES,
                "What the mod tells players in chat.");
        config.setCategoryPropertyOrder(CATEGORY_MESSAGES, mutableOrder(ORDER_MESSAGES));

        ANNOUNCE_PENALTY = config.getBoolean(
                "ANNOUNCE_PENALTY", CATEGORY_MESSAGES, true,
                "If true (default), tell the player in chat when they lose hearts on respawn,\n"
                        + "and when they are at the MIN_HEARTS floor and therefore lost nothing."
        );

        ANNOUNCE_PROGRESS = config.getBoolean(
                "ANNOUNCE_PROGRESS", CATEGORY_MESSAGES, true,
                "If true (default), tell the player how many deaths remain before the next\n"
                        + "penalty when they respawn without being charged.\n"
                        + "Has no visible effect when DEATHS_PER_PENALTY is 1, since there is never\n"
                        + "a death that does not charge."
        );

        BROADCAST_PENALTY_TO_SERVER = config.getBoolean(
                "BROADCAST_PENALTY_TO_SERVER", CATEGORY_MESSAGES, false,
                "If true, announce every penalty to everyone on the server rather than only to\n"
                        + "the player who paid it. Off by default; mainly for multiplayer servers\n"
                        + "that want deaths to be public."
        );
    }

    // ------------------------------------------------------------------

    /**
     * Forge's own range checking covers the config GUI, but a hand-edited .cfg can still
     * contain anything at all, so the values are clamped again here.
     */
    private static void clampAndDerive() {
        if (HEARTS_LOST_PER_PENALTY < 0.0F) HEARTS_LOST_PER_PENALTY = 0.0F;
        if (DEATHS_PER_PENALTY < 1) DEATHS_PER_PENALTY = 1;
        if (MIN_HEARTS < 1.0F) MIN_HEARTS = 1.0F;
        if (DURABILITY_LOSS_ON_KEPT_ITEMS < 0.0F) DURABILITY_LOSS_ON_KEPT_ITEMS = 0.0F;
        if (DURABILITY_LOSS_ON_KEPT_ITEMS > 1.0F) DURABILITY_LOSS_ON_KEPT_ITEMS = 1.0F;

        Set<String> exempt = new HashSet<String>();
        for (String type : EXEMPT_DAMAGE_TYPES) {
            if (type != null && !type.trim().isEmpty()) {
                exempt.add(type.trim().toLowerCase(Locale.ROOT));
            }
        }
        exemptDamageTypes = exempt;
    }

    /**
     * One entry per category, so the config screen opens on four labelled groups rather
     * than a single list of twenty-odd options.
     */
    public static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        for (String category : CATEGORIES) {
            elements.add(new ConfigElement(config.getCategory(category)));
        }
        return elements;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RLCraftDeathOverhaul.MODID)) {
            // Every setting is read live at death/respawn time, so a reload is enough —
            // nothing here needs a restart to take effect.
            loadConfig();
        }
    }

    public static boolean isDimensionExempt(int dimensionId) {
        for (int exempt : EXEMPT_DIMENSIONS) {
            if (exempt == dimensionId) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDamageTypeExempt(String damageType) {
        return damageType != null
                && exemptDamageTypes.contains(damageType.toLowerCase(Locale.ROOT));
    }

    // --- Half-heart (raw HP) conversions, for everything below ScalingHealthBridge ---

    public static float getHeartsLostHp() {
        return HEARTS_LOST_PER_PENALTY * 2.0F;
    }

    public static float getMinHealthHp() {
        return Math.max(MIN_HEARTS * 2.0F, ScalingHealthBridge.ABSOLUTE_MIN_HP);
    }
}
