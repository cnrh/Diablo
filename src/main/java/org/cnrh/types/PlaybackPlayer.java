package org.cnrh.types;

import net.minestom.server.entity.*;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class PlaybackPlayer extends EntityCreature {
    private final String username;
    private final String skinTexture;
    private final String skinSignature;
    public static final HashMap<String, PlaybackPlayer> playbackPlayerMap = new HashMap<>();

    public PlaybackPlayer(@NotNull String username, @NotNull String skinTexture, @NotNull String skinSignature) {
        super(EntityType.PLAYER);
        this.username = username;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;

        //setNoGravity(true)
        setAutoViewable(true);
        setInvisible(false);
        setInvulnerable(true);
    }

    public void updateNewView(@NotNull Player player) {
        var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
        if (skinTexture != null && skinSignature != null) {
            properties.add(new PlayerInfoUpdatePacket.Property("textures", skinTexture, skinSignature));
        }
        var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), username, properties, false,
                0, GameMode.SURVIVAL, null, null);
        player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));
        super.updateNewViewer(player);
        player.sendPackets(new EntityMetaDataPacket(getEntityId(), Map.of(17, Metadata.Byte((byte) 127))));
        playbackPlayerMap.put(username, this);
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        super.updateOldViewer(player);
        player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
    }
}
