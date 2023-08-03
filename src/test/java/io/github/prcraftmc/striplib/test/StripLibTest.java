package io.github.prcraftmc.striplib.test;

import io.github.prcraftmc.striplib.ClassStripper;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class StripLibTest {
    private static final ClassStripper.Builder FACTORY = ClassStripper.builder()
        .annotation("client", Client.class, "stripLambdas")
        .annotation("server", Server.class, "stripLambdas");

    @Test
    public void simple() {
        performTest("simple", true, true);
    }

    @Test
    public void lambda() {
        performTest("lambda", true, false);
    }

    @Test
    public void entire() {
        assertTrue(FACTORY.build("server")
            .calcStripData(new ClassReader(read("io/github/prcraftmc/striplib/test/entire/Input")), 0)
            .stripEntireClass()
        );
    }

    private void performTest(String name, boolean client, boolean server) {
        final String packagePath = "io/github/prcraftmc/striplib/test/" + name + '/';
        final ClassReader reader = new ClassReader(read(packagePath + "Input"));
        if (client) {
            performTestInEnv(packagePath, reader, "client", "OutputClient");
        }
        if (server) {
            performTestInEnv(packagePath, reader, "server", "OutputServer");
        }
    }

    private void performTestInEnv(String packagePath, ClassReader reader, String env, String outputPath) {
        final StringWriter testOutput = new StringWriter();
        FACTORY.build(env).strip(reader, 0, new TraceClassVisitor(new PrintWriter(testOutput)));

        final String expectOutput = becomeInput(packagePath, outputPath);
        assertEquals(expectOutput, testOutput.toString());

        System.out.println("Test: " + env + " env for " + packagePath);
        System.out.println(expectOutput);
        System.out.println();
    }

    private String becomeInput(String packagePath, String className) {
        final ClassReader reader = new ClassReader(read(packagePath + className));
        final StringWriter result = new StringWriter();
        reader.accept(new ClassRemapper(
            new ClassVisitor(Opcodes.ASM9, new TraceClassVisitor(new PrintWriter(result))) {
                @Override
                public void visitSource(String source, String debug) {
                    super.visitSource("Input.java", debug);
                }
            },
            new SimpleRemapper(packagePath + className, packagePath + "Input")
        ), 0);
        return result.toString();
    }

    private byte[] read(String path) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (InputStream is = StripLibTest.class.getResourceAsStream('/' + path + ".class")) {
            assertNotNull(is, "Class file for " + path + " is missing");
            final byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                result.write(buf, 0, n);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result.toByteArray();
    }
}
