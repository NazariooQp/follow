package follow.testPracticFollow.Command;

import follow.testPracticFollow.manager.Manager;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

import java.util.UUID;

public class UnfollowCommand extends SimpleCommand {

    private final Manager manager;

    public UnfollowCommand(Manager manager) {
        super("unfollow");
        this.manager = manager;
        setDescription("Прекратить слежку за игроком");
        setUsage("<nick>");
        setMinArguments(1);
        setPermission(null);
    }

    @Override
    protected void onCommand() {
        checkConsole();

        Player follower = getPlayer();
        String playerName = args[0];

        UUID targetUUID = manager.getUUIDByName(playerName);
        if (targetUUID == null) {
            returnTell("&cВы не следите за &e" + playerName + "&c.");
            return;
        }

        if (!manager.unfollow(follower.getUniqueId(), targetUUID)) {
            returnTell("&cВы не следите за &e" + playerName + "&c.");
            return;
        }

        tell("&cВы прекратили слежку за &e" + playerName + "&c.");
    }
}
