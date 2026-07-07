#version 330

#moj_import <minecraft:projection.glsl>

// spans the volume's bounding quad on the camera plane and hands each vertex's camera-relative
// position to the fragment shader, which marches rays through it. the corners come from gl_VertexID,
// so the draw binds no vertex buffer, like vanilla's screenquad

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

out vec3 vRay;

const vec2 CORNERS[6] = vec2[](
    vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0),
    vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(1.0, -1.0)
);

void main() {
    vec2 corner = CORNERS[gl_VertexID];
    vec3 world = CenterBound.xyz + (CamRight.xyz * corner.x + CamUp.xyz * corner.y) * CenterBound.w;
    vRay = world;
    gl_Position = ProjMat * VolumeModelView * vec4(world, 1.0);
}
