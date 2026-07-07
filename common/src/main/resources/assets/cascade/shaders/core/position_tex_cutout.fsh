#version 330

// vanilla 26.1 position_tex_color with the pre-26.1 alpha cutout restored: the old shader discarded
// below 0.1 where the new one only discards exact zero, and cascade's soft sprites rely on that
// threshold to carve their silhouette out of the quad under one-one additive blending

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color * ColorModulator;
}
