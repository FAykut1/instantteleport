package com.faykut.instantteleport;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateTeleportSlotPayload(int slot, Action action, String name) implements CustomPacketPayload {
    public static final Type<UpdateTeleportSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InstantTeleport.MODID, "update_teleport_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTeleportSlotPayload> STREAM_CODEC =
            CustomPacketPayload.codec(UpdateTeleportSlotPayload::write, UpdateTeleportSlotPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateTeleportSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TeleportDeviceItem)) return;

        String status = "Ready";
        if (payload.action == Action.SAVE_CURRENT) {
            TeleportDeviceItem.saveLocation(stack, payload.slot, player);
            TeleportDeviceItem.renameLocation(stack, payload.slot, payload.name);
            status = "Saved current position to slot " + (payload.slot + 1);
        } else if (payload.action == Action.RENAME) {
            TeleportDeviceItem.renameLocation(stack, payload.slot, payload.name);
            status = "Renamed slot " + (payload.slot + 1);
        } else if (payload.action == Action.REMOVE) {
            TeleportDeviceItem.removeLocation(stack, payload.slot);
            status = "Removed slot " + (payload.slot + 1);
        }

        PacketDistributor.sendToPlayer(player, OpenTeleportDeviceScreenPayload.from(stack, status));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(slot);
        buffer.writeVarInt(action.ordinal());
        buffer.writeUtf(name);
    }

    private static UpdateTeleportSlotPayload read(RegistryFriendlyByteBuf buffer) {
        int slot = buffer.readVarInt();
        Action action = Action.values()[buffer.readVarInt()];
        return new UpdateTeleportSlotPayload(slot, action, buffer.readUtf());
    }

    public enum Action {
        SAVE_CURRENT,
        RENAME,
        REMOVE
    }
}
