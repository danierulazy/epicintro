package id.danierulazy.acediamcintro;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class ShowcaseEngine {

    private final AcediaMCIntroPlugin plugin;
    private final EpicMusicEngine musicEngine;
    private BukkitTask activeTask;
    private ArmorStand activeFigure;
    private ArmorStand nameHologram;
    private boolean running = false;

    private final Map<UUID, GameMode> previousModes = new HashMap<>();
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public ShowcaseEngine(AcediaMCIntroPlugin plugin) {
        this.plugin = plugin;
        this.musicEngine = new EpicMusicEngine(plugin);
    }

    public boolean isRunning() {
        return running;
    }

    public void startShowcase(List<MemberEntry> members, Location stageLoc, Location camLoc) {
        if (running) {
            stopShowcase();
        }
        if (members.isEmpty()) {
            return;
        }

        running = true;
        prepareAudience(camLoc);
        musicEngine.startMusic(stageLoc);

        int bannerSeconds = plugin.getConfig().getInt("settings.intro-banner-seconds", 4);
        int memberSeconds = plugin.getConfig().getInt("settings.member-duration-seconds", 6);
        String serverTitle = plugin.getConfig().getString("settings.server-title", "&6&lACEDIAMC");
        String serverSub = plugin.getConfig().getString("settings.server-subtitle", "&f&lPERKENALAN MEMBER");

        final int bannerTicks = bannerSeconds * 20;
        final int memberTicks = memberSeconds * 20;
        final int totalMembers = members.size();

        activeTask = new BukkitRunnable() {
            int globalTick = 0;
            int currentMemberIdx = -1;

            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }

                if (globalTick < bannerTicks) {
                    if (globalTick == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            VersionCompat.sendTitle(p, serverTitle, serverSub, 10, bannerTicks - 10, 10);
                        }
                    }
                    if (globalTick % 5 == 0) {
                        VersionCompat.spawnParticleSafe(stageLoc.getWorld(), stageLoc.clone().add(0, 1.5, 0),
                            35, 1.8, 1.2, 1.8, 0.08, "FIREWORKS_SPARK", "FIREWORK", "FLAME");
                    }
                    keepCameraLocked(camLoc, stageLoc, 0.0);
                    globalTick++;
                    return;
                }

                int memberPhaseTick = globalTick - bannerTicks;
                int targetIdx = memberPhaseTick / memberTicks;
                int localTick = memberPhaseTick % memberTicks;

                if (targetIdx >= totalMembers) {
                    stopShowcase();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        VersionCompat.sendTitle(p, "&6&lSHOWCASE SELESAI!", "&7Selamat bertualang di AcediaMC!", 10, 50, 20);
                    }
                    return;
                }

                MemberEntry current = members.get(targetIdx);

                if (targetIdx != currentMemberIdx) {
                    currentMemberIdx = targetIdx;
                    cleanupEntities();
                    spawnMemberFigure(current, stageLoc);
                    musicEngine.playMemberRevealImpact();
                }

                animateMemberFrame(current, stageLoc, camLoc, localTick, memberTicks, totalMembers);
                globalTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnMemberFigure(MemberEntry member, Location stageLoc) {
        World world = stageLoc.getWorld();
        Location spawnLoc = stageLoc.clone();

        String anim = member.getAnimation().toUpperCase();
        if (anim.equals("SKY_SLAM") || anim.equals("LEVITATION_DROP")) {
            spawnLoc.add(0, 7.0, 0);
        } else if (anim.equals("HERO_CHARGE")) {
            Vector back = stageLoc.getDirection().setY(0).normalize().multiply(-4.5);
            spawnLoc.add(back);
        }

        activeFigure = (ArmorStand) world.spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        activeFigure.setBasePlate(false);
        activeFigure.setArms(true);
        activeFigure.setGravity(false);
        activeFigure.setCanPickupItems(false);
        activeFigure.setCustomNameVisible(false);

        activeFigure.setHelmet(VersionCompat.createPlayerHead(member.getUsername()));
        activeFigure.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        activeFigure.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        activeFigure.setBoots(new ItemStack(Material.LEATHER_BOOTS));

        ItemStack hand = VersionCompat.createMaterialSafe(member.getHandItem());
        if (hand != null) {
            activeFigure.setItemInHand(hand);
        }

        nameHologram = (ArmorStand) world.spawnEntity(spawnLoc.clone().add(0, 2.35, 0), EntityType.ARMOR_STAND);
        nameHologram.setVisible(false);
        nameHologram.setGravity(false);
        VersionCompat.setMarkerSafe(nameHologram, true);
        nameHologram.setCustomName(ChatColor.translateAlternateColorCodes('&', member.getDisplayName()));
        nameHologram.setCustomNameVisible(true);
    }

    private void animateMemberFrame(MemberEntry member, Location stageLoc, Location camLoc,
                                    int localTick, int maxTicks, int totalMembers) {
        if (activeFigure == null || !activeFigure.isValid()) return;

        String anim = member.getAnimation().toUpperCase();
        double progress = (double) localTick / maxTicks;
        Location currentLoc = activeFigure.getLocation();

        if (localTick <= 25) {
            double t = localTick / 25.0;
            if (anim.equals("SKY_SLAM") || anim.equals("LEVITATION_DROP")) {
                double yOffset = 7.0 * (1.0 - (t * t));
                currentLoc = stageLoc.clone().add(0, yOffset, 0);
                activeFigure.teleport(currentLoc);
                if (localTick == 25) {
                    VersionCompat.spawnParticleSafe(stageLoc.getWorld(), stageLoc.clone().add(0, 0.2, 0),
                        60, 1.5, 0.2, 1.5, 0.12, "EXPLOSION_LARGE", "CLOUD", "SMOKE_LARGE");
                }
            } else if (anim.equals("HERO_CHARGE")) {
                Vector offset = stageLoc.getDirection().setY(0).normalize().multiply(-4.5 * (1.0 - t));
                currentLoc = stageLoc.clone().add(offset);
                activeFigure.teleport(currentLoc);
            } else if (anim.equals("TELEPORT_FLASH")) {
                currentLoc = stageLoc.clone();
                currentLoc.setYaw(stageLoc.getYaw() + (float) (360.0 * (1.0 - t)));
                activeFigure.teleport(currentLoc);
            } else {
                VersionCompat.spawnParticleSafe(stageLoc.getWorld(), stageLoc.clone().add(0, 1.0, 0),
                    40, 0.8, 0.9, 0.8, 0.05, "CLOUD", "SMOKE_NORMAL");
            }
        } else {
            double wave = Math.sin((localTick - 25) * 0.22);
            activeFigure.setRightArmPose(new EulerAngle(Math.toRadians(-125 + wave * 18), 0, Math.toRadians(12)));
            activeFigure.setLeftArmPose(new EulerAngle(Math.toRadians(-20 - wave * 10), 0, Math.toRadians(-15)));
            activeFigure.setHeadPose(new EulerAngle(Math.toRadians(wave * 5), Math.toRadians(wave * 8), 0));
        }

        if (nameHologram != null && nameHologram.isValid()) {
            nameHologram.teleport(activeFigure.getLocation().clone().add(0, 2.35, 0));
        }

        if (localTick == 15) {
            String footerInfo = "&eMEMBER " + String.format("%02d", member.getIndex()) + " OF " + String.format("%02d", totalMembers)
                              + " &8| " + member.getQuote();
            for (Player p : Bukkit.getOnlinePlayers()) {
                VersionCompat.sendTitle(p, member.getDisplayName(), footerInfo, 5, maxTicks - 25, 10);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            VersionCompat.sendActionBar(p, member.getDisplayName() + " &8» &f" + member.getQuote());
        }

        if (localTick % 2 == 0) {
            spawnThemedParticles(member.getParticleTheme(), activeFigure.getLocation().clone().add(0, 1.2, 0));
        }

        double zoomFactor = Math.min(1.0, progress * 1.3) * 0.35;
        keepCameraLocked(camLoc, activeFigure.getLocation().clone().add(0, 1.3, 0), zoomFactor);
    }

    private void spawnThemedParticles(String theme, Location center) {
        World w = center.getWorld();
        String t = theme == null ? "" : theme.toUpperCase();
        if (t.contains("FLAME")) {
            VersionCompat.spawnParticleSafe(w, center, 18, 1.1, 1.0, 1.1, 0.05, "FLAME", "LAVA");
        } else if (t.contains("GOLD") || t.contains("TOTEM")) {
            VersionCompat.spawnParticleSafe(w, center, 22, 1.2, 1.1, 1.2, 0.08, "TOTEM", "VILLAGER_HAPPY", "FIREWORKS_SPARK");
        } else if (t.contains("HEART")) {
            VersionCompat.spawnParticleSafe(w, center, 14, 1.1, 1.0, 1.1, 0.04, "HEART", "CRIT_MAGIC", "NOTE");
        } else {
            VersionCompat.spawnParticleSafe(w, center, 20, 1.2, 1.0, 1.2, 0.06, "FIREWORKS_SPARK", "SPELL_WITCH", "CLOUD");
        }
    }

    private void keepCameraLocked(Location camLoc, Location lookTarget, double zoomRatio) {
        Vector toTarget = lookTarget.toVector().subtract(camLoc.toVector());
        Location dynamicCam = camLoc.clone().add(toTarget.clone().multiply(zoomRatio));

        Vector dir = lookTarget.toVector().subtract(dynamicCam.toVector()).normalize();
        double dx = dir.getX();
        double dy = dir.getY();
        double dz = dir.getZ();

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.asin(dy));
        dynamicCam.setYaw(yaw);
        dynamicCam.setPitch(pitch);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) {
                p.setGameMode(GameMode.SPECTATOR);
            }
            p.teleport(dynamicCam);
        }
    }

    private void prepareAudience(Location camLoc) {
        previousModes.clear();
        previousLocations.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            previousModes.put(p.getUniqueId(), p.getGameMode());
            previousLocations.put(p.getUniqueId(), p.getLocation());
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(camLoc);
        }
    }

    public void stopShowcase() {
        running = false;
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
        musicEngine.stopMusic();
        cleanupEntities();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (previousModes.containsKey(p.getUniqueId())) {
                p.setGameMode(previousModes.get(p.getUniqueId()));
            }
            if (previousLocations.containsKey(p.getUniqueId())) {
                p.teleport(previousLocations.get(p.getUniqueId()));
            }
        }
        previousModes.clear();
        previousLocations.clear();
    }

    private void cleanupEntities() {
        if (activeFigure != null) {
            activeFigure.remove();
            activeFigure = null;
        }
        if (nameHologram != null) {
            nameHologram.remove();
            nameHologram = null;
        }
    }
}
