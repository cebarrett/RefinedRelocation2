package net.blay09.mods.refinedrelocation.client.gui.element;

import net.blay09.mods.refinedrelocation.client.gui.base.ITooltipElement;
import net.blay09.mods.refinedrelocation.client.gui.base.element.SizableButton;
import net.minecraft.client.gui.widget.button.Button;

public abstract class GuiTooltipButton extends SizableButton implements ITooltipElement {
    public GuiTooltipButton(int x, int y, int width, int height, String displayString, IPressable pressable) {
        super(x, y, width, height, displayString, pressable);
    }
}
