package net.cobblers.irt;

import net.minecraft.block.InventoryProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;

public class InventoryAccessor {

    public static List<ItemStack> getInventory(ClientPlayerEntity player) {
        return player.getInventory().main;
    }
}
