#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

// vertex stage for lit solid mesh particles: vanilla dropped its position_color_lightmap shader in 26.1,
// so this vendors the same idea, folding the lightmap tint into the vertex color like the particle shader

in vec3 Position;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color * sample_lightmap(Sampler2, UV2);
}
