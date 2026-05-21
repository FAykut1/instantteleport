package com.faykut.instantteleport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class TeleportDeviceScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 17;
    private static final int PANEL_WIDTH = 330;
    private static final int HEADER_HEIGHT = 48;

    private OpenTeleportDeviceScreenPayload payload;
    private final List<EditBox> nameBoxes = new ArrayList<>();

    public TeleportDeviceScreen(OpenTeleportDeviceScreenPayload payload) {
        super(Component.literal("Teleport Device"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        rebuildTeleportWidgets();
    }

    public void update(OpenTeleportDeviceScreenPayload payload) {
        this.payload = payload;
        rebuildTeleportWidgets();
    }

    private void rebuildTeleportWidgets() {
        clearWidgets();
        nameBoxes.clear();

        int left = (width - PANEL_WIDTH) / 2;
        int top = panelTop() + HEADER_HEIGHT;
        int nameWidth = 112;
        int buttonX = left + PANEL_WIDTH - 124;

        for (OpenTeleportDeviceScreenPayload.SlotInfo slot : payload.slots()) {
            int y = top + slot.slot() * ROW_HEIGHT;
            EditBox nameBox = new EditBox(font, left + 66, y, nameWidth, BUTTON_HEIGHT, Component.literal("Slot name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(slot.name().isBlank() ? "Slot " + (slot.slot() + 1) : slot.name());
            addRenderableWidget(nameBox);
            nameBoxes.add(nameBox);

            addRenderableWidget(Button.builder(Component.literal("Set"), button ->
                    send(slot.slot(), UpdateTeleportSlotPayload.Action.SAVE_CURRENT, nameBox.getValue()))
                    .bounds(buttonX, y, 35, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Name"), button ->
                    send(slot.slot(), UpdateTeleportSlotPayload.Action.RENAME, nameBox.getValue()))
                    .bounds(buttonX + 39, y, 43, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("X"), button ->
                    send(slot.slot(), UpdateTeleportSlotPayload.Action.REMOVE, ""))
                    .bounds(buttonX + 86, y, 24, BUTTON_HEIGHT)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 66, panelTop() + panelHeight() - 25, 54, BUTTON_HEIGHT)
                .build());
    }

    private static void send(int slot, UpdateTeleportSlotPayload.Action action, String name) {
        ClientPacketDistributor.sendToServer(new UpdateTeleportSlotPayload(slot, action, name));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelHeight = panelHeight();
        int left = (width - PANEL_WIDTH) / 2;
        int top = panelTop();
        graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight, 0xDD0C1218);
        graphics.outline(left, top, PANEL_WIDTH, panelHeight, 0xFF2A95B8);
        graphics.fill(left + 7, top + 7, left + PANEL_WIDTH - 7, top + 38, 0xEE1A3342);
        graphics.centeredText(font, Component.literal("Teleportation Device"), width / 2, top + 10, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal(payload.status()), width / 2, top + 24, 0xFF9FD8EC);

        int rowLeft = left + 10;
        for (OpenTeleportDeviceScreenPayload.SlotInfo slot : payload.slots()) {
            int y = top + HEADER_HEIGHT + slot.slot() * ROW_HEIGHT;
            int rowColor = slot.saved() ? 0x2924C6A8 : 0x18101820;
            int dotColor = slot.saved() ? 0xFF35FF9A : 0xFF5F6670;
            graphics.fill(left + 7, y - 1, left + PANEL_WIDTH - 7, y + BUTTON_HEIGHT + 1, rowColor);
            graphics.fill(left + 14, y + 6, left + 19, y + 11, dotColor);
            graphics.text(font, String.valueOf(slot.slot() + 1), rowLeft + 14, y + 5, 0xDDEEFF);
            graphics.text(font, slot.saved() ? "Saved" : "Empty", rowLeft + 32, y + 5, slot.saved() ? 0x66FFAA : 0x8A9099);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, Component.literal("Teleportation Device"), width / 2, top + 10, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal(payload.status()), width / 2, top + 24, 0xFF9FD8EC);
    }

    private int panelHeight() {
        return HEADER_HEIGHT + TeleportDeviceItem.MAX_LOCATIONS * ROW_HEIGHT + 32;
    }

    private int panelTop() {
        return Math.max(6, (height - panelHeight()) / 2);
    }
}
