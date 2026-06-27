package dev.chocoboy.cascade.engine.math;

public final class Noise {

    private Noise() {
    }

    // Smooth 3D value noise in [-1, 1]. Not Perlin, but spatially coherent enough to drive turbulence.
    public static float value(float x, float y, float z) {
        int x0 = floor(x);
        int y0 = floor(y);
        int z0 = floor(z);
        float u = smooth(x - x0);
        float v = smooth(y - y0);
        float w = smooth(z - z0);

        float x00 = lerp(hash(x0, y0, z0), hash(x0 + 1, y0, z0), u);
        float x10 = lerp(hash(x0, y0 + 1, z0), hash(x0 + 1, y0 + 1, z0), u);
        float x01 = lerp(hash(x0, y0, z0 + 1), hash(x0 + 1, y0, z0 + 1), u);
        float x11 = lerp(hash(x0, y0 + 1, z0 + 1), hash(x0 + 1, y0 + 1, z0 + 1), u);

        float y0v = lerp(x00, x10, v);
        float y1v = lerp(x01, x11, v);
        return lerp(y0v, y1v, w);
    }

    private static int floor(float f) {
        int i = (int) f;
        return f < i ? i - 1 : i;
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 32767.5f - 1f;
    }
}
