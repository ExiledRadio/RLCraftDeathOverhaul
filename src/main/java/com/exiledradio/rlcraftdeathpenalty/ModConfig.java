package com.exiledradio.rlcraftdeathpenalty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * All settings are expressed in <em>whole hearts</em>, because that is the unit players
 * think in. Scaling Health's own config is in half-hearts, so anything crossing into
 * {@link ScalingHealthBridge} goes through the {@code ...Hp()} accessors at the bottom.
 */
@Mod.EventBusSubscriber(modid = RLCraftDeathPenalty.MODID)
public class ModConfig {

    public static Configuration config;

    public static float HEARTS_LOST_PER_PENALTY = 1.0F;
    public static int DEATHS_PER_PENALTY = 1;
    public static float MIN_HEARTS = 10.0F;
    public static boolean RESET_COUNTER_ON_PENALTY = true;
    public static boolean RESET_COUNTER_ON_SLEEP = false;
    public static boolean COUNT_CREATIVE_DEATHS = false;
    public static boolean ANNOUNCE_PENALTY = true;
    public static boolean ANNOUNCE_PROGRESS = true;
    public static boolean BROADCAST_PENALTY_TO_SERVER = false;
    public static int[] EXEMPT_DIMENSIONS = new int[0];
    public static String[] EXEMPT_DAMAGE_TYPES = new String[0];

    public static boolean KEEP_INVENTORY = false;
    public static boolean KEEP_ARMOR = true;
    public static boolean KEEP_HOTBAR = true;
    public static boolean KEEP_MAINHAND = true;
    public static boolean KEEP_OFFHAND = true;
    public static boolean KEEP_MAIN_INVENTORY = false;
    public static boolean KEEP_XP = false;

