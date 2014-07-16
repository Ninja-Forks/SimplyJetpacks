package tonius.simplyjetpacks.item.jetpack;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import tonius.simplyjetpacks.KeyboardTracker;
import tonius.simplyjetpacks.config.MainConfig;
import tonius.simplyjetpacks.item.ItemEnergyArmor;
import tonius.simplyjetpacks.network.PacketHandler;
import tonius.simplyjetpacks.network.message.MessageJetpackParticles;
import tonius.simplyjetpacks.setup.SJItems;
import tonius.simplyjetpacks.util.StackUtils;
import tonius.simplyjetpacks.util.StringUtils;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemJetpack extends ItemEnergyArmor {

    protected int jetpackTier;
    protected int tickEnergy;
    protected int tickEnergyHover;
    protected double maxSpeed;
    protected double acceleration;
    protected double forwardThrust;
    protected double hoverModeIdleSpeed;
    protected double hoverModeActiveSpeed;

    public ItemJetpack(ArmorMaterial material, String name, int maxEnergy, int maxInput, int jetpackTier, int tickEnergy, double maxSpeed, double acceleration, double forwardThrust, double hoverModeIdleSpeed, double hoverModeActiveSpeed) {
        super(material, 2, 1, name, maxEnergy, maxInput, 0);
        this.jetpackTier = jetpackTier;
        this.tickEnergy = tickEnergy;
        this.tickEnergyHover = (int) (tickEnergy / 1.5);
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
        this.forwardThrust = forwardThrust;
        this.hoverModeIdleSpeed = hoverModeIdleSpeed;
        this.hoverModeActiveSpeed = hoverModeActiveSpeed;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack itemStack) {
        switch (this.jetpackTier) {
        case 3:
            return StringUtils.YELLOW + super.getItemStackDisplayName(itemStack);
        case 4:
            return StringUtils.BRIGHT_BLUE + super.getItemStackDisplayName(itemStack);
        case 9001:
            return StringUtils.PINK + super.getItemStackDisplayName(itemStack);
        }
        return super.getItemStackDisplayName(itemStack);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer player, List list, boolean par4) {
        if (StringUtils.canShowDetails()) {
            list.add(StringUtils.getChargeText(this.jetpackTier == 9001, this.getEnergyStored(itemStack), this.getMaxEnergyStored(itemStack)));
            list.add(StringUtils.getStateText(this.isOn(itemStack)));
            list.add(StringUtils.getHoverModeText(this.isHoverModeActive(itemStack)));
            int currentTickEnergy = this.isHoverModeActive(itemStack) ? this.tickEnergyHover : this.tickEnergy;
            list.add(StringUtils.getEnergyUsageText(currentTickEnergy));
            list.add(StringUtils.BRIGHT_GREEN + StringUtils.translate("tooltip.jetpack.description.1"));
            list.add(StringUtils.BRIGHT_GREEN + StringUtils.translate("tooltip.jetpack.description.2"));
            if (this.jetpackTier > 0 && this.jetpackTier <= 4 && MainConfig.enableCraftingArmorPlating) {
                list.add(StringUtils.getArmorText(this.isArmored()));
                if (!this.isArmored()) {
                    list.add(StringUtils.getRequiredArmorText(this.jetpackTier));
                }
            }
        } else {
            list.add(StringUtils.getShiftText());
        }
    }

    @Override
    public void getSubItems(Item item, CreativeTabs creativeTabs, List list) {
        if (this.jetpackTier > 0 && this.jetpackTier <= 4) {
            list.add(new ItemStack(this));
        }
        list.add(this.getChargedItem(this));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
        if (this.jetpackTier > 0 && this.jetpackTier <= 4 && player.isSneaking()) {
            if (this.isArmored()) {
                this.removeArmor(itemStack, player);
                if (!world.isRemote) {
                    EntityItem item = player.entityDropItem(new ItemStack(SJItems.components, 1, this.jetpackTier + 4), 0.0F);
                    item.delayBeforeCanPickup = 0;
                }
            } else {
                InventoryPlayer inv = player.inventory;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack currentStack = inv.getStackInSlot(i);
                    if (currentStack != null && currentStack.getItem() == SJItems.components && currentStack.getItemDamage() == this.jetpackTier + 4) {
                        inv.setInventorySlotContents(i, StackUtils.decrementStack(currentStack));
                        this.applyArmor(itemStack, player);
                        break;
                    }
                }
            }
            return itemStack;
        }
        return super.onItemRightClick(itemStack, world, player);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
        this.useJetpack(player, itemStack, false);
    }

    @Override
    public boolean isDamaged(ItemStack itemStack) {
        return this.jetpackTier == 9001 ? false : super.isDamaged(itemStack);
    }

    public void useJetpack(EntityLivingBase user, ItemStack jetpack, boolean force) {
        if (isOn(jetpack)) {
            boolean hoverMode = this.isHoverModeActive(jetpack);
            double hoverSpeed = this.getHoverSpeed(jetpack, user);
            boolean jumpKeyDown = true;
            if (!force && user instanceof EntityPlayer && !KeyboardTracker.isJumpKeyDown((EntityPlayer) user)) {
                jumpKeyDown = false;
            }

            if (jumpKeyDown || (hoverMode && !user.onGround && user.motionY < 0)) {
                int usedPower = hoverMode ? this.tickEnergyHover : this.tickEnergy;
                if (!user.worldObj.isRemote) {
                    this.subtractEnergy(jetpack, usedPower, false);
                }

                if (this.getEnergyStored(jetpack) > 0) {
                    if (jumpKeyDown) {
                        if (!hoverMode) {
                            user.motionY = Math.min(user.motionY + this.acceleration, this.maxSpeed);
                        } else {
                            user.motionY = Math.min(user.motionY + this.acceleration, this.hoverModeActiveSpeed);
                        }
                    } else {
                        user.motionY = Math.max(user.motionY, -hoverSpeed);
                    }

                    if (user instanceof EntityPlayer) {
                        if (KeyboardTracker.isForwardKeyDown((EntityPlayer) user)) {
                            user.moveFlying(0, (float) this.forwardThrust, (float) this.forwardThrust);
                        }
                        if (KeyboardTracker.isBackwardKeyDown((EntityPlayer) user)) {
                            user.moveFlying(0, (float) -this.forwardThrust, (float) this.forwardThrust * 0.8F);
                        }
                        if (KeyboardTracker.isLeftKeyDown((EntityPlayer) user)) {
                            user.moveFlying((float) this.forwardThrust, 0, (float) this.forwardThrust);
                        }
                        if (KeyboardTracker.isRightKeyDown((EntityPlayer) user)) {
                            user.moveFlying((float) -this.forwardThrust, 0, (float) this.forwardThrust);
                        }
                    }

                    if (!user.worldObj.isRemote) {
                        user.fallDistance = 0.0F;
                        if (user instanceof EntityPlayerMP) {
                            ((EntityPlayerMP) user).playerNetServerHandler.floatingTickCount = 0;
                        }
                        this.sendParticlePacket(user, this.getParticleType(jetpack));
                        this.sendParticlePacket(user, this.getParticleType(jetpack));
                    }
                }
            }
        }
    }

    public void applyArmor(ItemStack jetpack, EntityPlayer player) {
        jetpack.func_150996_a(SJItems.armoredJetpacks[this.jetpackTier]);
        player.worldObj.playSoundAtEntity(player, "random.anvil_use", 0.8F, 0.9F + player.getRNG().nextFloat() * 0.2F);
    }

    public void removeArmor(ItemStack jetpack, EntityPlayer player) {
        jetpack.func_150996_a(SJItems.jetpacks[this.jetpackTier]);
        player.worldObj.playSoundAtEntity(player, "random.break", 1.0F, 0.9F + player.getRNG().nextFloat() * 0.2F);
    }

    public boolean isHoverModeActive(ItemStack itemStack) {
        return StackUtils.getNBT(itemStack).getBoolean("HoverModeActive");
    }

    public void toggleHoverMode(ItemStack itemStack, EntityPlayer player) {
        if (this.isHoverModeActive(itemStack)) {
            player.addChatMessage(new ChatComponentText(StringUtils.translate("chat.jetpack.hoverMode") + " " + StringUtils.LIGHT_RED + StringUtils.translate("chat.disabled")));
            itemStack.stackTagCompound.setBoolean("HoverModeActive", false);
        } else {
            player.addChatMessage(new ChatComponentText(StringUtils.translate("chat.jetpack.hoverMode") + " " + StringUtils.BRIGHT_GREEN + StringUtils.translate("chat.enabled")));
            itemStack.stackTagCompound.setBoolean("HoverModeActive", true);
        }
    }

    public double getHoverSpeed(ItemStack jetpack, EntityLivingBase user) {
        if (!MainConfig.invertHoverSneakingBehavior) {
            return user.isSneaking() ? this.hoverModeActiveSpeed : this.hoverModeIdleSpeed;
        } else {
            return user.isSneaking() ? this.hoverModeIdleSpeed : this.hoverModeActiveSpeed;
        }
    }

    public void sendParticlePacket(EntityLivingBase user, JetpackParticleType particle) {
        PacketHandler.instance.sendToAllAround(new MessageJetpackParticles(user.getEntityId(), particle), new TargetPoint(user.worldObj.provider.dimensionId, user.posX, user.posY, user.posZ, 96));
    }

    public boolean isArmored() {
        return false;
    }

    public int getTier() {
        return this.jetpackTier;
    }

    public JetpackParticleType getParticleType(ItemStack jetpack) {
        int particle = StackUtils.getNBT(jetpack).getInteger("JetParticleType");
        JetpackParticleType particleType = JetpackParticleType.values()[particle];
        if (particleType != null) {
            return particleType;
        }
        return JetpackParticleType.DEFAULT;
    }

    public void setParticleType(ItemStack jetpack, JetpackParticleType particle) {
        StackUtils.getNBT(jetpack).setInteger("JetParticleType", particle.ordinal());
    }

    @Override
    public String getActivateMsg() {
        return StringUtils.translate("chat.jetpack.engine") + " " + StringUtils.BRIGHT_GREEN + StringUtils.translate("chat.enabled");
    }

    @Override
    public String getDeactivateMsg() {
        return StringUtils.translate("chat.jetpack.engine") + " " + StringUtils.LIGHT_RED + StringUtils.translate("chat.disabled");
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        return 0;
    }

}
