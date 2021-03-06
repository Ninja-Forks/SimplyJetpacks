package tonius.simplyjetpacks.tickhandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.monster.EntityMob;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import tonius.simplyjetpacks.item.ItemJetpack;
import tonius.simplyjetpacks.item.jetpack.Jetpack;
import tonius.simplyjetpacks.item.jetpack.JetpackParticleType;
import tonius.simplyjetpacks.item.jetpack.JetpackPotato;
import tonius.simplyjetpacks.network.PacketHandler;
import tonius.simplyjetpacks.network.message.MessageJetpackSync;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class LivingTickHandler {
    
    private static Map<Integer, JetpackParticleType> lastJetpackState = new ConcurrentHashMap<Integer, JetpackParticleType>();
    private static Random rand = new Random();
    
    @SubscribeEvent
    public void onLivingTick(LivingUpdateEvent evt) {
        JetpackParticleType jetpackState = null;
        ItemStack armor = evt.entityLiving.getEquipmentInSlot(3);
        boolean foundJetpack = false;
        if (armor != null && armor.getItem() instanceof ItemJetpack) {
            Jetpack jetpack = ((ItemJetpack) armor.getItem()).getJetpack(armor);
            if (jetpack != null) {
                jetpackState = jetpack.particleToShow(armor, (ItemJetpack) armor.getItem(), evt.entityLiving);
                foundJetpack = true;
            }
        }
        
        if (jetpackState != lastJetpackState.get(evt.entityLiving.getEntityId())) {
            if (jetpackState == null) {
                lastJetpackState.remove(evt.entityLiving.getEntityId());
            } else {
                lastJetpackState.put(evt.entityLiving.getEntityId(), jetpackState);
            }
            PacketHandler.instance.sendToAllAround(new MessageJetpackSync(evt.entityLiving.getEntityId(), jetpackState != null ? jetpackState.ordinal() : -1), new TargetPoint(evt.entityLiving.dimension, evt.entityLiving.posX, evt.entityLiving.posY, evt.entityLiving.posZ, 256));
        } else if (foundJetpack && evt.entityLiving.worldObj.getTotalWorldTime() % 160L == 0) {
            PacketHandler.instance.sendToAllAround(new MessageJetpackSync(evt.entityLiving.getEntityId(), jetpackState != null ? jetpackState.ordinal() : -1), new TargetPoint(evt.entityLiving.dimension, evt.entityLiving.posX, evt.entityLiving.posY, evt.entityLiving.posZ, 256));
        }
        
        if (evt.entityLiving.worldObj.getTotalWorldTime() % 200L == 0) {
            Iterator<Integer> itr = lastJetpackState.keySet().iterator();
            while (itr.hasNext()) {
                int entityId = itr.next();
                if (evt.entityLiving.worldObj.getEntityByID(entityId) == null) {
                    itr.remove();
                }
            }
        }
    }
    
    @SubscribeEvent
    public void mobUseJetpack(LivingUpdateEvent evt) {
        if (evt.entityLiving instanceof EntityMob) {
            ItemStack armor = evt.entityLiving.getEquipmentInSlot(3);
            if (armor != null && armor.getItem() instanceof ItemJetpack) {
                ItemJetpack jetpackItem = (ItemJetpack) armor.getItem();
                Jetpack jetpack = jetpackItem.getJetpack(armor);
                if (jetpack != null) {
                    if (jetpack instanceof JetpackPotato || rand.nextInt(3) == 0) {
                        jetpack.setMobMode(armor);
                        jetpack.useJetpack(evt.entityLiving, armor, jetpackItem, false);
                    }
                }
                if (evt.entityLiving.posY > evt.entityLiving.worldObj.getActualHeight() + 10) {
                    evt.entityLiving.attackEntityFrom(DamageSource.generic, 80);
                }
            }
        }
    }
    
}
