package mctmods.immersivetechnology.common.blocks.metal.tileentities;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.util.ChatUtils;
import blusunrize.immersiveengineering.common.util.Utils;
import mctmods.immersivetechnology.common.util.ITFluidTank;
import mctmods.immersivetechnology.common.util.TranslationKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Random;

public class TileEntityBarrelOpen extends TileEntityBarrelSteel {

	public ITFluidTank tank = new ITFluidTank(12000, this);

	private int lastRandom = 0;
	private int sleep = 0;

	private static Random RANDOM = new Random();

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		this.readTank(nbt);
	}

	public void readTank(NBTTagCompound nbt) {
		tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		this.writeTank(nbt, false);
	}

	public void writeTank(NBTTagCompound nbt, boolean toItem) {
		boolean write = tank.getFluidAmount() > 0;
		NBTTagCompound tankTag = tank.writeToNBT(new NBTTagCompound());
		if(!toItem || write) nbt.setTag("tank", tankTag);
	}

	@Override
	public void update() {
		if(world.isRemote) return;
		if(tank.getFluidAmount() != tank.getCapacity()) {
			int random = 1 + RANDOM.nextInt(100);
			if(random == lastRandom) {
				if(tank.getFluid() == null || tank.getFluid().getFluid() == FluidRegistry.WATER) {
					float temp = world.getBiomeProvider().getTemperatureAtHeight(world.getBiome(pos).getTemperature(pos), pos.getY());
					if(world.isRaining() && world.canSeeSky(pos) && temp > 0.05F && temp < 2.0F) {
						int amount = 100;
						if(world.isThundering()) amount = 200;
						tank.fill(new FluidStack(FluidRegistry.WATER, amount), true);
					} else if(temp >= 2.0F) {
						tank.drain(100, true);
					}
				}
			}
			lastRandom = random;
		}
		for(int index = 0; index < 2; index++) {
			if(tank.getFluidAmount() > 0 && sideConfig[index] == 1) {
				if(tank.getFluidAmount() > 0) {
					EnumFacing face = EnumFacing.getFront(index);
					IFluidHandler output = FluidUtil.getFluidHandler(world, getPos().offset(face), face.getOpposite());
					if(output != null) {
						if(sleep == 0) {
							FluidStack accepted = Utils.copyFluidStackWithAmount(tank.getFluid(), Math.min(40, tank.getFluidAmount()), false);
							accepted.amount = output.fill(Utils.copyFluidStackWithAmount(accepted, accepted.amount, true), false);
							if(accepted.amount > 0) {
								int drained = output.fill(Utils.copyFluidStackWithAmount(accepted, accepted.amount, false), true);
								tank.drain(drained, true);
								sleep = 0;
							} else {
								sleep = 20;
							}
						} else {
							sleep--;
						}
					}
				}
			}
		}
	}

	@Override
	public void TankContentsChanged() {
		this.markContainingBlockForUpdate(null);
	}

	@Override
	public String[] getOverlayText(EntityPlayer player, RayTraceResult mop, boolean hammer) {
		if(Utils.isFluidRelatedItemStack(player.getHeldItem(EnumHand.MAIN_HAND))) {
			FluidStack fluid = tank.getFluid();
			return (fluid != null)?
					new String[]{TranslationKey.OVERLAY_OSD_BARREL_NORMAL_FIRST_LINE.format(fluid.getLocalizedName(), fluid.amount)}:
					new String[]{TranslationKey.GUI_EMPTY.text()};
		}
		return null;
	}

	@Override
	public int getComparatorInputOverride()	{
		return (int)(15 * (tank.getFluidAmount() / (float)tank.getCapacity()));
	}

	@Override
	public boolean toggleSide(int side, EntityPlayer p) {
		return false;
	}

	@Override
	public boolean interact(EnumFacing side, EntityPlayer player, EnumHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ) {
		FluidStack fluid = FluidUtil.getFluidContained(heldItem);
		if(!isFluidValid(fluid)) {
			ChatUtils.sendServerNoSpamMessages(player, new TextComponentTranslation(Lib.CHAT_INFO + "noGasAllowed"));
			return true;
		}
		if(FluidUtil.interactWithFluidHandler(player, hand, tank)) {
			return true;
		}
		return false;
	}

}