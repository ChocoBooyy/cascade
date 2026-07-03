package dev.chocoboy.cascade.testmod;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL43C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// spike for the gpu sim: proves the whole compute path end to end before any real design lands on it.
// one ssbo of particles, a compute dispatch advancing them every frame, and a vertex-pulling point draw
// straight from the same buffer, so nothing crosses back to the cpu after init. the fountain shape makes
// gpu-side motion unmistakable: if the points arc and respawn, the compute shader is doing the work
public final class GpuSimSpike {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");
    private static final int COUNT = 65536;
    private static final int GROUP_SIZE = 64;
    // std430: vec4 pos + vec4 vel per particle
    private static final int FLOATS_PER_PARTICLE = 8;

    private static final String COMPUTE_SRC = """
            #version 430
            layout(local_size_x = 64) in;
            struct P { vec4 pos; vec4 vel; };
            layout(std430, binding = 0) buffer Particles { P p[]; };
            void main() {
                uint i = gl_GlobalInvocationID.x;
                P q = p[i];
                q.vel.y -= 0.0008;
                q.pos.xyz += q.vel.xyz;
                if (q.pos.y < 0.0) {
                    q.pos.xyz = vec3(0.0);
                    q.vel.y = q.vel.w;
                }
                p[i] = q;
            }
            """;

    private static final String VERTEX_SRC = """
            #version 430
            struct P { vec4 pos; vec4 vel; };
            layout(std430, binding = 0) buffer Particles { P p[]; };
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            uniform vec3 Offset;
            out float height;
            void main() {
                vec3 pos = p[gl_VertexID].pos.xyz + Offset;
                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                gl_PointSize = 4.0;
                height = p[gl_VertexID].pos.y;
            }
            """;

    private static final String FRAGMENT_SRC = """
            #version 430
            in float height;
            out vec4 color;
            void main() {
                color = vec4(1.0, 0.35 + 0.2 * height, 0.15, 1.0);
            }
            """;

    private static boolean active;
    private static boolean failed;
    private static boolean built;
    private static int computeProgram;
    private static int drawProgram;
    private static int ssbo;
    private static int vao;
    private static int uModelView;
    private static int uProj;
    private static int uOffset;
    private static Vec3 origin = Vec3.ZERO;

    private GpuSimSpike() {
    }

    // returns the chat line for the command; the origin is where the fountain sits in the world
    static String toggle(Vec3 at) {
        if (failed) {
            return "gpu spike failed earlier, see log";
        }
        if (!GL.getCapabilities().OpenGL43) {
            return "no gl 4.3 on this machine";
        }
        active = !active;
        if (active) {
            origin = at;
            return "gpu spike on: " + COUNT + " particles";
        }
        return "gpu spike off";
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (!active || failed || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        try {
            if (!built) {
                init();
                built = true;
            }
            dispatchAndDraw();
        } catch (RuntimeException e) {
            failed = true;
            active = false;
            LOGGER.error("Cascade gpu spike failed, disabled", e);
        }
    }

    private static void init() {
        computeProgram = link(compile(GL43C.GL_COMPUTE_SHADER, COMPUTE_SRC));
        drawProgram = link(compile(GL43C.GL_VERTEX_SHADER, VERTEX_SRC),
                compile(GL43C.GL_FRAGMENT_SHADER, FRAGMENT_SRC));
        uModelView = GL43C.glGetUniformLocation(drawProgram, "ModelViewMat");
        uProj = GL43C.glGetUniformLocation(drawProgram, "ProjMat");
        uOffset = GL43C.glGetUniformLocation(drawProgram, "Offset");

        // launch velocities are the only cpu-authored data; vel.w keeps the launch speed for the respawn
        FloatBuffer data = BufferUtils.createFloatBuffer(COUNT * FLOATS_PER_PARTICLE);
        Random rng = new Random(42L);
        for (int i = 0; i < COUNT; i++) {
            float vy = 0.03f + rng.nextFloat() * 0.06f;
            data.put(0f).put(0f).put(0f).put(1f);
            data.put((rng.nextFloat() - 0.5f) * 0.02f).put(vy)
                    .put((rng.nextFloat() - 0.5f) * 0.02f).put(vy);
        }
        data.flip();
        ssbo = GL43C.glGenBuffers();
        GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL43C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, data, GL43C.GL_DYNAMIC_COPY);
        GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);

        // vertex pulling reads the ssbo by gl_VertexID, so the vao stays empty; core profile still wants one
        vao = GL43C.glGenVertexArrays();
    }

    private static void dispatchAndDraw() {
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, ssbo);
        GL43C.glUseProgram(computeProgram);
        GL43C.glDispatchCompute(COUNT / GROUP_SIZE, 1, 1);
        GL43C.glMemoryBarrier(GL43C.GL_SHADER_STORAGE_BARRIER_BIT);

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        GL43C.glUseProgram(drawProgram);
        GL43C.glUniformMatrix4fv(uModelView, false, RenderSystem.getModelViewMatrix().get(new float[16]));
        GL43C.glUniformMatrix4fv(uProj, false, RenderSystem.getProjectionMatrix().get(new float[16]));
        GL43C.glUniform3f(uOffset, (float) (origin.x - cam.x), (float) (origin.y - cam.y),
                (float) (origin.z - cam.z));
        GL43C.glBindVertexArray(vao);
        GL43C.glEnable(GL43C.GL_PROGRAM_POINT_SIZE);
        GL43C.glDrawArrays(GL43C.GL_POINTS, 0, COUNT);
        GL43C.glDisable(GL43C.GL_PROGRAM_POINT_SIZE);
        GL43C.glBindVertexArray(0);
        GL43C.glUseProgram(0);
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
    }

    private static int compile(int type, String src) {
        int shader = GL43C.glCreateShader(type);
        GL43C.glShaderSource(shader, src);
        GL43C.glCompileShader(shader);
        if (GL43C.glGetShaderi(shader, GL43C.GL_COMPILE_STATUS) == 0) {
            String log = GL43C.glGetShaderInfoLog(shader);
            GL43C.glDeleteShader(shader);
            throw new IllegalStateException("shader compile failed: " + log);
        }
        return shader;
    }

    private static int link(int... shaders) {
        int program = GL43C.glCreateProgram();
        for (int s : shaders) {
            GL43C.glAttachShader(program, s);
        }
        GL43C.glLinkProgram(program);
        for (int s : shaders) {
            GL43C.glDetachShader(program, s);
            GL43C.glDeleteShader(s);
        }
        if (GL43C.glGetProgrami(program, GL43C.GL_LINK_STATUS) == 0) {
            String log = GL43C.glGetProgramInfoLog(program);
            GL43C.glDeleteProgram(program);
            throw new IllegalStateException("program link failed: " + log);
        }
        return program;
    }
}
