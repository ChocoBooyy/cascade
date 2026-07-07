#version 330

#moj_import <minecraft:projection.glsl>

// sphere-traces the smooth union of the effect's shapes along the camera ray, shades the hit by its
// rim factor through the multi-stop gradient, and writes the hit's real depth so world geometry
// occludes the volume correctly (the quad's own depth is meaningless)

layout(std140) uniform SdfConfig {
    mat4 VolumeModelView;
    vec4 CenterBound;     // xyz volume origin relative to the camera, w bounding radius with margin
    vec4 CamRight;
    vec4 CamUp;
    vec4 Params;          // x spin angle, y smooth width, z alpha, w unused
    ivec4 Counts;         // x shapes, y color stops, z color ease
    ivec4 ShapeTypes[8];  // x ordinal of SdfSpec.Type: 0 sphere, 1 box, 2 torus
    vec4 ShapeCenters[8];
    vec4 ShapeSizes[8];
    vec4 ColorStops[8];
};

in vec3 vRay;

out vec4 fragColor;

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
    if (Counts.y <= 1) {
        return ColorStops[0].rgb;
    }
    float scaled = t * float(Counts.y - 1);
    int i = min(int(scaled), Counts.y - 2);
    return mix(ColorStops[i].rgb, ColorStops[i + 1].rgb, easeAt(Counts.z, scaled - float(i)));
}

float sdShape(int i, vec3 p) {
    vec3 q = p - ShapeCenters[i].xyz;
    if (ShapeTypes[i].x == 1) {
        // box rounded by a fixed fraction of its smallest half extent, so edges never render harsh
        vec3 b = ShapeSizes[i].xyz;
        float r = 0.15 * min(b.x, min(b.y, b.z));
        vec3 d = abs(q) - b + vec3(r);
        return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0) - r;
    }
    if (ShapeTypes[i].x == 2) {
        vec2 t = vec2(length(q.xz) - ShapeSizes[i].x, q.y);
        return length(t) - ShapeSizes[i].y;
    }
    return length(q) - ShapeSizes[i].x;
}

// polynomial smooth min; the width never reaches zero, the cpu side clamps it
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float map(vec3 p) {
    float d = sdShape(0, p);
    for (int i = 1; i < Counts.x; i++) {
        d = smin(d, sdShape(i, p), Params.y);
    }
    return d;
}

vec3 normalAt(vec3 p) {
    const vec2 e = vec2(0.002, -0.002);
    return normalize(e.xyy * map(p + e.xyy) + e.yyx * map(p + e.yyx)
            + e.yxy * map(p + e.yxy) + e.xxx * map(p + e.xxx));
}

void main() {
    vec3 center = CenterBound.xyz;
    float bound = CenterBound.w;
    vec3 rd = normalize(vRay);
    // clip the march to the bounding sphere; the ray origin is the camera at zero
    float b = dot(rd, center);
    float c = dot(center, center) - bound * bound;
    float disc = b * b - c;
    if (disc < 0.0) {
        discard;
    }
    float sq = sqrt(disc);
    float tFar = b + sq;
    if (tFar < 0.0) {
        discard;
    }
    float t = max(b - sq, 0.0);

    // march in the volume's unspun local frame
    float cs = cos(Params.x);
    float sn = sin(Params.x);
    mat3 unspin = mat3(cs, 0.0, -sn, 0.0, 1.0, 0.0, sn, 0.0, cs);

    bool hit = false;
    vec3 p;
    for (int i = 0; i < 64; i++) {
        p = unspin * (rd * t - center);
        float d = map(p);
        if (d < 0.002) {
            hit = true;
            break;
        }
        t += d;
        if (t > tFar) {
            break;
        }
    }
    if (!hit) {
        discard;
    }

    vec3 n = normalAt(p);
    vec3 view = unspin * rd;
    // rim factor: 0 facing the camera, 1 at the silhouette, which drives both gradient and glow
    float rim = 1.0 - abs(dot(n, view));
    fragColor = vec4(colorAt(rim) * Params.z * (0.25 + 0.75 * rim), 1.0);

    vec4 clip = ProjMat * VolumeModelView * vec4(rd * t, 1.0);
    gl_FragDepth = clip.z / clip.w * 0.5 + 0.5;
}
