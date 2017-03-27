package net.blay09.mods.refinedrelocation.tile;

import net.blay09.mods.refinedrelocation.ModItems;
import net.blay09.mods.refinedrelocation.util.RelativeSide;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileBlockExtender extends TileMod implements ITickable {

	private class ItemHandlerWrapper implements IItemHandler {
		private final TileEntity tileEntity;
		private final EnumFacing facing;
		private IItemHandler baseHandler;

		public ItemHandlerWrapper(TileEntity tileEntity, @Nullable EnumFacing facing, IItemHandler baseHandler) {
			this.tileEntity = tileEntity;
			this.facing = facing;
			this.baseHandler = baseHandler;
		}

		public boolean revalidate() {
			IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
			if (itemHandler == null) {
				return false;
			}
			baseHandler = itemHandler;
			return true;
		}

		@Override
		public int getSlots() {
			return baseHandler.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return baseHandler.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			if (hasStackLimiter) {
				int space = stackLimiterLimit - getStackInSlot(slot).getCount();
				if (space <= 0) {
					return stack;
				}
				int amount = Math.min(stack.getCount(), space);
				if (amount < stack.getCount()) {
					ItemStack insertStack = ItemHandlerHelper.copyStackWithSize(stack, amount);
					ItemStack restStack = baseHandler.insertItem(slot, insertStack, simulate);
					int initialRest = stack.getCount() - amount;
					if (initialRest > 0) {
						ItemStack otherRestStack = ItemHandlerHelper.copyStackWithSize(stack, initialRest);
						if(restStack.isEmpty()) {
							return otherRestStack;
						}
						if (ItemHandlerHelper.canItemStacksStack(stack, restStack)) {
							restStack.grow(initialRest);
						} else if (!world.isRemote) {
							// If the remainder item is different than the input item upon failed insertion that's most likely a bug or bad game mechanic, so drop the other rest item in the world rather than having it disappear.
							world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, otherRestStack));
						}
					}
					return restStack;
				}
			}
			return baseHandler.insertItem(slot, stack, simulate);
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return baseHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return baseHandler.getSlots();
		}
	}

	private final ItemStackHandler itemHandlerUpgrades = new ItemStackHandler(3) {
		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			if(stack.getItem() != ModItems.stackLimiter) {
				return stack;
			}
			for(int i = 0; i < getSlots(); i++) {
				if(getStackInSlot(i).getItem() == stack.getItem()) {
					return stack;
				}
			}
			return super.insertItem(slot, stack, simulate);
		}

		@Override
		protected void onContentsChanged(int slot) {
			updateUpgrades();
			markDirty();
		}
	};

	private final EnumFacing[] sideMappings = new EnumFacing[5];
	private int stackLimiterLimit = 64;

	private TileEntity cachedConnectedTile;
	private boolean hasStackLimiter;
	private final ItemHandlerWrapper[] cachedItemHandlers = new ItemHandlerWrapper[6];
	private final EnumFacing[] cachedFacingToFacingMappings = new EnumFacing[6];

	@Nullable
	public EnumFacing getSideMapping(RelativeSide side) {
		return sideMappings[side.ordinal()];
	}

	public void setSideMapping(RelativeSide side, @Nullable EnumFacing facing) {
		sideMappings[side.ordinal()] = facing;
		cachedFacingToFacingMappings[side.toFacing(getFacing()).ordinal()] = facing;
		markDirty();
	}

	public boolean hasVisibleConnection(EnumFacing side) {
		if (side == getFacing()) {
			return false;
		}
		BlockPos sidePos = pos.offset(side);
		TileEntity tileEntity = world.getTileEntity(sidePos);
		return tileEntity != null && isCompatibleTile(tileEntity, side.getOpposite());
	}

	private boolean isCompatibleTile(TileEntity tileEntity, EnumFacing side) {
		return tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
	}

	@Override
	protected void onFirstTick() {
		cachedConnectedTile = world.getTileEntity(pos.offset(getFacing()));
		for(int i = 0; i < sideMappings.length; i++) {
			cachedFacingToFacingMappings[RelativeSide.fromIndex(i).toFacing(getFacing()).ordinal()] = sideMappings[i];
		}
		updateUpgrades();
	}

	@Nullable
	public EnumFacing getSideMapping(@Nullable EnumFacing facing) {
		if (facing == null) {
			return getFacing().getOpposite();
		}
		return cachedFacingToFacingMappings[facing.ordinal()];
	}

	public EnumFacing getFacing() {
		return EnumFacing.getFront(getBlockMetadata());
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		byte[] mappings = new byte[5];
		for (int i = 0; i < sideMappings.length; i++) {
			mappings[i] = sideMappings[i] == null ? -1 : (byte) sideMappings[i].getIndex();
		}
		compound.setByteArray("SideMappings", mappings);
		compound.setTag("Upgrades", itemHandlerUpgrades.serializeNBT());
		compound.setByte("StackLimiter", (byte) stackLimiterLimit);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		byte[] mappings = compound.getByteArray("SideMappings");
		if (mappings.length == 5) {
			for (int i = 0; i < mappings.length; i++) {
				if (mappings[i] != -1) {
					sideMappings[i] = EnumFacing.getFront(mappings[i]);
				} else {
					sideMappings[i] = null;
				}
			}
		}

		itemHandlerUpgrades.deserializeNBT(compound.getCompoundTag("Upgrades"));
		stackLimiterLimit = compound.getByte("StackLimiter");
		updateUpgrades();
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		if (cachedConnectedTile != null) {
			EnumFacing ioSide = getSideMapping(facing);
			if (ioSide != null) {
				return cachedConnectedTile.hasCapability(capability, ioSide);
			}
		}
		return super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (cachedConnectedTile != null) {
			EnumFacing ioSide = getSideMapping(facing);
			if (ioSide != null) {
				if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && requiresItemHandlerWrapping()) {
					int cacheIdx = ioSide.getIndex();
					if (cachedItemHandlers[cacheIdx] != null) {
						if (!cachedItemHandlers[cacheIdx].revalidate()) {
							cachedItemHandlers[cacheIdx] = null;
							return null;
						}
					} else {
						IItemHandler itemHandler = cachedConnectedTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, ioSide);
						if (itemHandler == null) {
							return null;
						}
						cachedItemHandlers[cacheIdx] = new ItemHandlerWrapper(cachedConnectedTile, ioSide, itemHandler);
					}
					return (T) cachedItemHandlers[cacheIdx];
				}
				return cachedConnectedTile.getCapability(capability, ioSide);
			}
		}
		return super.getCapability(capability, facing);
	}

	private boolean requiresItemHandlerWrapping() {
		return hasStackLimiter;
	}

	@Override
	public String getUnlocalizedName() {
		return "container.refinedrelocation:block_extender";
	}

	public ItemStackHandler getItemHandlerUpgrades() {
		return itemHandlerUpgrades;
	}

	private void updateUpgrades() {
		hasStackLimiter = false;
		for (int i = 0; i < itemHandlerUpgrades.getSlots(); i++) {
			ItemStack itemStack = itemHandlerUpgrades.getStackInSlot(i);
			if (!itemStack.isEmpty()) {
				if(itemStack.getItem() == ModItems.stackLimiter) {
					hasStackLimiter = true;
				}
			}
		}
	}

	public int getStackLimiterLimit() {
		return stackLimiterLimit;
	}

	public void setStackLimiterLimit(int stackLimiterLimit) {
		this.stackLimiterLimit = stackLimiterLimit;
	}
}
