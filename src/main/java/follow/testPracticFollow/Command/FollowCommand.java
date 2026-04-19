package follow.testPracticFollow.Command;

import follow.testPracticFollow.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

import java.util.UUID;

public class FollowCommand extends SimpleCommand {

    private final Manager manager;

    public FollowCommand(Manager manager) {
        super("follow");
        this.manager = manager;
        setDescription("Начать слежку за игроком");
        setUsage("<nick>");
        setMinArguments(1);
        setPermission(null);
    }

    @Override
    protected void onCommand() {
        checkConsole();

        Player follower = getPlayer();
        String playerName = args[0];

        Player online = Bukkit.getPlayerExact(playerName);
        UUID targetUUID;

        if (online != null) {
            targetUUID = online.getUniqueId();
            playerName = online.getName();
        } else {
            targetUUID = manager.getUUIDByName(playerName);
            if (targetUUID == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
                if (!op.hasPlayedBefore()) {
                    returnTell("&cИгрок &e" + playerName + " &cне найден.");
                    return;
                }
                targetUUID = op.getUniqueId();
                playerName = op.getName() != null ? op.getName() : playerName;
            }
        }

        if (targetUUID.equals(follower.getUniqueId())) {
            returnTell("&cВы не можете следить за собой.");
            return;
        }

        if (manager.isFollowing(follower.getUniqueId(), targetUUID)) {
            returnTell("&cВы уже следите за &e" + playerName + "&c.");
            return;
        }

        int limit = manager.getLimit(follower);
        if (manager.getFollowing(follower.getUniqueId()).size() >= limit) {
            returnTell("&cВы достигли лимита слежки &e(" + limit + " слотов)&c.");
            return;
        }

        manager.follow(follower, playerName, targetUUID);
        tell("&aВы начали слежку за &e" + playerName + "&a.");
    }
}
