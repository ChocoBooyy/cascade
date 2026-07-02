package dev.chocoboy.cascade.neoforge.shader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

// Mirrors the contract Minecraft's ShaderInstance enforces when it loads a core shader, so a rename or
// a missing file fails the build instead of only surfacing on a real client launch. runGameTest is a
// dedicated server and never compiles these, which is how the soft particle sampler bugs slipped through.
class ShaderAssetTest {

    private static final Path CORE = locate("src/main/resources/assets/cascade/shaders/core");
    // the render code that calls setSampler lives in :common now, so scan both loader and shared sources
    private static final List<Path> JAVA_ROOTS = javaRoots();

    // uniform <type> <name>;  (also matches "uniform sampler2D DepthSampler;")
    private static final Pattern UNIFORM = Pattern.compile("\\buniform\\s+(\\w+)\\s+(\\w+)\\s*;");
    private static final Pattern SET_SAMPLER = Pattern.compile("setSampler\\(\\s*\"(\\w+)\"");

    @Test
    void everyCoreShaderMatchesItsGlsl() {
        List<Path> jsons = coreJsons();
        assertTrue(jsons.size() >= 2, "expected the soft core shaders under " + CORE);
        List<String> problems = new ArrayList<>();
        for (Path json : jsons) {
            checkShader(json, problems);
        }
        if (!problems.isEmpty()) {
            fail(String.join("\n", problems));
        }
    }

    @Test
    void everySetSamplerNamesADeclaredSampler() {
        Set<String> declared = new HashSet<>();
        for (Path json : coreJsons()) {
            declared.addAll(names(parse(json), "samplers"));
        }
        List<String> problems = new ArrayList<>();
        for (Path java : javaSources()) {
            Matcher m = SET_SAMPLER.matcher(read(java));
            while (m.find()) {
                String name = m.group(1);
                // Sampler0/1/2 are vanilla texture units bound by the render type, not our json samplers
                if (name.matches("Sampler\\d+") || declared.contains(name)) {
                    continue;
                }
                problems.add(java.getFileName() + ": setSampler(\"" + name + "\") is not declared in any core shader");
            }
        }
        if (!problems.isEmpty()) {
            fail(String.join("\n", problems));
        }
    }

    private void checkShader(Path json, List<String> problems) {
        String name = stripExt(json.getFileName().toString());
        JsonObject root = parse(json);
        Path vsh = program(root, "vertex", ".vsh");
        Path fsh = program(root, "fragment", ".fsh");
        if (!Files.exists(vsh)) {
            problems.add(name + ": missing vertex source " + vsh.getFileName());
            return;
        }
        if (!Files.exists(fsh)) {
            problems.add(name + ": missing fragment source " + fsh.getFileName());
            return;
        }

        Set<String> glslUniforms = new LinkedHashSet<>();
        Set<String> glslSamplers = new LinkedHashSet<>();
        collectUniforms(read(vsh), glslUniforms, glslSamplers);
        collectUniforms(read(fsh), glslUniforms, glslSamplers);

        Set<String> jsonUniforms = names(root, "uniforms");
        Set<String> jsonSamplers = names(root, "samplers");

        // a uniform the glsl reads but the json never feeds stays at zero at runtime, the class of bug
        // that made FadeDistance a no-op
        Set<String> declared = new LinkedHashSet<>(jsonUniforms);
        declared.addAll(jsonSamplers);
        for (String u : glslUniforms) {
            if (!declared.contains(u)) {
                problems.add(name + ": glsl uniform '" + u + "' is not declared in the json");
            }
        }
        for (String s : glslSamplers) {
            if (!jsonSamplers.contains(s)) {
                problems.add(name + ": glsl sampler '" + s + "' is not declared in the json samplers");
            }
        }
        for (String s : jsonSamplers) {
            if (!glslSamplers.contains(s)) {
                problems.add(name + ": json sampler '" + s + "' is never used by the glsl");
            }
        }
        for (String u : jsonUniforms) {
            if (!glslUniforms.contains(u)) {
                problems.add(name + ": json uniform '" + u + "' is never used by the glsl");
            }
        }
    }

    private static void collectUniforms(String glsl, Set<String> uniforms, Set<String> samplers) {
        for (String line : glsl.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//")) {
                continue;
            }
            Matcher m = UNIFORM.matcher(trimmed);
            while (m.find()) {
                String type = m.group(1);
                String uName = m.group(2);
                if (type.startsWith("sampler")) {
                    samplers.add(uName);
                } else {
                    uniforms.add(uName);
                }
            }
        }
    }

    private Path program(JsonObject root, String field, String ext) {
        String ref = root.get(field).getAsString();       // "cascade:cascade_soft_lit"
        String base = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return CORE.resolve(base + ext);
    }

    private static Set<String> names(JsonObject root, String array) {
        Set<String> out = new LinkedHashSet<>();
        if (!root.has(array)) {
            return out;
        }
        JsonArray arr = root.getAsJsonArray(array);
        for (JsonElement e : arr) {
            out.add(e.getAsJsonObject().get("name").getAsString());
        }
        return out;
    }

    private List<Path> coreJsons() {
        try (Stream<Path> s = Files.list(CORE)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Path> javaSources() {
        List<Path> out = new ArrayList<>();
        for (Path root : JAVA_ROOTS) {
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    private static JsonObject parse(Path json) {
        return JsonParser.parseString(read(json)).getAsJsonObject();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String stripExt(String file) {
        int dot = file.lastIndexOf('.');
        return dot < 0 ? file : file.substring(0, dot);
    }

    // gradle runs the test with the module dir as the working dir, but fall back a level so it also
    // works when a runner launches from the repo root
    private static Path locate(String relative) {
        Path direct = Path.of(relative);
        if (Files.exists(direct)) {
            return direct;
        }
        return Path.of("neoforge").resolve(relative);
    }

    // this loader's sources plus the shared :common sources, wherever the cwd sits
    private static List<Path> javaRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(locate("src/main/java"));
        for (Path candidate : List.of(Path.of("../common/src/main/java"), Path.of("common/src/main/java"))) {
            if (Files.exists(candidate)) {
                roots.add(candidate);
                break;
            }
        }
        return roots;
    }
}
