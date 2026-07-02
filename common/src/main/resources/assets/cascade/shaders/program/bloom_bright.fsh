#version 150

uniform sampler2D DiffuseSampler;

uniform float Threshold;

in vec2 texCoord;

out vec4 fragColor;

// keep only the bright part of the scene, scaled up from the threshold so pixels just over it fade
// in instead of popping
void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    float bright = clamp((luma - Threshold) / max(1.0 - Threshold, 0.001), 0.0, 1.0);
    fragColor = vec4(color.rgb * bright, 1.0);
}
