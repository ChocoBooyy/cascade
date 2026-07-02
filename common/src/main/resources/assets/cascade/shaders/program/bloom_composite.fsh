#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;

in vec2 texCoord;

out vec4 fragColor;

// scene plus blurred brights. done offscreen because a post pass clears its output target, so adding
// straight onto the main target would erase the frame first
void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    fragColor = vec4(scene + bloom, 1.0);
}
