package follow.testPracticFollow.menu;

import follow.testPracticFollow.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Menu extends org.mineacademy.fo.menu.Menu {

    private final Manager manager;
    private final List<UUID> targets;

    public Menu(Player viewer, Manager manager) {
        super(null);
        this.manager = manager;
        this.targets = new ArrayList<>(manager.getFollowing(viewer.getUniqueId()));

        int size = Math.max(9, (int) Math.ceil(Math.max(targets.size(), 1) / 9.0) * 9);
        setSize(Math.min(size, 54));
        setTitle("&6Слежка &7(" + targets.size() + ")");
    }

    @Override
    public ItemStack getItemAt(int slot) {
        if (slot >= targets.size())
            return NO_ITEM;

        UUID targetUUID = targets.get(slot);
        String name = manager.getPlayerName(targetUUID);
        Player online = Bukkit.getPlayer(targetUUID);
        String status = online != null ? "&aОнлайн" : "&cОффлайн";

        ItemStack skull = CompMaterial.PLAYER_HEAD.toItem();
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            if (online != null) {
                skullMeta.setOwningPlayer(online);
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                skullMeta.setOwningPlayer(op);
            }
            skull.setItemMeta(skullMeta);
        }

        return ItemCreator.fromItemStack(skull)
                .name("&e" + name)
                .lore(status, "", "&cЛКМ &7— отписаться")
                .hideTags(true)
                .make();
    }

    @Override
    protected void onMenuClick(Player player, int slot, InventoryAction action,
                               ClickType click, ItemStack cursor, ItemStack clicked, boolean cancelled) {
        if (slot >= targets.size()) return;

        UUID targetUUID = targets.get(slot);
        String name = manager.getPlayerName(targetUUID);

        manager.unfollow(player.getUniqueId(), targetUUID);
        player.sendMessage(("&cВы прекратили слежку за &e" + name + "&c.").replace("&", "§"));

        player.closeInventory();
        new Menu(player, manager).displayTo(player);
    }

    protected String getInfoButtonTitle() {
        return "&6Информация";
    }

    @Override
    protected String[] getInfo() {
        List<String> lines = new ArrayList<>();
        lines.add("&7Список игроков, за которыми вы следите.");
        lines.add("");

        if (targets.isEmpty()) {
            lines.add("&cСписок пуст.");
        } else {
            lines.add("&eОтслеживаемые игроки:");
            for (UUID uuid : targets) {
                String name = manager.getPlayerName(uuid);
                Player online = Bukkit.getPlayer(uuid);
                String status = online != null ? "&a●" : "&c●";
                lines.add(" " + status + " &f" + name);
            }
        }

        lines.add("");
        lines.add("&cЛКМ&8-&cПКМ &7 — перестать следить.");
        return lines.toArray(new String[0]);
    }
}
