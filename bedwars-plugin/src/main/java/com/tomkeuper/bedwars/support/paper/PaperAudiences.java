package com.tomkeuper.bedwars.support.paper;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Audiences straight from Paper, whose command senders are audiences themselves. adventure-platform-bukkit drops every
 * message on Paper 26, which ships Adventure 5.
 */
public final class PaperAudiences implements BukkitAudiences {

    /**
     * Paper's own audiences where the server has them, else adventure-platform-bukkit. Behind a method so the plugin
     * class never names this one, whose interface comes from a library loaded only at startup.
     */
    public static BukkitAudiences create(Plugin plugin) {
        return Audience.class.isAssignableFrom(CommandSender.class) ? new PaperAudiences() : BukkitAudiences.create(plugin);
    }

    private static Audience of(Object sender) {
        return sender == null ? Audience.empty() : (Audience) sender;
    }

    private static Audience of(List<? extends CommandSender> senders) {
        var audiences = new ArrayList<Audience>(senders.size());
        for (var sender : senders) audiences.add(of(sender));
        return Audience.audience(audiences);
    }

    @Override
    public @NotNull Audience sender(@NotNull CommandSender sender) {
        return of(sender);
    }

    @Override
    public @NotNull Audience player(@NotNull Player player) {
        return of(player);
    }

    @Override
    public @NotNull Audience filter(@NotNull Predicate<CommandSender> filter) {
        var senders = new ArrayList<CommandSender>();
        for (var player : Bukkit.getOnlinePlayers()) {
            if (filter.test(player)) senders.add(player);
        }
        if (filter.test(Bukkit.getConsoleSender())) senders.add(Bukkit.getConsoleSender());
        return of(senders);
    }

    @Override
    public @NotNull Audience all() {
        return of(Bukkit.getServer());
    }

    @Override
    public @NotNull Audience console() {
        return of(Bukkit.getConsoleSender());
    }

    @Override
    public @NotNull Audience players() {
        return of(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public @NotNull Audience player(@NotNull UUID playerId) {
        return of(Bukkit.getPlayer(playerId));
    }

    @Override
    public @NotNull Audience permission(@NotNull Key permission) {
        return permission(permission.namespace() + "." + permission.value());
    }

    @Override
    public @NotNull Audience permission(@NotNull String permission) {
        return filter(sender -> sender.hasPermission(permission));
    }

    @Override
    public @NotNull Audience world(@NotNull Key world) {
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getName().equals(world.value())) return of(new ArrayList<>(candidate.getPlayers()));
        }
        return Audience.empty();
    }

    @Override
    public @NotNull Audience server(@NotNull String serverName) {
        return all();
    }

    @Override
    public @NotNull ComponentFlattener flattener() {
        return ComponentFlattener.basic();
    }

    @Override
    public void close() {
    }
}
