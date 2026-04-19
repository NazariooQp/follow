package follow.testPracticFollow.Command;

import follow.testPracticFollow.manager.Manager;
import follow.testPracticFollow.menu.Menu;
import org.bukkit.entity.Player;
import org.mineacademy.fo.command.SimpleCommand;

public class FollowsCommand extends SimpleCommand {

    private final Manager manager;

    public FollowsCommand(Manager manager) {
        super("follows");
        this.manager = manager;
        setDescription("Открыть меню отслеживаемых игроков");
        setPermission(null);
    }

    @Override
    protected void onCommand() {
        checkConsole();
        Player player = getPlayer();
        new Menu(player, manager).displayTo(player);
    }
}
