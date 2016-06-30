package net.blay09.mods.refinedrelocation.filter;

import net.blay09.mods.refinedrelocation.RefinedRelocation;
import net.blay09.mods.refinedrelocation.api.client.IFilterIcon;
import net.blay09.mods.refinedrelocation.api.filter.IFilter;
import net.blay09.mods.refinedrelocation.api.grid.ISortingInventory;
import net.blay09.mods.refinedrelocation.capability.CapabilitySortingInventory;
import net.blay09.mods.refinedrelocation.client.ClientProxy;
import net.blay09.mods.refinedrelocation.util.TileWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

public class SameItemFilter implements IFilter {

	public static final String ID = RefinedRelocation.MOD_ID + ":SameItemFilter";

	private boolean ignoreMetadata = false;
	private boolean ignoreNBT = true;

	@Override
	public String getIdentifier() {
		return ID;
	}

	@Override
	public boolean isFilterUsable(TileWrapper pos) {
		return pos.hasCapability(CapabilitySortingInventory.CAPABILITY, null);
	}

	@Override
	public boolean passes(TileWrapper pos, ItemStack itemStack) {
		ISortingInventory sortingInventory = pos.getCapability(CapabilitySortingInventory.CAPABILITY, null);
		if(sortingInventory != null) {
			IItemHandler itemHandler = sortingInventory.getItemHandler();
			for(int i = 0; i < itemHandler.getSlots(); i++) {
				ItemStack otherStack = itemHandler.getStackInSlot(i);
				if(otherStack != null) {
					if(itemStack.getItem() != otherStack.getItem()) {
						continue;
					}
					if(!ignoreMetadata && itemStack.getItemDamage() != otherStack.getItemDamage()) {
						continue;
					}
					if(!ignoreNBT && !ItemStack.areItemStackTagsEqual(itemStack, otherStack)) {
						continue;
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public NBTBase serializeNBT() {
		NBTTagCompound compound = new NBTTagCompound();
		compound.setBoolean("IgnoreMetadata", ignoreMetadata);
		compound.setBoolean("IgnoreNBT", ignoreNBT);
		return compound;
	}

	@Override
	public void deserializeNBT(NBTBase nbt) {
		NBTTagCompound compound = (NBTTagCompound) nbt;
		ignoreMetadata = compound.getBoolean("IgnoreMetadata");
		ignoreNBT = compound.getBoolean("IgnoreNBT");
	}


	@Override
	public String getLangKey() {
		return "filter.refinedrelocation:SameItemFilter";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IFilterIcon getFilterIcon() {
		return ClientProxy.TEXTURE_ATLAS.getSprite("refinedrelocation:icon_PresetFilter");
	}
}