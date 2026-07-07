#version 330

#moj_import <minecraft:projection.glsl>

// gpu-resident billboards, rebuilt for 26.1 without compute: each vertex carries its particle's spawn
// state and re-integrates the force stack from spawn to the burst's current age, mirroring the cpu sim's
// per-tick order exactly (modifiers touch velocity, then position integrates). every particle of a burst
// ages together, so the whole march runs from uniforms and spawn attributes; nothing persists between
// frames. six vertices per particle, the corner from gl_VertexID

in vec4 SpawnPos;   // xyz position in emitter space, w billboard roll
in vec4 SpawnVel;   // xyz velocity per tick, w roll change per tick

layout(std140) uniform GpuBurstConfig {
    mat4 BurstModelView;
    vec4 OffsetLife;     // xyz emitter origin relative to the camera, w 0..1 life fraction
    vec4 CamRightAge;    // xyz camera right, w age in ticks
    vec4 CamUpFrames;    // xyz camera up, w flipbook frame count (1 when not animating)
    vec4 SizeAlpha;      // size start, size end, alpha start, alpha end
    ivec4 Eases;         // x size ease, y alpha ease, z color ease, w color stop count
    ivec4 ForceMeta;     // x force count
    ivec4 ForceTypes[8]; // x: 1 gravity, 2 drag, 3 attractor, 4 vortex
    vec4 ForceA[8];
    vec4 ForceB[8];
    vec4 ColorStops[8];
    vec4 UvCell[4];      // one atlas cell per flipbook frame
};

out vec4 vColor;
out vec2 vUv;

const vec2 CORNERS[6] = vec2[](
    vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0),
    vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(1.0, -1.0)
);
const vec2 UVS[6] = vec2[](
    vec2(0.0, 1.0), vec2(0.0, 0.0), vec2(1.0, 0.0),
    vec2(0.0, 1.0), vec2(1.0, 0.0), vec2(1.0, 1.0)
);

// ordinals of the engine's Easings enum
float easeAt(int kind, float t) {
    if (kind == 1) {
        return t * t;
    }
    if (kind == 2) {
        return t * (2.0 - t);
    }
    if (kind == 3) {
        return t < 0.5 ? 2.0 * t * t : -1.0 + (4.0 - 2.0 * t) * t;
    }
    if (kind == 4) {
        return t * t * t;
    }
    if (kind == 5) {
        float f = t - 1.0;
        return f * f * f + 1.0;
    }
    return t;
}

// multi-stop gradient, the ColorCurve mapping: t picks the segment, the easing runs inside it
vec3 colorAt(float t) {
    if (Eases.w <= 1) {
        return ColorStops[0].rgb;
    }
    float scaled = t * float(Eases.w - 1);
    int i = min(int(scaled), Eases.w - 2);
    return mix(ColorStops[i].rgb, ColorStops[i + 1].rgb, easeAt(Eases.z, scaled - float(i)));
}

void main() {
    int corner = gl_VertexID % 6;
    float life = OffsetLife.w;
    int age = int(CamRightAge.w + 0.5);

    // the same tick the old compute shader ran, repeated from spawn: cheap per step, and a burst's
    // lifetime is short, so even large bursts stay a bounded march
    vec3 pos = SpawnPos.xyz;
    vec3 vel = SpawnVel.xyz;
    for (int s = 0; s < age; s++) {
        for (int f = 0; f < ForceMeta.x; f++) {
            if (ForceTypes[f].x == 1) {
                vel += ForceA[f].xyz;
            } else if (ForceTypes[f].x == 2) {
                vel *= ForceA[f].x;
            } else if (ForceTypes[f].x == 3) {
                vec3 d = ForceA[f].xyz - pos;
                float dist = length(d);
                if (dist >= 1e-4) {
                    vel += d * (ForceB[f].x / dist);
                }
            } else if (ForceTypes[f].x == 4) {
                vec2 r = pos.xz - ForceA[f].xz;
                float len = length(r);
                if (len >= 1e-4) {
                    vel += vec3(-r.y, 0.0, r.x) * (ForceB[f].x / len);
                }
            }
        }
        pos += vel;
    }
    float roll = SpawnPos.w + SpawnVel.w * float(age);

    float size = SizeAlpha.x + (SizeAlpha.y - SizeAlpha.x) * easeAt(Eases.x, life);
    float alpha = SizeAlpha.z + (SizeAlpha.w - SizeAlpha.z) * easeAt(Eases.y, life);

    // rotate the unit corner by the particle's roll, then span it on the camera plane
    vec2 c = CORNERS[corner];
    float cs = cos(roll);
    float sn = sin(roll);
    vec2 rc = vec2(c.x * cs - c.y * sn, c.x * sn + c.y * cs);
    vec3 world = pos + OffsetLife.xyz + (CamRightAge.xyz * rc.x + CamUpFrames.xyz * rc.y) * size;
    gl_Position = ProjMat * BurstModelView * vec4(world, 1.0);

    int frames = int(CamUpFrames.w + 0.5);
    int frame = min(int(life * float(frames)), frames - 1);
    vec4 cell = UvCell[frame];
    vUv = mix(cell.xy, cell.zw, UVS[corner]);
    vColor = vec4(colorAt(life), alpha);
}
