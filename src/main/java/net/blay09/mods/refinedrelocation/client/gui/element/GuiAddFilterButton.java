package net.blay09.mods.refinedrelocation.client.gui.element;

import net.blay09.mods.refinedrelocation.api.RefinedRelocationAPI;
import net.blay09.mods.refinedrelocation.api.client.IDrawable;
import net.blay09.mods.refinedrelocation.api.filter.IFilter;
import net.blay09.mods.refinedrelocation.client.gui.GuiAddFilter;
import net.blay09.mods.refinedrelocation.client.gui.GuiTextures;
import net.blay09.mods.refinedrelocation.client.gui.base.ITooltipElement;
import net.blay09.mods.refinedrelocation.client.gui.base.element.GuiImageButton;
import net.blay09.mods.refinedrelocation.container.ContainerRootFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class GuiAddFilterButton extends GuiImageButton implements ITooltipElement {

    private final GuiAddFilter parentGui;
    private IFilter currentFilter;

    public GuiAddFilterButton(int buttonId, int x, int y, GuiAddFilter parentGui) {
        super(buttonId, x, y, 151, 27, GuiTextures.BUTTON_NONE);
        this.parentGui = parentGui;
    }

    @Override
    public void onClick(double p_194829_1_, double p_194829_3_) {
        if (currentFilter != null) {
            RefinedRelocationAPI.sendContainerMessageToServer(ContainerRootFilter.KEY_ADD_FILTER, currentFilter.getIdentifier());
            if (!currentFilter.hasConfiguration()) {
                Minecraft.getInstance().displayGuiScreen(parentGui.getParentGui());
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);

        if (hovered) {
            drawRect(x, y, x + width, y + height, 0xAAFFFFFF);
        }

        if (currentFilter != null) {
            IDrawable filterIcon = currentFilter.getFilterIcon();
            if (filterIcon != null) {
                GlStateManager.color4f(1f, 1f, 1f, 1f);
                filterIcon.draw(x + 2, y + height / 2f - 12, 24, 24, zLevel);
            }

            FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
            drawString(fontRenderer, I18n.format(currentFilter.getLangKey()), x + 32, y + height / 2 - fontRenderer.FONT_HEIGHT / 2, 0xFFFFFF);
        }
    }

    public void setCurrentFilter(@Nullable IFilter currentFilter) {
        this.currentFilter = currentFilter;
    }

    @Override
    public void addTooltip(List<String> tooltip) {
        if (currentFilter != null) {
            Collections.addAll(tooltip, I18n.format(currentFilter.getDescriptionLangKey()).split("\\\\n"));
        }
    }

}
