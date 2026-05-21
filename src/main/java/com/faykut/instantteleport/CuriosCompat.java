package com.faykut.instantteleport;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

public final class CuriosCompat {
    private static final EntityCapability<ResourceHandler<ItemResource>, Void> CURIOS_INVENTORY =
            EntityCapability.createVoid(Identifier.fromNamespaceAndPath("curios", "item_handler"), ResourceHandler.asClass());

    private CuriosCompat() {}

    public static Optional<ItemStack> findTeleportDevice(ServerPlayer player) {
        ResourceHandler<ItemResource> inventory = player.getCapability(CURIOS_INVENTORY);
        if (inventory == null) {
            return Optional.empty();
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = ItemUtil.getStack(inventory, slot);
            if (stack.getItem() instanceof TeleportDeviceItem) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }
}
