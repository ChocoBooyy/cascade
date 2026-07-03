#version 150

// spans the volume's bounding quad on the camera plane and hands each vertex's camera-relative
// position to the fragment shader, which marches rays through it

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Center;    // volume origin relative to the camera
uniform vec3 CamRight;
uniform vec3 CamUp;
uniform float Bound;    // bounding radius with margin

in vec2 Corner;

out vec3 vRay;

void main() {
    vec3 world = Center + (CamRight * Corner.x + CamUp * Corner.y) * Bound;
    vRay = world;
    gl_Position = ProjMat * ModelViewMat * vec4(world, 1.0);
}
