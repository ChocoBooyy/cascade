#version 430

// vertex-pulled billboards: six vertices per particle, the particle index from gl_VertexID, state read
// straight from the sim's ssbo so nothing crosses back over the cpu. curves evaluate here from the
// shared life fraction, since every particle of a burst ages together

struct Particle {
    vec4 pos;
    vec4 vel;
};

layout(std430, binding = 0) buffer Particles {
    Particle particles[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Offset;       // emitter origin relative to the camera
uniform vec3 CamRight;
uniform vec3 CamUp;
uniform float Life;        // 0..1 life fraction shared by the whole burst
uniform vec2 Size;         // start, end
uniform int SizeEase;
uniform vec2 Alpha;        // start, end
uniform int AlphaEase;
uniform vec3 ColorStops[8];
uniform int ColorCount;
uniform int ColorEase;
uniform vec4 UvCell[4];    // one atlas cell per flipbook frame
uniform int Frames;        // 1 when the sprite does not animate

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
    if (ColorCount <= 1) {
        return ColorStops[0];
    }
    float scaled = t * float(ColorCount - 1);
    int i = min(int(scaled), ColorCount - 2);
    return mix(ColorStops[i], ColorStops[i + 1], easeAt(ColorEase, scaled - float(i)));
}

void main() {
    uint particle = uint(gl_VertexID) / 6u;
    uint corner = uint(gl_VertexID) % 6u;
    vec4 state = particles[particle].pos;

    float size = Size.x + (Size.y - Size.x) * easeAt(SizeEase, Life);
    float alpha = Alpha.x + (Alpha.y - Alpha.x) * easeAt(AlphaEase, Life);

    // rotate the unit corner by the particle's roll, then span it on the camera plane
    vec2 c = CORNERS[corner];
    float cs = cos(state.w);
    float sn = sin(state.w);
    vec2 rc = vec2(c.x * cs - c.y * sn, c.x * sn + c.y * cs);
    vec3 world = state.xyz + Offset + (CamRight * rc.x + CamUp * rc.y) * size;
    gl_Position = ProjMat * ModelViewMat * vec4(world, 1.0);

    int frame = min(int(Life * float(Frames)), Frames - 1);
    vec4 cell = UvCell[frame];
    vUv = mix(cell.xy, cell.zw, UVS[corner]);
    vColor = vec4(colorAt(Life), alpha);
}
