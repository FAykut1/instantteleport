package com.faykut.instantteleport;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = InstantTeleport.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = InstantTeleport.MODID, value = Dist.CLIENT)
public class InstantTeleportClient {
    private static final String TELEPORT_CATEGORY = "key.categories.instantteleport.teleport";
    private static final KeyMapping[] TELEPORT_SLOT_KEYS = new KeyMapping[TeleportDeviceItem.MAX_LOCATIONS];
    private static final boolean[] TELEPORT_SLOT_KEY_STATES = new boolean[TeleportDeviceItem.MAX_LOCATIONS];

    static {
        for (int i = 0; i < TELEPORT_SLOT_KEYS.length; i++) {
            TELEPORT_SLOT_KEYS[i] = new KeyMapping(
                    "key.instantteleport.teleport_slot_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    KeyModifier.ALT,
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_1 + i,
                    TELEPORT_CATEGORY);
        }
    }

    public InstantTeleportClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(InstantTeleportClient::onClientTick);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        InstantTeleport.LOGGER.info("HELLO FROM CLIENT SETUP");
        InstantTeleport.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : TELEPORT_SLOT_KEYS) {
            event.register(keyMapping);
        }
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }

        long window = minecraft.getWindow().getWindow();
        boolean altDown = InputConstants.isKeyDown(window, InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RALT);
        for (int slot = 0; slot < TELEPORT_SLOT_KEYS.length; slot++) {
            boolean keyDown = altDown && InputConstants.isKeyDown(window, InputConstants.KEY_1 + slot);
            if (keyDown && !TELEPORT_SLOT_KEY_STATES[slot]) {
                PacketDistributor.sendToServer(new TeleportSlotPayload(slot));
            }
            TELEPORT_SLOT_KEY_STATES[slot] = keyDown;
        }
    }
}
