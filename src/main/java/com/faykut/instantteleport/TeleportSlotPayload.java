package com.faykut.instantteleport;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TeleportSlotPayload(int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportSlotPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(InstantTeleport.MODID, "teleport_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportSlotPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(TeleportSlotPayload::new, TeleportSlotPayload::slot).mapStream(buffer -> buffer);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TeleportSlotPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            teleportFromHeldDevice(player, payload.slot());
        }
    }

    private static void teleportFromHeldDevice(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= TeleportDeviceItem.MAX_LOCATIONS) {
            return;
        }

        Optional<ItemStack> stack = CuriosCompat.findTeleportDevice(player)
                .or(() -> findHeldTeleportDevice(player));
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("Equip or hold a teleport device to use slot shortcuts.")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            return;
        }

        ItemStack deviceStack = stack.get();
        TeleportDeviceItem.getLocation(deviceStack, slot).ifPresentOrElse(
                location -> TeleportDeviceItem.teleport(player, deviceStack, location),
                () -> player.sendSystemMessage(
                        Component.literal("No location saved in slot " + (slot + 1) + ".")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW),
                        true));
    }

    private static Optional<ItemStack> findHeldTeleportDevice(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof TeleportDeviceItem) {
            return Optional.of(mainHand);
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof TeleportDeviceItem) {
            return Optional.of(offhand);
        }

        return Optional.empty();
    }
}
