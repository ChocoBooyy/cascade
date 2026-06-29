package dev.chocoboy.cascade.engine.math;

public record Vec3f(float x, float y, float z) {

    public static final Vec3f ZERO = new Vec3f(0f, 0f, 0f);

    public Vec3f add(Vec3f o) {
        return new Vec3f(x + o.x, y + o.y, z + o.z);
    }

    public Vec3f scale(float s) {
        return new Vec3f(x * s, y * s, z * s);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3f normalize() {
        float len = length();
        return len == 0f ? this : scale(1f / len);
    }

    public Vec3f cross(Vec3f o) {
        return new Vec3f(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }
}
