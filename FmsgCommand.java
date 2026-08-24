package com.friends.plugin.command;

import com.friends.plugin.FriendsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class FmsgCommand implements CommandExecutor, TabCompleter {

    private final FriendsPlugin plugin;

    public FmsgCommand(FriendsPlugin plugin) {
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

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /fmsg <player> <message>", NamedTextColor.RED));
            return true;
        }

        String targetName = args[0];
        String message = String.join(" ", List.of(args).subList(1, args.length));

        plugin.getMessageManager().sendMessage(player, targetName, message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            String partial = args[0].toLowerCase();
            try {
                return plugin.getFriendsDAO().getFriends(player.getUniqueId()).stream()
                        .map(f -> f.getFriendName())
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            } catch (SQLException e) {
                return List.of();
            }
        }
        return List.of();
    }
}
