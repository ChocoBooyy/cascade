#version 150

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;
uniform vec4 ColorModulator;
uniform mat4 ProjMat;
uniform float FadeDistance;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// turn a non linear depth buffer value into a positive view space distance using the projection matrix
float linearize(float depth) {
    float ndc = depth * 2.0 - 1.0;
    return ProjMat[3][2] / (ProjMat[2][2] + ndc);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    ivec2 px = ivec2(gl_FragCoord.xy);
    float sceneDepth = texelFetch(DepthSampler, px, 0).r;
    float scene = linearize(sceneDepth);
    float here = linearize(gl_FragCoord.z);
    float soft = clamp((scene - here) / FadeDistance, 0.0, 1.0);
    fragColor = vec4(tex.rgb, tex.a * soft);
    if (fragColor.a < 0.001) discard;
}
