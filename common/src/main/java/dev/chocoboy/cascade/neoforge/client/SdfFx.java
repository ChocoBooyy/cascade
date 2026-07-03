package dev.chocoboy.cascade.neoforge.client;

import org.lwjgl.opengl.GL32C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// the sdf volume backend: one raymarching program and one shared corner quad. gl 3.2 is minecraft's
// floor, so unlike the gpu sim there is no capability gate; any gl failure still disables sdf volumes
// for the session and live effects remove themselves
final class SdfFx {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    static final int MAX_COLOR_STOPS = 8;

    private static boolean failed;
    private static boolean built;
    static int program;
    static int vao;
    private static int vbo;

    private SdfFx() {
    }

    // lazily builds the program and quad on first use; throws to the caller's fail-soft on any gl error
    static void ensureBuilt() {
        if (built) {
            return;
        }
        program = GlShaders.link(
                GlShaders.compile(GL32C.GL_VERTEX_SHADER, GlShaders.load("shaders/sdf/volume.vert")),
                GlShaders.compile(GL32C.GL_FRAGMENT_SHADER, GlShaders.load("shaders/sdf/volume.frag")));
        vao = GL32C.glGenVertexArrays();
        vbo = GL32C.glGenBuffers();
        GL32C.glBindVertexArray(vao);
        GL32C.glBindBuffer(GL32C.GL_ARRAY_BUFFER, vbo);
        GL32C.glBufferData(GL32C.GL_ARRAY_BUFFER, new float[] {
                -1f, -1f, -1f, 1f, 1f, 1f,
                -1f, -1f, 1f, 1f, 1f, -1f,
        }, GL32C.GL_STATIC_DRAW);
        // glsl 150 has no explicit attrib locations, so the linker picks one; ask it
        int corner = GL32C.glGetAttribLocation(program, "Corner");
        GL32C.glEnableVertexAttribArray(corner);
        GL32C.glVertexAttribPointer(corner, 2, GL32C.GL_FLOAT, false, 0, 0L);
        GL32C.glBindBuffer(GL32C.GL_ARRAY_BUFFER, 0);
        GL32C.glBindVertexArray(0);
        built = true;
    }

    // permanently drops sdf volumes for the session; the rest of the library is unaffected
    static void fail(String what, RuntimeException e) {
        if (!failed) {
            failed = true;
            LOGGER.error("Cascade sdf rendering failed while {}, disabling sdf volumes", what, e);
        }
    }

    static boolean failed() {
        return failed;
    }
}
