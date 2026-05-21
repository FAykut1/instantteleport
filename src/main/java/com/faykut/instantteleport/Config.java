package com.faykut.instantteleport;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TELEPORT_DEVICE_CAPACITY = BUILDER
            .comment("Maximum FE that a teleportation device can store")
            .defineInRange("teleportDeviceCapacity", TeleportDeviceItem.CAPACITY, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue TELEPORT_COST = BUILDER
            .comment("FE consumed by each teleport")
            .defineInRange("teleportCost", TeleportDeviceItem.TELEPORT_COST, 0, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