    /** Lower-cased {@link #EXEMPT_DAMAGE_TYPES}, rebuilt on every load so lookups stay cheap. */
    private static Set<String> exemptDamageTypes = new HashSet<String>();

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    public static void loadConfig() {
        HEARTS_LOST_PER_PENALTY = config.getFloat(
                "HEARTS_LOST_PER_PENALTY",
                Configuration.CATEGORY_GENERAL,
                1.0F,
                0.0F,
                100.0F,
                "How many hearts to remove each time the death penalty is charged.\n"
                        + "1.0 (default) is one full heart. 0.5 removes a single half-heart.\n"
                        + "This is charged once per penalty, NOT once per death - see DEATHS_PER_PENALTY.\n"
                        + "Set to 0 to disable health loss entirely while still tracking deaths."
        );

        DEATHS_PER_PENALTY = config.getInt(
                "DEATHS_PER_PENALTY",
                Configuration.CATEGORY_GENERAL,
                1,
                1,
                1000,
                "How many deaths it takes to be charged one penalty.\n"
                        + "1 (default) means every death costs you HEARTS_LOST_PER_PENALTY hearts.\n"
                        + "3 means you can die twice for free, and the third death takes the hearts.\n"
                        + "The counter resets after each penalty unless RESET_COUNTER_ON_PENALTY is false."
        );

        MIN_HEARTS = config.getFloat(
                "MIN_HEARTS",
                Configuration.CATEGORY_GENERAL,
                10.0F,
                1.0F,
                100.0F,
                "The lowest maximum health a player can ever be reduced to by dying, in hearts.\n"
                        + "Deaths at or below this floor still count and are still announced, but cost\n"
                        + "no health - you cannot be penalised into oblivion.\n"
                        + "\n"
                        + "The default of 10 is deliberately set to match Scaling Health's \"Starting\n"
                        + "Health\" of 20 half-hearts, and that is the whole design: the hearts you were\n"
                        + "born with are untouchable, so dying while you are learning the pack costs you\n"
                        + "nothing. Only hearts you chose to add with heart containers can be taken away.\n"
                        + "That turns every heart container into a decision - spend it now and put it at\n"
                        + "risk, or hold it until you trust yourself in the fight ahead.\n"
                        + "If you change Scaling Health's Starting Health, change this to match.\n"
                        + "\n"
                        + "Cannot go below 1.0: Scaling Health itself refuses to set a max health under\n"
                        + "one heart, so a lower value here would silently have no effect.\n"
                        + "This is separate from Scaling Health's own \"Min Health\" setting, which this\n"
                        + "mod ignores - set the floor here."
        );

        RESET_COUNTER_ON_PENALTY = config.getBoolean(
                "RESET_COUNTER_ON_PENALTY",
                Configuration.CATEGORY_GENERAL,
                true,
                "If true (default), the death counter resets to zero after a penalty is charged, so\n"
                        + "the grace period from DEATHS_PER_PENALTY applies again from scratch.\n"
                        + "If false, the counter keeps climbing and every death past the threshold is\n"
                        + "charged - i.e. the grace period is one-time-only per world, not repeating."
        );

        RESET_COUNTER_ON_SLEEP = config.getBoolean(
                "RESET_COUNTER_ON_SLEEP",
                Configuration.CATEGORY_GENERAL,
                false,
                "If true, successfully sleeping through the night clears any accumulated deaths that\n"
                        + "have not yet been charged as a penalty.\n"
                        + "This does NOT give hearts back - hearts already lost stay lost, and are only\n"
                        + "recovered with Scaling Health heart containers.\n"
                        + "Pointless when DEATHS_PER_PENALTY is 1, since the counter never sits above zero."
        );

        COUNT_CREATIVE_DEATHS = config.getBoolean(
                "COUNT_CREATIVE_DEATHS",
                Configuration.CATEGORY_GENERAL,
                false,
                "If false (default), deaths in creative or spectator mode are ignored completely -\n"
                        + "they do not increment the counter and never cost hearts.\n"
                        + "Set to true to penalise them anyway."
        );

        ANNOUNCE_PENALTY = config.getBoolean(
                "ANNOUNCE_PENALTY",
                Configuration.CATEGORY_GENERAL,
                true,
                "If true (default), tell the player in chat when they lose hearts on respawn, and\n"
                        + "when they are at the MIN_HEARTS floor and therefore lost nothing."
        );

        ANNOUNCE_PROGRESS = config.getBoolean(
                "ANNOUNCE_PROGRESS",
                Configuration.CATEGORY_GENERAL,
                true,
                "If true (default), tell the player how many deaths remain before the next penalty\n"
                        + "when they respawn without being charged.\n"
                        + "Has no visible effect when DEATHS_PER_PENALTY is 1, since there is never a\n"
                        + "death that does not charge."
        );

        BROADCAST_PENALTY_TO_SERVER = config.getBoolean(
                "BROADCAST_PENALTY_TO_SERVER",
                Configuration.CATEGORY_GENERAL,
                false,
                "If true, announce every penalty to everyone on the server rather than only to the\n"
                        + "player who paid it. Off by default; mainly for multiplayer servers that want\n"
                        + "deaths to be public."
        );

        EXEMPT_DIMENSIONS = config.get(
                Configuration.CATEGORY_GENERAL,
                "EXEMPT_DIMENSIONS",
                new int[0],
                "Dimension IDs in which dying costs nothing - list one per line.\n"
                        + "Deaths in these dimensions do not increment the counter and never charge a\n"
                        + "penalty. Vanilla IDs are 0 (Overworld), -1 (Nether), 1 (The End).\n"
                        + "Empty (default) exempts nothing.\n"
                        + "Note that returning from The End through the exit portal is never treated as\n"
                        + "a death regardless of this setting."
        ).getIntList();

        EXEMPT_DAMAGE_TYPES = config.getStringList(
                "EXEMPT_DAMAGE_TYPES",
                Configuration.CATEGORY_GENERAL,
                new String[0],
                "Damage types that do not count as a death - list one per line, case-insensitive.\n"
                        + "Matched against Minecraft's internal damage type name, which is NOT the death\n"
                        + "message you see in chat. Common vanilla ones: fall, lava, inFire, onFire, drown,\n"
                        + "cactus, starve, fallingBlock, outOfWorld, mob, player, arrow, explosion, magic.\n"
                        + "Modded sources use their own names - if you are unsure of one, set the log to\n"
                        + "debug and this mod prints the damage type of every death it sees.\n"
                        + "Empty (default) exempts nothing."
        );

        KEEP_INVENTORY = config.getBoolean(
                "KEEP_INVENTORY",
                Configuration.CATEGORY_GENERAL,
                false,
                "Master switch for this mod keeping items on death. Off by default.\n"
                        + "\n"
                        + "Leave this OFF if you already run Corpse Complex, Corail Tombstone, a\n"
                        + "gravestone mod, or anything else that handles death drops - two mods both\n"
                        + "trying to save your inventory is how items go missing. This exists so the\n"
                        + "mod can stand on its own in a pack that has none of those.\n"
                        + "\n"
                        + "This is NOT the vanilla keepInventory gamerule and is not meant to replace\n"
                        + "it. If that gamerule is on, vanilla already keeps everything and this mod\n"
                        + "leaves your inventory completely alone regardless of what is set here.\n"
                        + "\n"
                        + "The intended setup is to keep your equipped kit and still drop your haul -\n"
                        + "the defaults below do exactly that. The hearts remain the real penalty."
        );

        KEEP_ARMOR = config.getBoolean(
                "KEEP_ARMOR", Configuration.CATEGORY_GENERAL, true,
                "Keep equipped armour on death. Only applies when KEEP_INVENTORY is true."
        );

        KEEP_HOTBAR = config.getBoolean(
                "KEEP_HOTBAR", Configuration.CATEGORY_GENERAL, true,
                "Keep hotbar items on death, not counting whatever you were holding -\n"
                        + "that one is KEEP_MAINHAND. Only applies when KEEP_INVENTORY is true."
        );

        KEEP_MAINHAND = config.getBoolean(
                "KEEP_MAINHAND", Configuration.CATEGORY_GENERAL, true,
                "Keep the item you were holding when you died.\n"
                        + "Only applies when KEEP_INVENTORY is true."
        );

        KEEP_OFFHAND = config.getBoolean(
                "KEEP_OFFHAND", Configuration.CATEGORY_GENERAL, true,
                "Keep the offhand item on death. Only applies when KEEP_INVENTORY is true."
        );

        KEEP_MAIN_INVENTORY = config.getBoolean(
                "KEEP_MAIN_INVENTORY", Configuration.CATEGORY_GENERAL, false,
                "Keep the main inventory - the 27 slots that are not the hotbar - on death.\n"
                        + "Off by default on purpose: dropping your loot and materials is what keeps\n"
                        + "a death costly in the moment, while the hearts are the lasting cost.\n"
                        + "Turning this on together with the other options above is effectively the\n"
                        + "keepInventory gamerule, with the heart penalty layered over it.\n"
                        + "Only applies when KEEP_INVENTORY is true."
        );

        KEEP_XP = config.getBoolean(
                "KEEP_XP", Configuration.CATEGORY_GENERAL, false,
                "Keep your experience on death instead of dropping it.\n"
                        + "Only applies when KEEP_INVENTORY is true."
        );

        // Clamp defensively. Forge's own range checking covers the config GUI, but a
        // hand-edited .cfg can still contain anything at all.
        if (HEARTS_LOST_PER_PENALTY < 0.0F) HEARTS_LOST_PER_PENALTY = 0.0F;
        if (DEATHS_PER_PENALTY < 1) DEATHS_PER_PENALTY = 1;
        if (MIN_HEARTS < 1.0F) MIN_HEARTS = 1.0F;

        Set<String> exempt = new HashSet<String>();
        for (String type : EXEMPT_DAMAGE_TYPES) {
            if (type != null && !type.trim().isEmpty()) {
                exempt.add(type.trim().toLowerCase(Locale.ROOT));
            }
        }
        exemptDamageTypes = exempt;

        if (config.hasChanged()) {
            config.save();
        }

        RLCraftDeathPenalty.LOGGER.info("Config loaded - {} heart(s) lost every {} death(s), floor {} hearts",
                HEARTS_LOST_PER_PENALTY, DEATHS_PER_PENALTY, MIN_HEARTS);
    }

