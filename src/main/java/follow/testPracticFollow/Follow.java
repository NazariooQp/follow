package follow.testPracticFollow;

import follow.testPracticFollow.Command.FollowCommand;
import follow.testPracticFollow.Command.FollowsCommand;
import follow.testPracticFollow.Command.UnfollowAllCommand;
import follow.testPracticFollow.Command.UnfollowCommand;
import follow.testPracticFollow.listener.NetworkListener;
import follow.testPracticFollow.manager.Manager;
import lombok.Getter;
import org.mineacademy.fo.platform.BukkitPlugin;

public final class Follow extends BukkitPlugin {

    @Getter
    private static Follow instance;

    @Getter
    private Manager followManager;

    @Override
    protected void onPluginStart() {
        instance = this;

        followManager = new Manager(this);
        followManager.init();

        NetworkListener networkListener = new NetworkListener(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", networkListener);
        getServer().getPluginManager().registerEvents(networkListener, this);

        new FollowCommand(followManager).register();
        new UnfollowCommand(followManager).register();
        new UnfollowAllCommand(followManager).register();
        new FollowsCommand(followManager).register();
    }

    @Override
    protected void onPluginStop() {
        if (followManager != null)
            followManager.close();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord");
    }
}
