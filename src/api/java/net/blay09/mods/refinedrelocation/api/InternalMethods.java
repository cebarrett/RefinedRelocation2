package net.blay09.mods.refinedrelocation.api;

import net.blay09.mods.refinedrelocation.api.filter.IFilter;
import net.blay09.mods.refinedrelocation.api.grid.ISortingGridMember;
import net.blay09.mods.refinedrelocation.api.grid.ISortingInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public interface InternalMethods {

	void registerFilter(String id, Class<? extends IFilter> filterClass);

	void addToSortingGrid(ISortingGridMember member);
	void removeFromSortingGrid(ISortingGridMember member);

	void insertIntoSortingGrid(ISortingInventory sortingInventory, int fromSlotIndex, ItemStack itemStack);

	GuiButton createOpenFilterButton(GuiContainer guiContainer, TileEntity tileEntity, int buttonId);

	void sendContainerMessageToServer(String key, String value);
	void sendContainerMessageToServer(String key, int value);

	void syncContainerValue(String key, String value, Iterable<IContainerListener> listeners);
	void syncContainerValue(String key, NBTTagCompound value, Iterable<IContainerListener> listeners);

	void registerGuiHandler(Class<? extends TileEntity> tileClass, ITileGuiHandler handler);

}