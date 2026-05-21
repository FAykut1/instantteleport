package com.faykut.instantteleport;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;

public class TeleportDeviceItem extends Item {
    public static final int MAX_LOCATIONS = 9;
    public static final int CAPACITY = 100_000;
    public static final int TELEPORT_COST = 10_000;

    private final boolean alwaysCharged;

    public TeleportDeviceItem(Properties properties) {
        this(properties, false);
    }

    public TeleportDeviceItem(Properties properties, boolean alwaysCharged) {
        super(properties.stacksTo(1));
        this.alwaysCharged = alwaysCharged;
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (alwaysCharged) {
            CompoundTag tag = getTag(stack);
            tag.putInt("Energy", CAPACITY);
            setTag(stack, tag);
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());

        if (player.isShiftKeyDown()) {
            int slot = firstEmptySlot(stack).orElse(0);
            saveLocation(stack, slot, serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("Saved teleport location " + (slot + 1)).withStyle(ChatFormatting.GREEN), true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        PacketDistributor.sendToPlayer(serverPlayer, OpenTeleportDeviceScreenPayload.from(stack));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static boolean teleport(ServerPlayer player, ItemStack stack, TeleportLocation location) {
        DeviceEnergyStorage energy = new DeviceEnergyStorage(stack);
        boolean alwaysCharged = isAlwaysCharged(stack);
        if (!alwaysCharged && energy.getEnergyStored() < TELEPORT_COST) {
            player.sendSystemMessage(Component.literal("Teleport device needs " + TELEPORT_COST + " FE.").withStyle(ChatFormatting.RED), true);
            return false;
        }

        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(location.dimension()));
        ServerLevel targetLevel = player.level().getServer().getLevel(dimension);
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Saved dimension is not available.").withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (!alwaysCharged) {
            energy.extractEnergy(TELEPORT_COST, false);
        }
        player.teleportTo(targetLevel, location.x(), location.y(), location.z(), location.yRot(), location.xRot());
        player.sendSystemMessage(Component.literal("Teleported to slot " + (location.slot() + 1)).withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static void saveLocation(ItemStack stack, int slot, ServerPlayer player) {
        if (slot < 0 || slot >= MAX_LOCATIONS) return;
        CompoundTag tag = getTag(stack);
        CompoundTag locations = tag.getCompound("Locations");
        CompoundTag location = new CompoundTag();
        location.putInt("Slot", slot);
        location.putString("Dimension", player.level().dimension().location().toString());
        location.putDouble("X", player.getX());
        location.putDouble("Y", player.getY());
        location.putDouble("Z", player.getZ());
        location.putFloat("YRot", player.getYRot());
        location.putFloat("XRot", player.getXRot());
        locations.put(String.valueOf(slot), location);
        tag.put("Locations", locations);
        setTag(stack, tag);
    }

    public static void renameLocation(ItemStack stack, int slot, String name) {
        if (slot < 0 || slot >= MAX_LOCATIONS) return;
        CompoundTag tag = getTag(stack);
        CompoundTag locations = tag.getCompound("Locations");
        if (!locations.contains(String.valueOf(slot))) return;
        CompoundTag location = locations.getCompound(String.valueOf(slot));
        location.putString("Name", name.trim());
        locations.put(String.valueOf(slot), location);
        tag.put("Locations", locations);
        setTag(stack, tag);
    }

    public static void removeLocation(ItemStack stack, int slot) {
        if (slot < 0 || slot >= MAX_LOCATIONS) return;
        CompoundTag tag = getTag(stack);
        CompoundTag locations = tag.getCompound("Locations");
        locations.remove(String.valueOf(slot));
        tag.put("Locations", locations);
        setTag(stack, tag);
    }

    public static String getLocationName(ItemStack stack, int slot) {
        CompoundTag locations = getTag(stack).getCompound("Locations");
        if (!locations.contains(String.valueOf(slot))) return "";
        return locations.getCompound(String.valueOf(slot)).getString("Name");
    }

    public static Optional<TeleportLocation> getLocation(ItemStack stack, int slot) {
        CompoundTag locations = getTag(stack).getCompound("Locations");
        if (!locations.contains(String.valueOf(slot))) return Optional.empty();
        CompoundTag location = locations.getCompound(String.valueOf(slot));
        return Optional.of(new TeleportLocation(
                slot,
                location.contains("Dimension") ? location.getString("Dimension") : "minecraft:overworld",
                location.contains("X") ? location.getDouble("X") : 0,
                location.contains("Y") ? location.getDouble("Y") : 64,
                location.contains("Z") ? location.getDouble("Z") : 0,
                location.contains("YRot") ? location.getFloat("YRot") : 0,
                location.contains("XRot") ? location.getFloat("XRot") : 0
        ));
    }

    private static Optional<Integer> firstEmptySlot(ItemStack stack) {
        for (int i = 0; i < MAX_LOCATIONS; i++) {
            if (getLocation(stack, i).isEmpty()) return Optional.of(i);
        }
        return Optional.empty();
    }

    static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    static boolean isAlwaysCharged(ItemStack stack) {
        return stack.getItem() instanceof TeleportDeviceItem item && item.alwaysCharged;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        DeviceEnergyStorage energy = new DeviceEnergyStorage(stack);
        if (isAlwaysCharged(stack)) {
            tooltip.add(Component.literal("Infinite FE").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(energy.getEnergyStored() + " / " + energy.getMaxEnergyStored() + " FE").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.literal("Shift-right-click: save location").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Right-click: open editor").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isBarVisible(ItemStack stack) { return !isAlwaysCharged(stack); }
    @Override public int getBarWidth(ItemStack stack) { return isAlwaysCharged(stack) ? 13 : Math.round(13.0F * new DeviceEnergyStorage(stack).getEnergyStored() / CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return 0x35C6FF; }

    public record TeleportLocation(int slot, String dimension, double x, double y, double z, float yRot, float xRot) {}

    public static class DeviceEnergyStorage implements IEnergyStorage {
        private final ItemStack stack;

        public DeviceEnergyStorage(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public int getEnergyStored() {
            if (isAlwaysCharged(stack)) return CAPACITY;
            return getTag(stack).getInt("Energy");
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (amount < 0) return 0;
            if (isAlwaysCharged(stack)) return 0;
            int inserted = Math.min(CAPACITY - getEnergyStored(), amount);
            if (inserted > 0 && !simulate) {
                setEnergy(getEnergyStored() + inserted);
            }
            return inserted;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (amount < 0) return 0;
            if (isAlwaysCharged(stack)) return Math.min(CAPACITY, amount);
            int extracted = Math.min(getEnergyStored(), amount);
            if (extracted > 0 && !simulate) {
                setEnergy(getEnergyStored() - extracted);
            }
            return extracted;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return !isAlwaysCharged(stack);
        }

        private void setEnergy(int energy) {
            CompoundTag tag = getTag(stack);
            tag.putInt("Energy", Math.max(0, Math.min(CAPACITY, energy)));
            setTag(stack, tag);
        }
    }
}
