package follow.testPracticFollow.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import follow.testPracticFollow.Follow;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

@RequiredArgsConstructor
public class NetworkListener implements PluginMessageListener, Listener {

    public static final String CHANNEL     = "BungeeCord";
    public static final String SUB_CHANNEL = "FollowSync";

    private final Follow plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        notifyLocal(player.getUniqueId(), player.getName(), "JOIN", null);
        sendToAll("JOIN", player.getUniqueId(), player.getName(), null, player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        notifyLocal(player.getUniqueId(), player.getName(), "QUIT", null);
        sendToAll("QUIT", player.getUniqueId(), player.getName(), null, player);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (!SUB_CHANNEL.equals(subChannel)) return;

        String action     = in.readUTF();
        UUID   targetUUID = UUID.fromString(in.readUTF());
        String targetName = in.readUTF();
        String server     = action.equals("SWITCH") ? in.readUTF() : null;

        notifyLocal(targetUUID, targetName, action, server);
    }

    private void notifyLocal(UUID targetUUID, String targetName, String action, String server) {
        String msg = switch (action) {
            case "JOIN"   -> "&e" + targetName + " &aзашёл в сеть.";
            case "QUIT"   -> "&e" + targetName + " &cвышел из сети.";
            case "SWITCH" -> "&e" + targetName + " &7перешёл на сервер &b" + server;
            default       -> null;
        };
        if (msg != null)
            plugin.getFollowManager().notifyAllFollowers(targetUUID, msg);
    }

    private void sendToAll(String action, UUID uuid, String name, String server, Player via) {
        if (!via.isOnline()) return;

        ByteArrayDataOutput payload = ByteStreams.newDataOutput();
        payload.writeUTF(action);
        payload.writeUTF(uuid.toString());
        payload.writeUTF(name);
        if (server != null) payload.writeUTF(server);

        byte[] payloadBytes = payload.toByteArray();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(SUB_CHANNEL);
        out.writeShort(payloadBytes.length);
        out.write(payloadBytes);

        via.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }
}
