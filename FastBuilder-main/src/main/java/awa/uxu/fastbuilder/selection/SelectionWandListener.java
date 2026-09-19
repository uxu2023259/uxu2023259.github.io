package awa.uxu.fastbuilder.selection;

import awa.uxu.fastbuilder.FastBuilderPro;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SelectionWandListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseArrowWand(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() != Material.ARROW) {
            return;
        }

        if (!player.hasPermission("fb.pos") && !player.hasPermission("fb.*")) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_BLOCK) {
            FastBuilderPro.get().getSelectionManager().setPos1At(player, clicked.getLocation());
            event.setCancelled(true);
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            FastBuilderPro.get().getSelectionManager().setPos2At(player, clicked.getLocation());
            event.setCancelled(true);
        }
    }
}
