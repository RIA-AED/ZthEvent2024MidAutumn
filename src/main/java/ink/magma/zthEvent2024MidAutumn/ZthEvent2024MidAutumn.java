package ink.magma.zthEvent2024MidAutumn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ZthEvent2024MidAutumn extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Random random;
    private boolean isEventActive = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        random = new Random();
        getServer().getPluginManager().registerEvents(this, this);

        // 启动定时任务，每10秒检查一次月相和事件状态
        new BukkitRunnable() {
            @Override
            public void run() {
                checkEventStatus();
                if (isEventActive) {
                    checkAndSetFullMoon();
                }
            }
        }.runTaskTimer(this, 0L, 200L); // 0L表示立即开始，200L表示每200tick（10秒）执行一次
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    // 检查事件是否应该结束
    private void checkEventStatus() {
        LocalDateTime endTime = LocalDateTime.of(2024, 9, 18, 4, 0);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (now.isAfter(endTime)) {
            isEventActive = false;
        }
    }

    // 检查并设置满月的方法
    private void checkAndSetFullMoon() {
        for (World world : getServer().getWorlds()) {
            long fullTime = world.getFullTime();
            if (fullTime > 24000) {
                long newTime = fullTime % 24000;
                world.setFullTime(newTime);
            }
        }
    }

    @EventHandler
    public void onPhantomSpawn(EntitySpawnEvent event) {
        if (!isEventActive || event.getEntityType() != EntityType.PHANTOM)
            return;
        Phantom phantom = (Phantom) event.getEntity();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (phantom.isValid()) {
                    phantom.setHealth(0);
                }
            }
        }.runTaskLater(this, 60); // 3秒后执行
    }

    @EventHandler
    public void onPhantomDeath(EntityDeathEvent event) {
        if (!isEventActive || event.getEntityType() != EntityType.PHANTOM)
            return;
        Location location = event.getEntity().getLocation();

        if (random.nextDouble() <= 0.75) {
            List<ItemStack> customDrops = (List<ItemStack>) config.getList("customDrops");
            if (customDrops != null && !customDrops.isEmpty()) {
                int dropCount = random.nextInt(2) + 1; // 随机决定掉落1-2种物品
                for (int i = 0; i < dropCount; i++) {
                    ItemStack drop = customDrops.get(random.nextInt(customDrops.size()));
                    drop = drop.clone();
                    drop.setAmount(1);
                    Item droppedItem = location.getWorld().dropItemNaturally(location, drop);
                    droppedItem.setGlowing(true);
                }

                for (Player player : location.getWorld().getPlayers()) {
                    if (player.getLocation().distance(location) <= 48) {
                        player.sendMessage(Component.text("幻翼送上了一份中秋祝福...").color(NamedTextColor.GRAY));
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!isEventActive) {
            sender.sendMessage(Component.text("中秋节活动已结束。").color(NamedTextColor.RED));
        }

        if (label.equalsIgnoreCase("zthevent2024midautumn")) {
            if (args.length < 1) {
                return false;
            }

            if (args[0].equalsIgnoreCase("add")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("只有玩家可以使用此命令。").color(NamedTextColor.RED));
                    return true;
                }
                ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                List<ItemStack> customDrops = (List<ItemStack>) config.getList("customDrops", new ArrayList<>());
                customDrops.add(item);
                config.set("customDrops", customDrops);
                saveConfig();
                sender.sendMessage(Component.text("物品已添加到自定义掉落列表。").color(NamedTextColor.GREEN));
                return true;
            } else if (args[0].equalsIgnoreCase("clear")) {
                config.set("customDrops", new ArrayList<>());
                saveConfig();
                sender.sendMessage(Component.text("已清除所有自定义掉落物。").color(NamedTextColor.GREEN));
                return true;
            }
        }
        return false;
    }
}
