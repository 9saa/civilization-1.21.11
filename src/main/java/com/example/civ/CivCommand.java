package com.example.civ;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CivCommand {

    public static void register(Commands commands, CivPlugin plugin) {

        LiteralCommandNode<CommandSourceStack> node = Commands.literal("civ")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> create(plugin, ctx,
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("disband")
                        .executes(ctx -> disband(plugin, ctx)))
                .then(Commands.literal("leave")
                        .executes(ctx -> leave(plugin, ctx)))
                .then(Commands.literal("invite")
                        .executes(ctx -> invite(plugin, ctx)))
                .then(Commands.literal("promote")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> promote(plugin, ctx,
                                        ctx.getArgument("player", Player.class)))))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> kick(plugin, ctx,
                                        ctx.getArgument("player", Player.class)))))
                .then(Commands.literal("setbase")
                        .executes(ctx -> setBase(plugin, ctx)))
                .then(Commands.literal("base")
                        .executes(ctx -> goBase(plugin, ctx)))
                .then(Commands.literal("msg")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> msg(plugin, ctx,
                                        StringArgumentType.getString(ctx, "message")))))
                .then(Commands.literal("crown")
                        .executes(ctx -> crown(plugin, ctx)))
                .build();

        commands.register(node, "Civilization management");
    }

    // ---------- /civ create <name> ----------

    private static int create(CivPlugin plugin, CommandContext<CommandSourceStack> ctx, String name) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        if (mgr.getCivOf(player.getUniqueId()) != null) {
            player.sendMessage(Component.text("You are already in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!mgr.create(name, player.getUniqueId())) {
            player.sendMessage(Component.text("A civilization with that name already exists.", NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(Component.text("Civilization '" + name + "' created!", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ disband ----------

    private static int disband(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!civ.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the owner can disband.", NamedTextColor.RED));
            return 0;
        }
        mgr.broadcast(civ, Component.text("The civilization '" + civ.getName() + "' has been disbanded.", NamedTextColor.RED));
        mgr.disband(civ.getName());
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ leave ----------

    private static int leave(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (civ.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("The owner must disband the civilization, not leave.", NamedTextColor.RED));
            return 0;
        }
        mgr.broadcast(civ, Component.text(player.getName() + " has left the civilization.", NamedTextColor.YELLOW));
        mgr.leave(player.getUniqueId());
        player.sendMessage(Component.text("You left the civilization.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ invite ----------

    private static int invite(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        CivRank rank = civ.getRank(player.getUniqueId());
        if (rank == null || !rank.isAtLeast(CivRank.MANAGER)) {
            player.sendMessage(Component.text("You need MANAGER rank or higher to invite.", NamedTextColor.RED));
            return 0;
        }
        InviteGUI.open(plugin, player, civ);
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ promote <player> ----------

    private static int promote(CivPlugin plugin, CommandContext<CommandSourceStack> ctx, Player target) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!civ.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the owner can promote.", NamedTextColor.RED));
            return 0;
        }

        CivRank current = civ.getRank(target.getUniqueId());
        if (current == null) {
            player.sendMessage(Component.text("That player is not in your civilization.", NamedTextColor.RED));
            return 0;
        }
        if (current == CivRank.SECOND_IN_COMMAND) {
            player.sendMessage(Component.text("They are already second in command.", NamedTextColor.RED));
            return 0;
        }

        CivRank next = current.next();
        civ.setRank(target.getUniqueId(), next);
        plugin.getManager().save();

        player.sendMessage(Component.text(target.getName() + " promoted to " + next.name() + ".", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You were promoted to " + next.name() + "!", NamedTextColor.GOLD));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ kick <player> ----------

    private static int kick(CivPlugin plugin, CommandContext<CommandSourceStack> ctx, Player target) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        CivRank myRank = civ.getRank(player.getUniqueId());
        if (myRank == null || !myRank.isAtLeast(CivRank.SECOND_IN_COMMAND)) {
            player.sendMessage(Component.text("Only the owner or 2nd in command can kick.", NamedTextColor.RED));
            return 0;
        }
        if (civ.isOwner(target.getUniqueId())) {
            player.sendMessage(Component.text("You cannot kick the owner.", NamedTextColor.RED));
            return 0;
        }
        CivRank targetRank = civ.getRank(target.getUniqueId());
        if (targetRank == null) {
            player.sendMessage(Component.text("That player is not in your civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!civ.isOwner(player.getUniqueId()) && !myRank.isAtLeast(targetRank)) {
            player.sendMessage(Component.text("You cannot kick someone of equal or higher rank.", NamedTextColor.RED));
            return 0;
        }

        civ.removeMember(target.getUniqueId());
        mgr.save();

        player.sendMessage(Component.text(target.getName() + " has been kicked.", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You were kicked from " + civ.getName() + ".", NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ setbase ----------

    private static int setBase(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!civ.isOwner(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the owner can set the base.", NamedTextColor.RED));
            return 0;
        }

        Location loc = player.getLocation();
        civ.setBase(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        mgr.save();

        player.sendMessage(Component.text("Base set to " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ base ----------

    private static int goBase(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }
        if (!civ.hasBase()) {
            player.sendMessage(Component.text("Your civilization has no base set.", NamedTextColor.RED));
            return 0;
        }

        World world = Bukkit.getWorld(civ.getBaseWorld());
        if (world == null) {
            player.sendMessage(Component.text("Base world not loaded.", NamedTextColor.RED));
            return 0;
        }

        Location dest = new Location(world, civ.getBaseX() + 0.5, civ.getBaseY() + 1.0, civ.getBaseZ() + 0.5);
        player.teleport(dest);
        player.sendMessage(Component.text("Teleported to civ base.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ msg <message> ----------

    private static int msg(CivPlugin plugin, CommandContext<CommandSourceStack> ctx, String message) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }

        CivRank rank = civ.getRank(player.getUniqueId());
        Component formatted = Component.text("[" + civ.getName() + "] ", NamedTextColor.GOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" [" + (rank != null ? rank.name() : "MEMBER") + "]: ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        mgr.broadcast(civ, formatted);
        return Command.SINGLE_SUCCESS;
    }

    // ---------- /civ crown ----------

    private static int crown(CivPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) return 0;

        CivilizationManager mgr = plugin.getManager();
        Civilization civ = mgr.getCivOf(player.getUniqueId());

        if (civ == null) {
            player.sendMessage(Component.text("You are not in a civilization.", NamedTextColor.RED));
            return 0;
        }

        int tier = civ.getCrownTier();
        int members = civ.getMemberCount();

        player.sendMessage(Component.text("=== " + civ.getName() + " Crown ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Members: " + members, NamedTextColor.YELLOW));

        if (tier == 0) {
            player.sendMessage(Component.text("Crown Tier: Locked (need 10 members)", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Need " + (10 - members) + " more to unlock.", NamedTextColor.GRAY));
        } else {
            String tierName = switch (tier) {
                case 3 -> "III";
                case 2 -> "II";
                default -> "I";
            };
            player.sendMessage(Component.text("Crown Tier: " + tierName, NamedTextColor.GOLD));
            String nextReq = tier == 3 ? "MAX" : "Needs " + (tier == 1 ? 20 : 30) + " members";
            player.sendMessage(Component.text("Next Tier: " + nextReq, NamedTextColor.GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    // ---------- Helpers ----------

    private static Player requirePlayer(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getExecutor() instanceof Player player) return player;
        ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return null;
    }
}
