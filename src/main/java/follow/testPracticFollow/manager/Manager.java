package follow.testPracticFollow.manager;

import follow.testPracticFollow.Follow;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.CommonCore;
import org.mineacademy.fo.database.SimpleDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class Manager {

    private static final int DEFAULT_LIMIT = 2;

    private final Follow plugin;

    private final Map<UUID, Set<UUID>> followMap = new ConcurrentHashMap<>();
    private final Map<String, UUID>    nameCache  = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar>   trackBars  = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar>   notifyBars = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    private FollowDatabase db;

    public void init() {
        db = new FollowDatabase();
        db.connect("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/follow_data.db");
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateTrackBars, 20L, 20L);
    }

    public void close() {
        if (updateTask != null) updateTask.cancel();
        if (db != null) db.disconnect();
        trackBars.values().forEach(BossBar::removeAll);
        notifyBars.values().forEach(BossBar::removeAll);
        trackBars.clear();
        notifyBars.clear();
    }

    void onDbConnected() {
        loadAll();
    }

    private void loadAll() {
        try (ResultSet rs = db.selectAll()) {
            if (rs == null) return;
            while (rs.next()) {
                UUID follower = UUID.fromString(rs.getString("follower"));
                UUID target   = UUID.fromString(rs.getString("target"));
                String name   = rs.getString("target_name");
                followMap.computeIfAbsent(follower, k -> ConcurrentHashMap.newKeySet()).add(target);
                nameCache.put(name.toLowerCase(), target);
            }
        } catch (SQLException e) {
            CommonCore.error(e, "Ошибка загрузки данных слежки");
        }
    }

    private void updateTrackBars() {
        for (Map.Entry<UUID, Set<UUID>> entry : followMap.entrySet()) {
            UUID followerUUID = entry.getKey();
            Player follower = Bukkit.getPlayer(followerUUID);
            if (follower == null || !follower.isOnline()) continue;

            Player target = null;
            for (UUID targetUUID : entry.getValue()) {
                Player t = Bukkit.getPlayer(targetUUID);
                if (t != null && t.isOnline()) {
                    target = t;
                    break;
                }
            }

            if (target == null) {
                BossBar bar = trackBars.remove(followerUUID);
                if (bar != null) bar.removeAll();
                continue;
            }

            Location loc = target.getLocation();
            String text = "§e" + target.getName() + " §7| §bX:" + loc.getBlockX()
                + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ()
                + " §7| §a" + loc.getWorld().getName();

            BossBar bar = trackBars.get(followerUUID);
            if (bar == null) {
                bar = Bukkit.createBossBar(text, BarColor.GREEN, BarStyle.SOLID);
                bar.addPlayer(follower);
                trackBars.put(followerUUID, bar);
            } else {
                bar.setTitle(text);
            }
        }
    }

    public void notifyFollower(UUID followerUUID, String message) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player follower = Bukkit.getPlayer(followerUUID);
            if (follower == null || !follower.isOnline()) return;

            String colored = message.replace("&", "§");

            BossBar old = notifyBars.remove(followerUUID);
            if (old != null) old.removeAll();

            BossBar bar = Bukkit.createBossBar(colored, BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(follower);
            notifyBars.put(followerUUID, bar);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                BossBar current = notifyBars.remove(followerUUID);
                if (current != null) current.removeAll();
            }, 100L);
        }, 5L);
    }

    public void notifyAllFollowers(UUID targetUUID, String message) {
        for (Map.Entry<UUID, Set<UUID>> entry : followMap.entrySet())
            if (entry.getValue().contains(targetUUID))
                notifyFollower(entry.getKey(), message);
    }

    public void refreshTrackBar(UUID followerUUID) {
        Bukkit.getScheduler().runTaskLater(plugin, this::updateTrackBars, 1L);
    }

    public int getLimit(Player player) {
        for (int i = 100; i >= 1; i--)
            if (player.hasPermission("follows." + i))
                return i;
        return DEFAULT_LIMIT;
    }

    public Set<UUID> getFollowing(UUID follower) {
        return Collections.unmodifiableSet(followMap.getOrDefault(follower, Collections.emptySet()));
    }

    public boolean isFollowing(UUID follower, UUID target) {
        Set<UUID> set = followMap.get(follower);
        return set != null && set.contains(target);
    }

    public boolean follow(Player follower, String playerName, UUID targetUUID) {
        UUID fid = follower.getUniqueId();
        if (isFollowing(fid, targetUUID)) return false;

        Set<UUID> set = followMap.computeIfAbsent(fid, k -> ConcurrentHashMap.newKeySet());
        if (set.size() >= getLimit(follower)) return false;

        set.add(targetUUID);
        nameCache.put(playerName.toLowerCase(), targetUUID);
        db.insertFollow(fid, targetUUID, playerName);
        refreshTrackBar(fid);
        return true;
    }

    public boolean unfollow(UUID follower, UUID target) {
        Set<UUID> set = followMap.get(follower);
        if (set == null || !set.remove(target)) return false;
        db.deleteFollow(follower, target);
        refreshTrackBar(follower);
        return true;
    }

    public void unfollowAll(UUID follower) {
        followMap.remove(follower);
        db.deleteAllFollows(follower);
        BossBar bar = trackBars.remove(follower);
        if (bar != null) bar.removeAll();
    }

    public UUID getUUIDByName(String name) {
        return nameCache.get(name.toLowerCase());
    }

    public String getPlayerName(UUID targetUUID) {
        return nameCache.entrySet().stream()
            .filter(e -> e.getValue().equals(targetUUID))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(targetUUID.toString().substring(0, 8));
    }

    class FollowDatabase extends SimpleDatabase {

        @Override
        protected void onConnected() {
            this.updateUnsafe(
                "CREATE TABLE IF NOT EXISTS follows (" +
                "  follower    TEXT NOT NULL," +
                "  target      TEXT NOT NULL," +
                "  target_name TEXT NOT NULL," +
                "  PRIMARY KEY (follower, target)" +
                ")"
            );
            onDbConnected();
        }

        ResultSet selectAll() {
            return this.queryUnsafe("SELECT * FROM follows");
        }

        void insertFollow(UUID follower, UUID target, String playerName) {
            try (var ps = this.prepareStatement(
                    "INSERT OR IGNORE INTO follows (follower, target, target_name) VALUES (?, ?, ?)")) {
                ps.setString(1, follower.toString());
                ps.setString(2, target.toString());
                ps.setString(3, playerName);
                ps.executeUpdate();
            } catch (SQLException e) {
                CommonCore.error(e, "Ошибка сохранения слежки");
            }
        }

        void deleteFollow(UUID follower, UUID target) {
            try (var ps = this.prepareStatement(
                    "DELETE FROM follows WHERE follower = ? AND target = ?")) {
                ps.setString(1, follower.toString());
                ps.setString(2, target.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                CommonCore.error(e, "Ошибка удаления слежки");
            }
        }

        void deleteAllFollows(UUID follower) {
            try (var ps = this.prepareStatement(
                    "DELETE FROM follows WHERE follower = ?")) {
                ps.setString(1, follower.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                CommonCore.error(e, "Ошибка очистки слежки");
            }
        }
    }
}