    public static List<IConfigElement> getConfigElements() {
        // Expose the category's individual properties directly, instead of wrapping
        // them in a single "General" category element the user has to click into.
        return new ConfigElement(config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RLCraftDeathPenalty.MODID)) {
            boolean wasKeepingInventory = KEEP_INVENTORY;

            // Every setting is read live at death/respawn time, so a reload is enough —
            // nothing here needs a restart to take effect.
            loadConfig();

            // Only worth saying at the moment it is switched on. The startup check
            // cannot cover this: it runs once, before anyone has touched the GUI.
            if (KEEP_INVENTORY && !wasKeepingInventory) {
                String warning = InventoryKeepHandler.buildConflictWarning();
                if (warning != null) {
                    RLCraftDeathPenalty.LOGGER.warn(warning);
                    notifyConflict(warning);
                }
            }
        }
    }

    // Config screens only ever exist client-side, so OnConfigChangedEvent (fired when one
    // is saved) never fires on a dedicated server - this is safe to call unconditionally
    // from the common event handler above.
    @SideOnly(Side.CLIENT)
    private static void notifyConflict(String warning) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "[Death Penalty] " + TextFormatting.YELLOW + warning));
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

    public static String describeExemptDimensions() {
        return EXEMPT_DIMENSIONS.length == 0 ? "none" : Arrays.toString(EXEMPT_DIMENSIONS);
    }
}
