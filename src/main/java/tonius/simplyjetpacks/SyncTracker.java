package tonius.simplyjetpacks;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import tonius.simplyjetpacks.config.SJConfig;
import tonius.simplyjetpacks.item.jetpack.JetpackParticleType;
import tonius.simplyjetpacks.network.PacketHandler;
import tonius.simplyjetpacks.network.message.MessageConfigSync;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class SyncTracker {
    
    private static Map<EntityPlayer, Boolean> flyKeyState = new HashMap<EntityPlayer, Boolean>();
    private static Map<EntityPlayer, Boolean> descendKeyState = new HashMap<EntityPlayer, Boolean>();
    
    private static Map<EntityPlayer, Boolean> forwardKeyState = new HashMap<EntityPlayer, Boolean>();
    private static Map<EntityPlayer, Boolean> backwardKeyState = new HashMap<EntityPlayer, Boolean>();
    private static Map<EntityPlayer, Boolean> leftKeyState = new HashMap<EntityPlayer, Boolean>();
    private static Map<EntityPlayer, Boolean> rightKeyState = new HashMap<EntityPlayer, Boolean>();
    
    private static Map<Integer, JetpackParticleType> jetpackState = new HashMap<Integer, JetpackParticleType>();
    
    public static boolean isFlyKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return flyKeyState.containsKey(user) && flyKeyState.get(user);
        }
        return true;
    }
    
    public static boolean isDescendKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return descendKeyState.containsKey(user) && descendKeyState.get(user);
        }
        return false;
    }
    
    public static boolean isForwardKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return forwardKeyState.containsKey(user) && forwardKeyState.get(user);
        }
        return true;
    }
    
    public static boolean isBackwardKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return backwardKeyState.containsKey(user) && backwardKeyState.get(user);
        }
        return false;
    }
    
    public static boolean isLeftKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return leftKeyState.containsKey(user) && leftKeyState.get(user);
        }
        return false;
    }
    
    public static boolean isRightKeyDown(EntityLivingBase user) {
        if (user instanceof EntityPlayer) {
            return rightKeyState.containsKey(user) && rightKeyState.get(user);
        }
        return false;
    }
    
    public static void processKeyUpdate(EntityPlayer player, boolean keyFly, boolean keyDescend, boolean keyForward, boolean keyBackward, boolean keyLeft, boolean keyRight) {
        flyKeyState.put(player, keyFly);
        descendKeyState.put(player, keyDescend);
        
        forwardKeyState.put(player, keyForward);
        backwardKeyState.put(player, keyBackward);
        leftKeyState.put(player, keyLeft);
        rightKeyState.put(player, keyRight);
    }
    
    public static void processJetpackUpdate(int entityId, JetpackParticleType particleType) {
        if (particleType != null) {
            jetpackState.put(entityId, particleType);
        } else {
            jetpackState.remove(entityId);
        }
    }
    
    public static Map<Integer, JetpackParticleType> getJetpackStates() {
        return jetpackState;
    }
    
    public static void clearAll() {
        flyKeyState.clear();
        forwardKeyState.clear();
        backwardKeyState.clear();
        leftKeyState.clear();
        rightKeyState.clear();
    }
    
    public static void removeFromAll(EntityPlayer player) {
        flyKeyState.remove(player);
        forwardKeyState.remove(player);
        backwardKeyState.remove(player);
        leftKeyState.remove(player);
        rightKeyState.remove(player);
    }
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent evt) {
        PacketHandler.instance.sendTo(new MessageConfigSync(), (EntityPlayerMP) evt.player);
    }
    
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent evt) {
        removeFromAll(evt.player);
    }
    
    @SubscribeEvent
    public void onClientDisconnectedFromServer(ClientDisconnectionFromServerEvent evt) {
        SJConfig.onConfigChanged("simplyjetpacks");
    }
    
}
