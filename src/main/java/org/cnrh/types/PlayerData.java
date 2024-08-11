package org.cnrh.types;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.cnrh.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class PlayerData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerData.class);

    public PlaybackPlayer npc;
    public GameEntity selector;
    public GameEntity camera;
    public Player plr;
    public Instance instance;
    public boolean isPathfinding;
    public Pos oldnpcpos;

    public static HashMap<Player, PlayerData> playerDataMap = new HashMap<>();

    public static void registerPlayer(Player player) {
        PlayerData playerData = new PlayerData(player);
        playerDataMap.put(player, playerData);
    }

    public static void unregisterPlayer(Player player) {
        PlayerData playerData = getPlayerData(player);
        playerData.camera.remove();
        playerData.selector.remove();
        playerData.npc.remove();
        playerDataMap.remove(player);
    }

    public static PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player);
    }

    private PlayerData(Player player) {
        plr = player;
        instance = plr.getInstance();

        plr.setInvulnerable(true);
        plr.setInvisible(true);

        npc = new PlaybackPlayer(plr.getUsername(), plr.getSkin().textures(), plr.getSkin().signature());
        npc.setInstance(instance, Main.getHighestBlock(instance, 0, 0, false).add(0, 1, 0));
        npc.updateNewView(player);
        oldnpcpos = npc.getPosition();

        camera = new GameEntity(plr, instance, EntityType.BAT);
        camera.setNoGravity(true);
        camera.addPassenger(plr);
        camera.addViewer(plr);
        plr.spectate(camera);

        selector = new GameEntity(plr, instance, EntityType.SHULKER);
        selector.setNoGravity(true);
        selector.setGlowing(true);
        selector.addViewer(plr);
    }

    public boolean isPathfinding() {
        return isPathfinding;
    }

    public void setPathfinding(boolean pathfinding) {
        isPathfinding = pathfinding;
    }

    public void setScene(PlayerData playerData) {
        playerData.camera.teleport(npc.getPosition().add(-2, 4, -2).withView(-45, 45));
        //playerData.plr.setView(0, 0);
        playerData.selector.teleport(npc.getPosition().add(0, -1, 0));
        playerData.setPathfinding(false);
    }

    public void updateCameraPosition() {
        Pos npcPos = npc.getPosition();
        double y = 4; // default 4 above player
        Pos newCameraPos = npcPos.add(-2, y, -2).withView(-45, 45);
        camera.teleport(newCameraPos);
        plr.teleport(newCameraPos.withView(0, 0));  // This ensures the player's view is updated
    }
}
