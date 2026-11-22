package id.danierulazy.acediamcintro;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;

public class VersionCompat {

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String t = ChatColor.translateAlternateColorCodes('&', title == null ? "" : title);
        String s = ChatColor.translateAlternateColorCodes('&', subtitle == null ? "" : subtitle);
        try {
            player.sendTitle(t, s, fadeIn, stay, fadeOut);
        } catch (NoSuchMethodError e) {
            try {
                Method m = player.getClass().getMethod("sendTitle", String.class, String.class);
                m.invoke(player, t, s);
            } catch (Exception ignored) {
                player.sendMessage(t + " " + s);
            }
        }
    }

    public static void sendActionBar(Player player, String message) {
        String msg = ChatColor.translateAlternateColorCodes('&', message);
        try {
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg)
            );
        } catch (Throwable t) {
            player.sendMessage(msg);
        }
    }

    public static void playSoundSafe(Player player, Location loc, float volume, float pitch, String... candidates) {
        for (String name : candidates) {
            try {
                Sound sound = Sound.valueOf(name.toUpperCase());
                player.playSound(loc, sound, volume, pitch);
                return;
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static void spawnParticleSafe(World world, Location loc, int count, double ox, double oy, double oz, double speed, String... candidates) {
        for (String name : candidates) {
            try {
                Particle particle = Particle.valueOf(name.toUpperCase());
                world.spawnParticle(particle, loc, count, ox, oy, oz, speed);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    public static ItemStack createPlayerHead(String username) {
        ItemStack skull = null;
        try {
            skull = new ItemStack(Material.valueOf("PLAYER_HEAD"), 1);
        } catch (IllegalArgumentException e) {
            try {
                skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
            } catch (IllegalArgumentException ignored) {
                return new ItemStack(Material.DIAMOND_HELMET);
            }
        }
        if (skull.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(username));
            } catch (Throwable t) {
                try {
                    Method setOwner = meta.getClass().getMethod("setOwner", String.class);
                    setOwner.invoke(meta, username);
                } catch (Exception ignored) {}
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

    public static ItemStack createMaterialSafe(String name) {
        if (name == null || name.equalsIgnoreCase("NONE") || name.isEmpty()) {
            return null;
        }
        try {
            return new ItemStack(Material.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            if (name.equalsIgnoreCase("TRIDENT") || name.equalsIgnoreCase("CROSSBOW")) {
                return new ItemStack(Material.BOW);
            }
            return new ItemStack(Material.DIAMOND_SWORD);
        }
    }

    public static void setMarkerSafe(ArmorStand stand, boolean marker) {
        try {
            stand.setMarker(marker);
        } catch (NoSuchMethodError ignored) {}
    }
}
