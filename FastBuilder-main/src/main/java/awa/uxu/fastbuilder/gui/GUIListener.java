package awa.uxu.fastbuilder.gui;

import awa.uxu.fastbuilder.FastBuilderPro;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = e.getView().getTitle();

        if (!isPluginGui(title)) {
            return;
        }

        e.setCancelled(true);

        if (e.isShiftClick() || e.getClick() == ClickType.NUMBER_KEY || e.getCurrentItem() == null) {
            return;
        }

        Material type = e.getCurrentItem().getType();

        if (title.equals(MainMenuGUI.TITLE)) {
            handleMainMenuClick(player, type);
            return;
        }

        if (title.equals(StructureSelectGUI.TITLE_PREVIEW) || title.equals(StructureSelectGUI.TITLE_BUILD)) {
            handleStructureSelectClick(player, type, e.getCurrentItem().getItemMeta() == null ? "" : e.getCurrentItem().getItemMeta().getDisplayName());
            return;
        }

        if (title.equals(MaterialLackGUI.TITLE)) {
            var session = FastBuilderPro.get().getActiveBuilds().get(player.getUniqueId());

            if (session == null) {
                player.closeInventory();
                return;
            }

            if (type == Material.LIME_WOOL) {
                player.closeInventory();
                String cmd = session.getContinueMode() == awa.uxu.fastbuilder.build.BuildSession.ContinueMode.BUILD
                        ? "/fb build"
                        : "/fb paste";
                player.sendMessage("§e请使用 " + cmd + " 继续建造");
            }

            if (type == Material.RED_WOOL) {
                session.cancel();
                player.closeInventory();
            }
            return;
        }

        if (title.equals(ConfirmGUI.TITLE)) {
            if (type == Material.ARROW) {
                String name = e.getCurrentItem().getItemMeta().getDisplayName();
                int current = ConfirmGUI.getPage(player.getUniqueId());

                if (name.contains("下一页")) {
                    ConfirmGUI.setPage(player.getUniqueId(), current + 1);
                    ConfirmGUI.open(player);
                }
                if (name.contains("上一页")) {
                    ConfirmGUI.setPage(player.getUniqueId(), current - 1);
                    ConfirmGUI.open(player);
                }
                return;
            }

            if (type == Material.LIME_WOOL) {
                player.closeInventory();
                Integer rotation = FastBuilderPro.get().getPendingRotation(player.getUniqueId());
                if (rotation == null) {
                    rotation = 0;
                }
                FastBuilderPro.get().startBuild(player, rotation);
                return;
            }

            if (type == Material.RED_WOOL) {
                player.closeInventory();
                FastBuilderPro.get().clearPending(player);
            }
            return;
        }

        if (title.equals(ProgressGUI.TITLE) && type == Material.RED_WOOL) {
            var session = FastBuilderPro.get().getActiveBuilds().get(player.getUniqueId());
            if (session != null) {
                session.cancel();
            }
            player.closeInventory();
        }
    }

    private void handleMainMenuClick(Player player, Material type) {
        switch (type) {
            case ARROW -> {
                player.closeInventory();
                player.performCommand("fb pos1");
            }
            case TIPPED_ARROW -> {
                player.closeInventory();
                player.performCommand("fb pos2");
            }
            case CHEST -> {
                player.closeInventory();
                player.performCommand("fb copy");
            }
            case SLIME_BLOCK -> {
                player.closeInventory();
                player.performCommand("fb paste 0");
            }
            case BOOK -> StructureSelectGUI.open(player, StructureSelectGUI.Mode.PREVIEW, 0);
            case BRICKS -> StructureSelectGUI.open(player, StructureSelectGUI.Mode.BUILD, 0);
            case BARRIER -> {
                player.closeInventory();
                player.performCommand("fb undo");
            }
            case MAP -> ProgressGUI.open(player);
            case OAK_SIGN -> {
                player.closeInventory();
                SaveInputManager.openSignInput(player);
            }
            case CLOCK -> {
                player.closeInventory();
                player.sendMessage("§e快速建造请使用：§f/fb quick <结构> [角度]");
                player.sendMessage("§7示例：§f/fb quick house 90");
            }
            case LIME_WOOL -> {
                player.closeInventory();
                player.performCommand("fb confirm");
            }
            case RED_WOOL -> {
                player.closeInventory();
                player.performCommand("fb cancel");
            }
            default -> {
            }
        }
    }

    private void handleStructureSelectClick(Player player, Material type, String displayName) {
        if (type != Material.BOOK && type != Material.ARROW) {
            return;
        }

        if (type == Material.ARROW) {
            if (displayName.contains("返回主菜单")) {
                MainMenuGUI.open(player);
                return;
            }

            int current = StructureSelectGUI.getPage(player.getUniqueId());
            StructureSelectGUI.Mode mode = StructureSelectGUI.getMode(player.getUniqueId());
            if (displayName.contains("上一页")) {
                StructureSelectGUI.open(player, mode, current - 1);
            } else if (displayName.contains("下一页")) {
                StructureSelectGUI.open(player, mode, current + 1);
            }
            return;
        }

        String raw = displayName.replace("§e", "").trim();
        String name = raw.contains(" §8[") ? raw.substring(0, raw.indexOf(" §8[")).trim() : raw;
        StructureSelectGUI.Mode mode = StructureSelectGUI.getMode(player.getUniqueId());

        player.closeInventory();
        if (mode == StructureSelectGUI.Mode.PREVIEW) {
            player.performCommand("fb preview " + name + " 0");
        } else {
            player.performCommand("fb build " + name + " 0");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (isPluginGui(e.getView().getTitle())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        SaveInputManager.handleSignSubmit(event);
    }

    private boolean isPluginGui(String title) {
        return title.equals(ConfirmGUI.TITLE)
                || title.equals(ProgressGUI.TITLE)
                || title.equals(MaterialLackGUI.TITLE)
                || title.equals(MainMenuGUI.TITLE)
                || title.equals(StructureSelectGUI.TITLE_PREVIEW)
                || title.equals(StructureSelectGUI.TITLE_BUILD);
    }
}
