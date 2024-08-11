package org.cnrh.types;

import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class GameEntity extends EntityCreature {
    public GameEntity(@NotNull Player player, @NotNull Instance instance, @NotNull EntityType entityType) {
        super(entityType);
        setInvisible(true);
        setInvulnerable(true);
        setAutoViewable(false);
        setInstance(instance);
    }
}
