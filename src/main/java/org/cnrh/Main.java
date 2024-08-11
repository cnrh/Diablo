package org.cnrh;

import de.articdive.jnoise.generators.noisegen.opensimplex.SuperSimplexNoiseGenerator;
import de.articdive.jnoise.pipeline.JNoise;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerRotationPacket;
import org.cnrh.types.GameEntity;
import org.cnrh.types.PlaybackPlayer;
import org.cnrh.types.PlayerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final float yLevel = 40;

    public static void main(String[] args) {
        LOGGER.info("Starting server..");

        MinecraftServer minecraftServer = MinecraftServer.init();
        MojangAuth.init();

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();

        InstanceContainer mainInstance = instanceManager.createInstanceContainer();

        JNoise noise = JNoise.newBuilder().superSimplex(SuperSimplexNoiseGenerator.newBuilder().setSeed((long) (Math.random()*5000)).build()).scale(0.005).build();

        mainInstance.setGenerator((GenerationUnit unit) -> {
            Point start = unit.absoluteStart();
            int chunkX = start.blockX() >> 4;
            int chunkZ = start.blockZ() >> 4;
            Block blockToUse = ((chunkX + chunkZ) % 2 == 0) ? Block.STONE : Block.SMOOTH_STONE;
            for (int x = 0; x < unit.size().x(); x++) {
                for (int z = 0; z < unit.size().z(); z++) {
                    Point bottom = start.add(x, 0, z);

                    double noiseValue = (noise.evaluateNoise(bottom.x(), bottom.z()) + 1) / 2;
                    int height = (int) (noiseValue * yLevel);

                    unit.modifier().fill(bottom, bottom.add(1, 0, 1).withY(height), blockToUse);
                }
            }
        });
        mainInstance.setChunkSupplier(LightingChunk::new);

        AtomicReference<Pos> spawnPoint = new AtomicReference<>(new Pos(0, yLevel + 1, 0));
        CompletableFuture<Chunk> chunkFuture = mainInstance.loadChunk(0, 0);
        chunkFuture.thenAccept(chunk -> {
            if (chunk != null) {
                spawnPoint.set(getHighestBlock(mainInstance, 0, 0, true));
            }
        });

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(mainInstance);
            player.setRespawnPoint(spawnPoint.get());
            LOGGER.info("Player {} has joined the server.", player.getUsername());
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();

            PlayerData.registerPlayer(player);
            PlayerData playerData = PlayerData.getPlayerData(player);
            playerData.setScene(playerData);

            player.getInstance().getPlayers().forEach(plr -> {
                PlayerData plrData = PlayerData.getPlayerData(plr);
                if (plrData.plr != playerData.plr) {
                    if (plrData.npc != null) plrData.npc.updateNewView(playerData.plr);
                    if (playerData.npc != null) playerData.npc.updateNewView(plrData.plr);
                }
            });

            player.setGameMode(GameMode.CREATIVE);
            Thread actionBarThread = new Thread(() -> {
                while (player.isOnline()) {
                    try {
                        int health = 100;
                        int mana = 1000;
                        final TextComponent actionBar = Component.text().content("Health: " + health).color(TextColor.color(0xe35f5f)).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(Component.text(" Mana: " + mana).color(TextColor.color(0x5fbbe3))).build();
                        player.sendActionBar(actionBar);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        LOGGER.info("Unable to update {}'s action bar. {}", player.getUsername(), e.getMessage());
                    }
                }
            });
            actionBarThread.start();
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerData playerData = PlayerData.getPlayerData(player);
            if (playerData != null) {
                PlayerData.unregisterPlayer(player);
            }
        });

        globalEventHandler.addListener(PlayerHandAnimationEvent.class, event -> {
            // Left click
            Player player = event.getPlayer();
            if (PlayerData.getPlayerData(player) == null) return;
            PlayerData playerData = PlayerData.getPlayerData(player);
            PlaybackPlayer npc = playerData.npc;
            GameEntity selector = playerData.selector;
            if (selector.getDistance(npc) < 3) return;

            // Set pathfinding flag to true
            playerData.setPathfinding(true);
            npc.getNavigator().setPathTo(selector.getPosition().add(0, 1, 0));

            // Continuously check if NPC has reached the selector
            new Thread(() -> {
                int count = 0;
                while (playerData.isPathfinding()) {
                    Pos npcPos = npc.getPosition();
                    Pos selectorPos = selector.getPosition().withY(npcPos.y());
                    double distance = npcPos.distance(selectorPos);

                    // If the NPC is close enough to the selector, force completion
                    if (distance < 2) {
                        playerData.setPathfinding(false);
                        playerData.updateCameraPosition();
                        break;
                    }

                    try {
                        playerData.updateCameraPosition();
                        Thread.sleep(100); // Check every 100 milliseconds
                        count++;
                        if (count >= 100) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        LOGGER.info("Can't sleep. {}", e.getMessage());
                    }
                }
                playerData.updateCameraPosition();
            }).start();
        });

        globalEventHandler.addListener(PlayerPacketEvent.class, event -> {
            ClientPacket packet = event.getPacket();
            Player player = event.getPlayer();
            if ((packet instanceof ClientPlayerRotationPacket rotationPacket) && (PlayerData.getPlayerData(player) != null)) {
                PlayerData playerData = PlayerData.getPlayerData(player);
                if (playerData.isPathfinding()) return;

                GameEntity selector = playerData.selector;
                PlaybackPlayer npc = playerData.npc;

                int sens = -8; // higher negative = slower
                float x = rotationPacket.yaw();
                float z = rotationPacket.pitch();

                Pos npcPosition = npc.getPosition();
                float offsetX = x / sens;
                float offsetZ = z / sens;
                Pos offsetPos = npcPosition.add(offsetX, 0, offsetZ);
                Pos pos = getHighestBlock(selector.getInstance(), (float) offsetPos.x(), (float) offsetPos.z(), true);
                double distance = npcPosition.distance(pos);
                if (distance > 10) {
                    double dx = pos.x() - npcPosition.x();
                    double dz = pos.z() - npcPosition.z();
                    double length = Math.sqrt(dx * dx + dz * dz);
                    dx = (dx / length) * 10;
                    dz = (dz / length) * 10;
                    pos = getHighestBlock(selector.getInstance(), (float)(npcPosition.x() + dx), (float)(npcPosition.z() + dz), true);
                }
                selector.teleport(pos);

                double dx = pos.x() - npcPosition.x();
                double dy = pos.y() - npcPosition.y();
                double dz = pos.z() - npcPosition.z();
                double xzDistance = Math.sqrt(dx * dx + dz * dz);
                double distance2 = Math.sqrt(xzDistance * xzDistance + dy * dy);
                float npcYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float npcPitch = (float) Math.toDegrees(-Math.atan2(dy, distance2));
                npcYaw = (npcYaw % 360 + 360) % 360;
                npcPitch = Math.max(-90, Math.min(90, npcPitch));
                npc.setView(npcYaw, npcPitch);
            }
        });

        minecraftServer.start("0.0.0.0", 25565);
        LOGGER.info("The server started successfully.");
    }

    public static Pos getHighestBlock(Instance instance, float x, float z, boolean floor) {
        int maxHeight = 256;
        for (float y = maxHeight - 1; y >= 0; y--) {
            Pos pos = new Pos(x, y, z);
            if (floor) pos = new Pos(Math.floor(x), Math.floor(y), Math.floor(z));
            if (!instance.getBlock(pos).isAir()) return pos;
        }
        return new Pos(x, yLevel+1, z);
    }
}
