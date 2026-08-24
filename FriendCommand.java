package com.friends.plugin.command;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.gui.MainFriendsGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final FriendsPlugin plugin;

    public FriendCommand(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("friends.use")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            new MainFriendsGui(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /friend add <player>", NamedTextColor.RED));
                    return true;
                }
                plugin.getFriendManager().sendFriendRequest(player, args[1]);
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /friend remove <player>", NamedTextColor.RED));
                    return true;
                }
                plugin.getFriendManager().removeFriend(player, args[1]);
            }
            case "accept" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /friend accept <player>", NamedTextColor.RED));
                    return true;
                }
                plugin.getFriendManager().acceptFriendRequest(player, args[1]);
            }
            case "decline", "deny" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /friend decline <player>", NamedTextColor.RED));
                    return true;
                }
                plugin.getFriendManager().declineFriendRequest(player, args[1]);
            }
            case "list" -> new MainFriendsGui(plugin, player).open();
            default -> new MainFriendsGui(plugin, player).open();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("add", "remove", "accept", "decline", "list"));
            String partial = args[0].toLowerCase();
            return options.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")
                || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline"))) {
            String partial = args[1].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return options;
    }
}
