package com.faykut.instantteleport;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(InstantTeleport.MODID)
public class InstantTeleport {
    public static final String MODID = "instantteleport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<Item> TELEPORT_DEVICE = ITEMS.registerItem("teleport_device", TeleportDeviceItem::new);
    public static final DeferredItem<Item> ADMIN_TELEPORT_DEVICE = ITEMS.registerItem("admin_teleport_device", properties -> new TeleportDeviceItem(properties, true));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TELEPORT_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.instantteleport"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> TELEPORT_DEVICE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(TELEPORT_DEVICE.get());
                output.accept(ADMIN_TELEPORT_DEVICE.get());
            })
            .build());

    public InstantTeleport(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloadHandlers);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new TeleportDeviceItem.DeviceEnergyStorage(stack),
                TELEPORT_DEVICE.get(), ADMIN_TELEPORT_DEVICE.get());
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(OpenTeleportDeviceScreenPayload.TYPE, OpenTeleportDeviceScreenPayload.STREAM_CODEC, OpenTeleportDeviceScreenPayload::handle)
                .playToServer(TeleportSlotPayload.TYPE, TeleportSlotPayload.STREAM_CODEC, TeleportSlotPayload::handle)
                .playToServer(UpdateTeleportSlotPayload.TYPE, UpdateTeleportSlotPayload.STREAM_CODEC, UpdateTeleportSlotPayload::handle);
    }
}
