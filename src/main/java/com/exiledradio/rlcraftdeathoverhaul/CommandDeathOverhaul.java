package com.exiledradio.rlcraftdeathoverhaul;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code /deathoverhaul} — inspect and administer the death ledger.
 *
 * <p>{@code status} is available to everyone so players can check their own standing;
 * the subcommands that change state require permission level 2, the same level vanilla
 * requires for {@code /gamemode}.
 */
public class CommandDeathOverhaul extends CommandBase {

    private static final int ADMIN_PERMISSION_LEVEL = 2;

    @Override
    public String getName() {
        return "deathoverhaul";
    }

    @Override
    public List<String> getAliases() {
        // "dp" carried over from when this mod was called Death Penalty.
        return Arrays.asList("dov", "dp");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/deathoverhaul <status|reset|sethearts> [player] [hearts]";
    }

    /**
     * Zero so that unprivileged players can reach {@code status}. The subcommands that
     * mutate anything check for level 2 themselves in {@link #checkAdmin}.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }

        String subCommand = args[0].toLowerCase();
        if ("status".equals(subCommand)) {
            executeStatus(server, sender, args);
        } else if ("reset".equals(subCommand)) {
            executeReset(server, sender, args);
        } else if ("sethearts".equals(subCommand)) {
            executeSetHearts(server, sender, args);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void executeStatus(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        EntityPlayer target;
        if (args.length >= 2) {
            // Reading someone else's record is an admin action; reading your own is not.
            checkAdmin(sender);
            target = getPlayer(server, sender, args[1]);
        } else {
            target = getCommandSenderAsPlayer(sender);
        }

        float maxHp = ScalingHealthBridge.getMaxHealth(target);
        int deaths = DeathPenaltyData.getDeathsSincePenalty(target);
        int remaining = Math.max(0, ModConfig.DEATHS_PER_PENALTY - deaths);

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD
                + "--- Death Overhaul: " + target.getName() + " ---"));
        sender.sendMessage(line("Max health",
                maxHp < 0.0F ? "unknown (no Scaling Health data)"
                        : DeathPenaltyHandler.formatHearts(maxHp) + " hearts"));
        sender.sendMessage(line("Floor", ModConfig.MIN_HEARTS + " hearts"));
        sender.sendMessage(line("Deaths toward next penalty",
                deaths + " / " + ModConfig.DEATHS_PER_PENALTY
                        + (remaining > 0 ? " (" + remaining + " to go)" : " (next death charges)")));
        sender.sendMessage(line("Cost per penalty",
                ModConfig.HEARTS_LOST_PER_PENALTY + " hearts"));
        sender.sendMessage(line("Lifetime deaths",
                String.valueOf(DeathPenaltyData.getTotalDeaths(target))));
        sender.sendMessage(line("Lifetime hearts lost",
                String.valueOf(DeathPenaltyData.getTotalHeartsLost(target))));
    }

    private void executeReset(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        checkAdmin(sender);
        if (args.length < 2) {
            throw new WrongUsageException("/deathoverhaul reset <player>");
        }
        EntityPlayer target = getPlayer(server, sender, args[1]);
        DeathPenaltyData.reset(target);
        notifyCommandListener(sender, this, "Cleared the death ledger for %s. "
                + "This resets counters only - it does not give hearts back "
                + "(use /deathoverhaul sethearts for that).", target.getName());
    }

    private void executeSetHearts(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        checkAdmin(sender);
        if (args.length < 3) {
            throw new WrongUsageException("/deathoverhaul sethearts <player> <hearts>");
        }
        EntityPlayer target = getPlayer(server, sender, args[1]);
        // Scaling Health refuses anything under one heart and clamps to its own cap, so
        // the value that lands may be lower than what was asked for. Report what stuck.
        double hearts = parseDouble(args[2], 1.0D);

        if (!ScalingHealthBridge.setMaxHealth(target, (float) (hearts * 2.0D))) {
            throw new CommandException("Scaling Health rejected the change. Check that "
                    + "\"Allow Modified Health\" is true in config/scalinghealth/main.cfg.");
        }
        target.setHealth(target.getMaxHealth());

        float actualHp = ScalingHealthBridge.getMaxHealth(target);
        notifyCommandListener(sender, this, "Set %s's max health to %s hearts.",
                target.getName(), DeathPenaltyHandler.formatHearts(actualHp));
    }

    private void checkAdmin(ICommandSender sender) throws CommandException {
        if (!sender.canUseCommand(ADMIN_PERMISSION_LEVEL, getName())) {
            throw new CommandException("commands.generic.permission");
        }
    }

    private static TextComponentString line(String label, String value) {
        return new TextComponentString(TextFormatting.GRAY + label + ": "
                + TextFormatting.WHITE + value);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "reset", "sethearts");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return new ArrayList<String>();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}
