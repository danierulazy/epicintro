package id.danierulazy.acediamcintro;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AcediaMCIntroPlugin extends JavaPlugin {

    private ShowcaseEngine showcaseEngine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.showcaseEngine = new ShowcaseEngine(this);
        getLogger().info("AcediaMCIntro berhasil aktif! Support lintas versi Minecraft (1.8 - 1.21+).");
    }

    @Override
    public void onDisable() {
        if (showcaseEngine != null && showcaseEngine.isRunning()) {
            showcaseEngine.stopShowcase();
        }
    }

    private Location readLocation(String path, Location fallback) {
        ConfigurationSection sec = getConfig().getConfigurationSection(path);
        if (sec == null) return fallback;
        World world = Bukkit.getWorld(sec.getString("world", "world"));
        if (world == null) world = fallback.getWorld();
        return new Location(
            world,
            sec.getDouble("x", fallback.getX()),
            sec.getDouble("y", fallback.getY()),
            sec.getDouble("z", fallback.getZ()),
            (float) sec.getDouble("yaw", fallback.getYaw()),
            (float) sec.getDouble("pitch", fallback.getPitch())
        );
    }

    private void saveLocation(String path, Location loc) {
        getConfig().set(path + ".world", loc.getWorld().getName());
        getConfig().set(path + ".x", loc.getX());
        getConfig().set(path + ".y", loc.getY());
        getConfig().set(path + ".z", loc.getZ());
        getConfig().set(path + ".yaw", loc.getYaw());
        getConfig().set(path + ".pitch", loc.getPitch());
        saveConfig();
    }

    private List<MemberEntry> loadMembers() {
        List<MemberEntry> list = new ArrayList<>();
        ConfigurationSection sec = getConfig().getConfigurationSection("members");
        if (sec == null) return list;

        int idx = 1;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection m = sec.getConfigurationSection(key);
            if (m == null) continue;
            list.add(new MemberEntry(
                idx++,
                m.getString("username", "Steve"),
                m.getString("display-name", "&6[0" + key + "] &e" + m.getString("username", "Member")),
                m.getString("quote", "&7Siap bertualang di AcediaMC!"),
                m.getString("animation", "EMERGE_SMOKE"),
                m.getString("hand-item", "DIAMOND_SWORD"),
                m.getString("particle-theme", "GOLD_SPARKS")
            ));
        }
        return list;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("acediaintro.admin")) {
            sender.sendMessage(ChatColor.RED + "Kamu tidak memiliki izin untuk menjalankan perintah ini.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== AcediaMCIntro Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro start " + ChatColor.GRAY + "- Mulai pertunjukan intro semua member");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro preview <username> [animasi] " + ChatColor.GRAY + "- Tes intro 1 player saja");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro stop " + ChatColor.GRAY + "- Hentikan pertunjukan");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro setstage " + ChatColor.GRAY + "- Set titik tengah panggung");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro setcam " + ChatColor.GRAY + "- Set titik kamera penonton");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro addmember <username> <animasi> <quote...> " + ChatColor.GRAY + "- Tambah member baru");
            sender.sendMessage(ChatColor.YELLOW + "/acediaintro reload " + ChatColor.GRAY + "- Muat ulang config.yml");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "setstage": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Perintah ini hanya bisa dijalankan di dalam game.");
                    return true;
                }
                Player p = (Player) sender;
                saveLocation("locations.stage", p.getLocation());
                p.sendMessage(ChatColor.GREEN + "Titik panggung (Stage) AcediaMC berhasil disimpan di posisimu!");
                return true;
            }
            case "setcam": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Perintah ini hanya bisa dijalankan di dalam game.");
                    return true;
                }
                Player p = (Player) sender;
                saveLocation("locations.camera", p.getLocation());
                p.sendMessage(ChatColor.GREEN + "Titik kamera (Camera) AcediaMC berhasil disimpan di posisimu!");
                return true;
            }
            case "start": {
                World defaultWorld = Bukkit.getWorlds().get(0);
                Location fallbackStage = (sender instanceof Player) ? ((Player) sender).getLocation() : defaultWorld.getSpawnLocation();
                Location fallbackCam = fallbackStage.clone().add(0, 2.5, 6.0);

                Location stage = readLocation("locations.stage", fallbackStage);
                Location cam = readLocation("locations.camera", fallbackCam);
                List<MemberEntry> members = loadMembers();

                if (members.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "Daftar member di config.yml masih kosong!");
                    return true;
                }
                showcaseEngine.startShowcase(members, stage, cam);
                sender.sendMessage(ChatColor.GOLD + "Pertunjukan AcediaMCIntro dimulai (" + members.size() + " member)!");
                return true;
            }
            case "preview": {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /acediaintro preview <username> [EMERGE_SMOKE|SKY_SLAM|TELEPORT_FLASH|HERO_CHARGE|LEVITATION_DROP]");
                    return true;
                }
                String targetName = args[1];
                String anim = args.length >= 3 ? args[2].toUpperCase() : "SKY_SLAM";
                World defaultWorld = Bukkit.getWorlds().get(0);
                Location fallbackStage = (sender instanceof Player) ? ((Player) sender).getLocation() : defaultWorld.getSpawnLocation();
                Location stage = readLocation("locations.stage", fallbackStage);
                Location cam = readLocation("locations.camera", stage.clone().add(0, 2.5, 6.0));

                MemberEntry solo = new MemberEntry(1, targetName, "&6[PREVIEW] &e" + targetName, "&7Tampilan Preview Skin & Animasi AcediaMC!", anim, "TRIDENT", "GOLD_SPARKS");
                showcaseEngine.startShowcase(Collections.singletonList(solo), stage, cam);
                return true;
            }
            case "addmember": {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /acediaintro addmember <username> <animasi> <quote...>");
                    return true;
                }
                String user = args[1];
                String anim = args[2].toUpperCase();
                StringBuilder quoteBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    quoteBuilder.append(args[i]).append(" ");
                }
                int nextNum = 1;
                if (getConfig().getConfigurationSection("members") != null) {
                    nextNum = getConfig().getConfigurationSection("members").getKeys(false).size() + 1;
                }
                String numStr = String.format("%02d", nextNum);
                String path = "members." + nextNum;
                getConfig().set(path + ".username", user);
                getConfig().set(path + ".display-name", "&6[" + numStr + "] &e" + user);
                getConfig().set(path + ".quote", "&7" + quoteBuilder.toString().trim());
                getConfig().set(path + ".animation", anim);
                getConfig().set(path + ".hand-item", "DIAMOND_SWORD");
                getConfig().set(path + ".particle-theme", "GOLD_SPARKS");
                saveConfig();

                sender.sendMessage(ChatColor.GREEN + "Member #" + numStr + " (" + user + ") berhasil ditambahkan ke AcediaMC!");
                return true;
            }
            case "stop": {
                showcaseEngine.stopShowcase();
                sender.sendMessage(ChatColor.YELLOW + "Pertunjukan AcediaMCIntro dihentikan.");
                return true;
            }
            case "reload": {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Konfigurasi AcediaMCIntro berhasil dimuat ulang!");
                return true;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Sub-perintah tidak dikenal. Ketik /acediaintro untuk bantuan.");
                return true;
        }
    }
}
