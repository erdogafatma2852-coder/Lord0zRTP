
package me.lord0z.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class Lord0zRTP extends JavaPlugin {

    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> tasks = new HashMap<>();

    private static final String WORLD_NAME = "SMP";
    private static final int RADIUS = 5000;
    private static final int COOLDOWN = 60;

    @Override
    public void onEnable() {
        getLogger().info("RTP sistemi etkin.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rtp")) return false;
        if (!(sender instanceof Player player)) return true;

        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) return true;

        UUID id = player.getUniqueId();

        if (tasks.containsKey(id)) return true;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(id, 0L);

        if (now - last < COOLDOWN * 1000L) {
            player.sendMessage("§dRTP kullanmak için biraz beklemelisin.");
            return true;
        }

        Location start = player.getLocation();
        player.sendMessage("§dRTP başlatıldı, 5 saniye bekle...");

        BukkitRunnable task = new BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    tasks.remove(id);
                    cancel();
                    return;
                }

                if (player.getWorld() != start.getWorld()
                        || player.getLocation().distanceSquared(start) > 0.04) {
                    player.sendMessage("§cHareket ettiğin için RTP iptal edildi!");
                    tasks.remove(id);
                    cancel();
                    return;
                }

                if (--seconds > 0) return;

                tasks.remove(id);
                cancel();

                Location target = findSafeLocation(world);
                if (target == null) {
                    player.sendMessage("§cGüvenli konum bulunamadı. Tekrar dene!");
                    return;
                }

                player.teleportAsync(target).thenAccept(success ->
                    Bukkit.getScheduler().runTask(Lord0zRTP.this, () -> {
                        if (success && player.isOnline()) {
                            cooldowns.put(id, System.currentTimeMillis());
                            player.sendMessage("§dRTP başarılı! İyi oyunlar.");
                        }
                    })
                );
            }
        };

        int taskId = task.runTaskTimer(this, 0L, 20L).getTaskId();
        tasks.put(id, taskId);
        return true;
    }

    private Location findSafeLocation(World world) {
        for (int i = 0; i < 40; i++) {
            int x = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int z = random.nextInt(RADIUS * 2 + 1) - RADIUS;

            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight() || y + 2 >= world.getMaxHeight()) continue;

            Block ground = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);

            if (!ground.getType().isSolid()) continue;
            if (feet.getType() != Material.AIR || head.getType() != Material.AIR) continue;
            if (ground.isLiquid() || ground.getType() == Material.MAGMA_BLOCK
                    || ground.getType() == Material.CACTUS
                    || ground.getType() == Material.CAMPFIRE
                    || ground.getType() == Material.SOUL_CAMPFIRE) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }
}
