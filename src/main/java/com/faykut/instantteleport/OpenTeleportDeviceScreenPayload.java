package com.faykut.instantteleport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record OpenTeleportDeviceScreenPayload(List<SlotInfo> slots, String status) implements CustomPacketPayload {
    public static final Type<OpenTeleportDeviceScreenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(InstantTeleport.MODID, "open_teleport_device"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTeleportDeviceScreenPayload> STREAM_CODEC =
            CustomPacketPayload.codec(OpenTeleportDeviceScreenPayload::write, OpenTeleportDeviceScreenPayload::read);

    public static OpenTeleportDeviceScreenPayload from(ItemStack stack) {
        List<SlotInfo> slots = new ArrayList<>();
        for (int slot = 0; slot < TeleportDeviceItem.MAX_LOCATIONS; slot++) {
            var location = TeleportDeviceItem.getLocation(stack, slot);
            String name = TeleportDeviceItem.getLocationName(stack, slot);
            String details = location
                    .map(value -> value.dimension() + " " + (int) value.x() + ", " + (int) value.y() + ", " + (int) value.z())
                    .orElse("Empty");
            slots.add(new SlotInfo(slot, location.isPresent(), name, details));
        }
        return new OpenTeleportDeviceScreenPayload(slots, "Ready");
    }

    public static OpenTeleportDeviceScreenPayload from(ItemStack stack, String status) {
        return new OpenTeleportDeviceScreenPayload(from(stack).slots(), status);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        for (SlotInfo slot : slots) {
            buffer.writeBoolean(slot.saved());
            buffer.writeUtf(slot.name());
            buffer.writeUtf(slot.details());
        }
        buffer.writeUtf(status);
    }

    private static OpenTeleportDeviceScreenPayload read(RegistryFriendlyByteBuf buffer) {
        List<SlotInfo> slots = new ArrayList<>();
        for (int slot = 0; slot < TeleportDeviceItem.MAX_LOCATIONS; slot++) {
            slots.add(new SlotInfo(slot, buffer.readBoolean(), buffer.readUtf(), buffer.readUtf()));
        }
        return new OpenTeleportDeviceScreenPayload(slots, buffer.readUtf());
    }

    public record SlotInfo(int slot, boolean saved, String name, String details) {}
}
