package com.faykut.instantteleport;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
            tag.putInt("Energy", maxEnergy());
            setTag(stack, tag);
        }
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            int slot = firstEmptySlot(stack).orElse(0);
            saveLocation(stack, slot, serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("Saved teleport location " + (slot + 1)).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.SUCCESS;
        }

        PacketDistributor.sendToPlayer(serverPlayer, OpenTeleportDeviceScreenPayload.from(stack));
        return InteractionResult.SUCCESS;
    }

    public static boolean teleport(ServerPlayer player, ItemStack stack, TeleportLocation location) {
        DeviceEnergyStorage energy = new DeviceEnergyStorage(stack);
        boolean alwaysCharged = isAlwaysCharged(stack);
        int teleportCost = teleportCost();
        if (!alwaysCharged && energy.getAmountAsInt() < teleportCost) {
            player.sendSystemMessage(Component.literal("Teleport device needs " + teleportCost + " FE.").withStyle(ChatFormatting.RED), true);
            return false;
        }

        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(location.dimension()));
        ServerLevel targetLevel = player.level().getServer().getLevel(dimension);
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Saved dimension is not available.").withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (!alwaysCharged) {
            try (Transaction transaction = Transaction.openRoot()) {
                energy.extract(teleportCost, transaction);
                transaction.commit();
            }
        }
        player.teleportTo(targetLevel, location.x(), location.y(), location.z(), Set.of(), location.yRot(), location.xRot(), false);
        player.sendSystemMessage(Component.literal("Teleported to slot " + (location.slot() + 1)).withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static void saveLocation(ItemStack stack, int slot, ServerPlayer player) {
        if (slot < 0 || slot >= MAX_LOCATIONS) return;
        CompoundTag tag = getTag(stack);
        CompoundTag locations = tag.getCompoundOrEmpty("Locations");
        CompoundTag location = new CompoundTag();
        location.putInt("Slot", slot);
        location.putString("Dimension", player.level().dimension().identifier().toString());
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
        CompoundTag locations = tag.getCompoundOrEmpty("Locations");
        if (!locations.contains(String.valueOf(slot))) return;
        CompoundTag location = locations.getCompoundOrEmpty(String.valueOf(slot));
        location.putString("Name", name.trim());
        locations.put(String.valueOf(slot), location);
        tag.put("Locations", locations);
        setTag(stack, tag);
    }

    public static void removeLocation(ItemStack stack, int slot) {
        if (slot < 0 || slot >= MAX_LOCATIONS) return;
        CompoundTag tag = getTag(stack);
        CompoundTag locations = tag.getCompoundOrEmpty("Locations");
        locations.remove(String.valueOf(slot));
        tag.put("Locations", locations);
        setTag(stack, tag);
    }

    public static String getLocationName(ItemStack stack, int slot) {
        CompoundTag locations = getTag(stack).getCompoundOrEmpty("Locations");
        if (!locations.contains(String.valueOf(slot))) return "";
        return locations.getCompoundOrEmpty(String.valueOf(slot)).getStringOr("Name", "");
    }

    public static Optional<TeleportLocation> getLocation(ItemStack stack, int slot) {
        CompoundTag locations = getTag(stack).getCompoundOrEmpty("Locations");
        if (!locations.contains(String.valueOf(slot))) return Optional.empty();
        CompoundTag location = locations.getCompoundOrEmpty(String.valueOf(slot));
        return Optional.of(new TeleportLocation(
                slot,
                location.getStringOr("Dimension", "minecraft:overworld"),
                location.getDoubleOr("X", 0),
                location.getDoubleOr("Y", 64),
                location.getDoubleOr("Z", 0),
                location.getFloatOr("YRot", 0),
                location.getFloatOr("XRot", 0)
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

    static int maxEnergy() {
        return Config.TELEPORT_DEVICE_CAPACITY.get();
    }

    static int teleportCost() {
        return Config.TELEPORT_COST.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        DeviceEnergyStorage energy = new DeviceEnergyStorage(stack);
        if (isAlwaysCharged(stack)) {
            tooltip.accept(Component.literal("Infinite FE").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.literal(energy.getAmountAsInt() + " / " + energy.getCapacityAsInt() + " FE").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.literal("Shift-right-click: save location").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Right-click: open teleport editor").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Alt+1-9: teleport to saved slot").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override public boolean isBarVisible(ItemStack stack) { return !isAlwaysCharged(stack); }
    @Override public int getBarWidth(ItemStack stack) { return isAlwaysCharged(stack) ? 13 : Math.round(13.0F * new DeviceEnergyStorage(stack).getAmountAsInt() / maxEnergy()); }
    @Override public int getBarColor(ItemStack stack) { return 0x35C6FF; }

    public record TeleportLocation(int slot, String dimension, double x, double y, double z, float yRot, float xRot) {}

    public static class DeviceEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final ItemStack stack;

        public DeviceEnergyStorage(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public long getAmountAsLong() {
            if (isAlwaysCharged(stack)) return maxEnergy();
            return Math.min(getTag(stack).getIntOr("Energy", 0), maxEnergy());
        }

        @Override
        public long getCapacityAsLong() {
            return maxEnergy();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
            if (isAlwaysCharged(stack)) return 0;
            int inserted = Math.min(maxEnergy() - getAmountAsInt(), amount);
            if (inserted > 0) {
                updateSnapshots(transaction);
                setEnergy(getAmountAsInt() + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
            if (isAlwaysCharged(stack)) return Math.min(maxEnergy(), amount);
            int extracted = Math.min(getAmountAsInt(), amount);
            if (extracted > 0) {
                updateSnapshots(transaction);
                setEnergy(getAmountAsInt() - extracted);
            }
            return extracted;
        }

        @Override
        protected Integer createSnapshot() {
            return getAmountAsInt();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            setEnergy(snapshot);
        }

        private void setEnergy(int energy) {
            CompoundTag tag = getTag(stack);
            tag.putInt("Energy", Math.max(0, Math.min(maxEnergy(), energy)));
            setTag(stack, tag);
        }
    }
}
