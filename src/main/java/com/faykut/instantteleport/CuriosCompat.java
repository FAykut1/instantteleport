package com.faykut.instantteleport;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;

public final class CuriosCompat {
    private static final EntityCapability<IItemHandler, Void> CURIOS_INVENTORY =
            EntityCapability.createVoid(ResourceLocation.fromNamespaceAndPath("curios", "item_handler"), IItemHandler.class);

    private CuriosCompat() {}

    public static Optional<ItemStack> findTeleportDevice(ServerPlayer player) {
        IItemHandler inventory = player.getCapability(CURIOS_INVENTORY);
        if (inventory == null) {
            return Optional.empty();
        }

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof TeleportDeviceItem) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }
}
