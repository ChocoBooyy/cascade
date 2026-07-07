#version 330

// same math as vanilla position_tex_color, so gpu billboards shade exactly like the cpu ones

uniform sampler2D Sampler0;

in vec4 vColor;
in vec2 vUv;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, vUv) * vColor;
}
