package follow.testPracticFollow.Command;

import follow.testPracticFollow.manager.Manager;
import org.mineacademy.fo.command.SimpleCommand;

public class UnfollowAllCommand extends SimpleCommand {

    private final Manager manager;

    public UnfollowAllCommand(Manager manager) {
        super("unfollowall");
        this.manager = manager;
        setDescription("Очистить весь список слежки");
        setPermission(null);
    }

    @Override
    protected void onCommand() {
        checkConsole();
        manager.unfollowAll(getPlayer().getUniqueId());
        tell("&cВы очистили весь список слежки.");
    }
}
