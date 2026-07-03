package dev.chocoboy.cascade.client;

import dev.chocoboy.cascade.engine.effect.AttractorSpec;
import dev.chocoboy.cascade.engine.effect.BlendMode;
import dev.chocoboy.cascade.engine.effect.ComponentSpec;
import dev.chocoboy.cascade.engine.effect.DragSpec;
import dev.chocoboy.cascade.engine.effect.EmissionSpec;
import dev.chocoboy.cascade.engine.effect.EmitterSpec;
import dev.chocoboy.cascade.engine.effect.GravitySpec;
import dev.chocoboy.cascade.engine.effect.MeshId;
import dev.chocoboy.cascade.engine.effect.VortexSpec;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// the gpu particle backend: capability gate, shader programs, and the eligibility rules that decide
// whether a spec runs here or on the cpu sim. opt-in and off by default; the cpu path stays the
// reference. any gl failure disables the backend for the session and everything falls back silently
public final class GpuSim {

    private static final Logger LOGGER = LoggerFactory.getLogger("Cascade");

    // uniform array bounds in the shaders
    static final int MAX_FORCES = 8;
    static final int MAX_COLOR_STOPS = 8;

    private static boolean enabled;
    private static boolean failed;
    private static Boolean capable;
    private static boolean built;
    static int computeProgram;
    static int drawProgram;
    // vertex pulling reads the ssbo by gl_VertexID, so one empty vao serves every draw
    static int vao;

    private GpuSim() {
    }

    public static void setEnabled(boolean on) {
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    // whether this machine can run the backend at all; only meaningful on the render thread
    public static boolean available() {
        if (failed) {
            return false;
        }
        if (capable == null) {
            capable = GL.getCapabilities().OpenGL43;
        }
        return capable;
    }

    static boolean shouldRun(EmitterSpec spec) {
        return enabled && !failed && eligible(spec) && available();
    }

    // the honest v1 cut: stateless additive billboard bursts only. anything the compute shader does not
    // mirror exactly routes to the cpu sim instead of getting a lookalike
    static boolean eligible(EmitterSpec spec) {
        if (spec.emission().mode() != EmissionSpec.Mode.BURST || spec.count() < 1) {
            return false;
        }
        if (spec.render().blend() != BlendMode.ADDITIVE || spec.render().mesh() != MeshId.NONE
                || spec.render().lit() || spec.render().soft() || spec.render().stretch() != 0f) {
            return false;
        }
        if (spec.trail().enabled() || spec.subEmitter() != null || spec.collision().enabled()) {
            return false;
        }
        if (spec.modifiers().size() > MAX_FORCES || spec.color().stops().size() > MAX_COLOR_STOPS) {
            return false;
        }
        for (ComponentSpec c : spec.modifiers()) {
            if (!(c instanceof GravitySpec || c instanceof DragSpec
                    || c instanceof AttractorSpec || c instanceof VortexSpec)) {
                return false;
            }
        }
        return true;
    }

    // lazily builds both programs on first use; throws to the caller's fail-soft on any gl error
    static void ensureBuilt() {
        if (built) {
            return;
        }
        computeProgram = GlShaders.link(
                GlShaders.compile(GL43C.GL_COMPUTE_SHADER, GlShaders.load("shaders/gpu/particle.comp")));
        drawProgram = GlShaders.link(
                GlShaders.compile(GL43C.GL_VERTEX_SHADER, GlShaders.load("shaders/gpu/particle.vert")),
                GlShaders.compile(GL43C.GL_FRAGMENT_SHADER, GlShaders.load("shaders/gpu/particle.frag")));
        vao = GL43C.glGenVertexArrays();
        built = true;
    }

    // permanently drops to the cpu path; live gpu effects keep no-op ticking until their lifetime ends
    static void fail(String what, RuntimeException e) {
        if (!failed) {
            failed = true;
            enabled = false;
            LOGGER.error("Cascade gpu sim failed while {}, falling back to the cpu sim", what, e);
        }
    }

    static boolean failed() {
        return failed;
    }

}
