package com.faykut.instantteleport;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateTeleportSlotPayload(int slot, Action action, String name) implements CustomPacketPayload {
    public static final Type<UpdateTeleportSlotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(InstantTeleport.MODID, "update_teleport_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTeleportSlotPayload> STREAM_CODEC =
            CustomPacketPayload.codec(UpdateTeleportSlotPayload::write, UpdateTeleportSlotPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateTeleportSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (payload.action == Action.INVALID || payload.slot < 0 || payload.slot >= TeleportDeviceItem.MAX_LOCATIONS) return;

        Optional<ItemStack> stack = findEditableTeleportDevice(player);
        if (stack.isEmpty()) return;

        String status = "Ready";
        if (payload.action == Action.SAVE_CURRENT) {
            TeleportDeviceItem.saveLocation(stack.get(), payload.slot, player);
            TeleportDeviceItem.renameLocation(stack.get(), payload.slot, payload.name);
            status = "Saved current position to slot " + (payload.slot + 1);
        } else if (payload.action == Action.RENAME) {
            TeleportDeviceItem.renameLocation(stack.get(), payload.slot, payload.name);
            status = "Renamed slot " + (payload.slot + 1);
        } else if (payload.action == Action.REMOVE) {
            TeleportDeviceItem.removeLocation(stack.get(), payload.slot);
            status = "Removed slot " + (payload.slot + 1);
        }

        PacketDistributor.sendToPlayer(player, OpenTeleportDeviceScreenPayload.from(stack.get(), status));
    }

    private static Optional<ItemStack> findEditableTeleportDevice(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof TeleportDeviceItem) {
            return Optional.of(mainHand);
        }

        return CuriosCompat.findTeleportDevice(player);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(slot);
        buffer.writeVarInt(action.ordinal());
        buffer.writeUtf(name);
    }

    private static UpdateTeleportSlotPayload read(RegistryFriendlyByteBuf buffer) {
        int slot = buffer.readVarInt();
        int actionIndex = buffer.readVarInt();
        Action[] actions = Action.values();
        Action action = actionIndex >= 0 && actionIndex < actions.length ? actions[actionIndex] : Action.INVALID;
        return new UpdateTeleportSlotPayload(slot, action, buffer.readUtf());
    }

    public enum Action {
        SAVE_CURRENT,
        RENAME,
        REMOVE,
        INVALID
    }
}
