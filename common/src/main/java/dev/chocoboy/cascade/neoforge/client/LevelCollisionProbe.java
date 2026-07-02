package dev.chocoboy.cascade.neoforge.client;

import dev.chocoboy.cascade.engine.effect.CollisionProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// block-backed probe for the client level. emitter-local offsets are resolved against the world origin.
public final class LevelCollisionProbe implements CollisionProbe {

    private final Level level;
    private final Vec3 origin;

    public LevelCollisionProbe(Level level, Vec3 origin) {
        this.level = level;
        this.origin = origin;
    }

    @Override
    public boolean solid(float x, float y, float z) {
        BlockPos pos = BlockPos.containing(origin.x + x, origin.y + y, origin.z + z);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
