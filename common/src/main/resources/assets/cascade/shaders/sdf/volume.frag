#version 150

// sphere-traces the smooth union of the effect's shapes along the camera ray, shades the hit by its
// rim factor through the multi-stop gradient, and writes the hit's real depth so world geometry
// occludes the volume correctly (the quad's own depth is meaningless)

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Center;      // volume origin relative to the camera
uniform float Bound;
uniform float Angle;      // current spin around the volume's y axis
uniform int ShapeCount;
uniform int ShapeType[8]; // ordinals of SdfSpec.Type: 0 sphere, 1 box, 2 torus
uniform vec3 ShapeCenter[8];
uniform vec3 ShapeSize[8];
uniform float Smooth;
uniform vec3 ColorStops[8];
uniform int ColorCount;
uniform int ColorEase;
uniform float Alpha;

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
    if (ColorCount <= 1) {
        return ColorStops[0];
    }
    float scaled = t * float(ColorCount - 1);
    int i = min(int(scaled), ColorCount - 2);
    return mix(ColorStops[i], ColorStops[i + 1], easeAt(ColorEase, scaled - float(i)));
}

float sdShape(int i, vec3 p) {
    vec3 q = p - ShapeCenter[i];
    if (ShapeType[i] == 1) {
        // box rounded by a fixed fraction of its smallest half extent, so edges never render harsh
        vec3 b = ShapeSize[i];
        float r = 0.15 * min(b.x, min(b.y, b.z));
        vec3 d = abs(q) - b + vec3(r);
        return length(max(d, 0.0)) + min(max(d.x, max(d.y, d.z)), 0.0) - r;
    }
    if (ShapeType[i] == 2) {
        vec2 t = vec2(length(q.xz) - ShapeSize[i].x, q.y);
        return length(t) - ShapeSize[i].y;
    }
    return length(q) - ShapeSize[i].x;
}

// polynomial smooth min; the width never reaches zero, the cpu side clamps it
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float map(vec3 p) {
    float d = sdShape(0, p);
    for (int i = 1; i < ShapeCount; i++) {
        d = smin(d, sdShape(i, p), Smooth);
    }
    return d;
}

vec3 normalAt(vec3 p) {
    const vec2 e = vec2(0.002, -0.002);
    return normalize(e.xyy * map(p + e.xyy) + e.yyx * map(p + e.yyx)
            + e.yxy * map(p + e.yxy) + e.xxx * map(p + e.xxx));
}

void main() {
    vec3 rd = normalize(vRay);
    // clip the march to the bounding sphere; the ray origin is the camera at zero
    float b = dot(rd, Center);
    float c = dot(Center, Center) - Bound * Bound;
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
    float cs = cos(Angle);
    float sn = sin(Angle);
    mat3 unspin = mat3(cs, 0.0, -sn, 0.0, 1.0, 0.0, sn, 0.0, cs);

    bool hit = false;
    vec3 p;
    for (int i = 0; i < 64; i++) {
        p = unspin * (rd * t - Center);
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
    fragColor = vec4(colorAt(rim) * Alpha * (0.25 + 0.75 * rim), 1.0);

    vec4 clip = ProjMat * ModelViewMat * vec4(rd * t, 1.0);
    gl_FragDepth = clip.z / clip.w * 0.5 + 0.5;
}
