package dev.chocoboy.cascade.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL32C;

// raw gl program plumbing shared by the shader-owning backends (gpu sim, sdf volumes). throws to the
// caller's fail-soft on any load, compile, or link error
final class GlShaders {

    private GlShaders() {
    }

    static String load(String path) {
        Identifier rl = Identifier.fromNamespaceAndPath("cascade", path);
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(rl)) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("missing gpu shader " + rl, e);
        }
    }

    static int compile(int type, String src) {
        int shader = GL32C.glCreateShader(type);
        GL32C.glShaderSource(shader, src);
        GL32C.glCompileShader(shader);
        if (GL32C.glGetShaderi(shader, GL32C.GL_COMPILE_STATUS) == 0) {
            String log = GL32C.glGetShaderInfoLog(shader);
            GL32C.glDeleteShader(shader);
            throw new IllegalStateException("gpu shader compile failed: " + log);
        }
        return shader;
    }

    static int link(int... shaders) {
        int program = GL32C.glCreateProgram();
        for (int s : shaders) {
            GL32C.glAttachShader(program, s);
        }
        GL32C.glLinkProgram(program);
        for (int s : shaders) {
            GL32C.glDetachShader(program, s);
            GL32C.glDeleteShader(s);
        }
        if (GL32C.glGetProgrami(program, GL32C.GL_LINK_STATUS) == 0) {
            String log = GL32C.glGetProgramInfoLog(program);
            GL32C.glDeleteProgram(program);
            throw new IllegalStateException("gpu program link failed: " + log);
        }
        return program;
    }
}
