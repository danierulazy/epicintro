package id.danierulazy.acediamcintro;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class EpicMusicEngine {

    private final AcediaMCIntroPlugin plugin;
    private BukkitTask musicTask;

    private static final float[] BASS_NOTES = {
        0.5f, 0.5f, 0.75f, 0.5f, 0.6f, 0.6f, 0.8f, 0.9f,
        0.5f, 0.5f, 0.75f, 1.0f, 0.9f, 0.75f, 0.6f, 0.5f
    };

    private static final float[] LEAD_NOTES = {
        1.0f, 0.0f, 1.189f, 1.335f, 1.498f, 0.0f, 1.335f, 1.498f,
        1.782f, 0.0f, 1.498f, 1.335f, 1.189f, 1.335f, 1.0f, 0.0f
    };

    public EpicMusicEngine(AcediaMCIntroPlugin plugin) {
        this.plugin = plugin;
    }

    public void startMusic(Location center) {
        stopMusic();
        if (!plugin.getConfig().getBoolean("settings.music.enabled", true)) {
            return;
        }

        String soundName = plugin.getConfig().getString("settings.music.sound-name", "BUILT_IN_EPIC");
        float volume = (float) plugin.getConfig().getDouble("settings.music.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("settings.music.pitch", 1.0);

        if (!"BUILT_IN_EPIC".equalsIgnoreCase(soundName)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), soundName, volume, pitch);
            }
            return;
        }

        musicTask = new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                int idx = step % BASS_NOTES.length;
                float bassPitch = BASS_NOTES[idx];
                float leadPitch = LEAD_NOTES[idx];

                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation();
                    if (step % 4 == 0) {
                        VersionCompat.playSoundSafe(p, loc, volume, 0.6f,
                            "ENTITY_ENDER_DRAGON_FLAP", "ENDERDRAGON_WINGS", "BLOCK_NOTE_BLOCK_BASEDRUM", "NOTE_BASS_DRUM");
                    }
                    if (step % 4 == 2) {
                        VersionCompat.playSoundSafe(p, loc, volume * 0.8f, 1.2f,
                            "BLOCK_NOTE_BLOCK_SNARE", "NOTE_SNARE_DRUM");
                    }
                    VersionCompat.playSoundSafe(p, loc, volume, bassPitch,
                        "BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS");

                    if (leadPitch > 0.0f) {
                        VersionCompat.playSoundSafe(p, loc, volume, leadPitch,
                            "BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING", "BLOCK_NOTE_BLOCK_HARP");
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    public void playMemberRevealImpact() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location loc = p.getLocation();
            VersionCompat.playSoundSafe(p, loc, 1.0f, 1.0f,
                "ENTITY_FIREWORK_ROCKET_LARGE_BLAST", "FIREWORK_LARGE_BLAST", "ENTITY_GENERIC_EXPLODE");
            VersionCompat.playSoundSafe(p, loc, 0.8f, 1.2f,
                "ITEM_TOTEM_USE", "ENTITY_PLAYER_LEVELUP", "LEVEL_UP");
        }
    }

    public void stopMusic() {
        if (musicTask != null) {
            musicTask.cancel();
            musicTask = null;
        }
    }
}
