package io.github.prcraftmc.striplib;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class StripData {
    private final boolean entireClass;
    private final Set<Member> fields;
    private final Set<Member> methods;
    private final Set<String> interfaces;
    private final Set<Type> annotations;

    StripData(boolean entireClass, Set<Member> fields, Set<Member> methods, Set<String> interfaces, Set<Type> annotations) {
        this.entireClass = entireClass;
        this.fields = fields;
        this.methods = methods;
        this.interfaces = interfaces;
        this.annotations = annotations;
    }

    public boolean isEmpty() {
        return !entireClass && fields.isEmpty() && methods.isEmpty() && interfaces.isEmpty();
    }

    public boolean stripEntireClass() {
        return entireClass;
    }

    public Set<Member> getMethods() {
        return methods;
    }

    public Set<Member> getFields() {
        return fields;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public ClassVisitor visitor(ClassVisitor delegate) {
        return new ClassVisitor(Opcodes.ASM9, delegate) {
            String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                final Set<String> stripInterfaces = StripData.this.interfaces;
                if (interfaces != null && !stripInterfaces.isEmpty()) {
                    int next = 0;
                    for (int i = 0; i < interfaces.length; i++) {
                        if (!stripInterfaces.contains(interfaces[i])) {
                            interfaces[next++] = interfaces[i];
                        }
                    }
                    if (next < interfaces.length) {
                        interfaces = next > 0 ? Arrays.copyOf(interfaces, next) : null;
                    }
                }
                this.className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                if (annotations.contains(Type.getType(descriptor))) {
                    return null;
                }
                return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (!fields.isEmpty() && fields.contains(new Member(name, Type.getType(descriptor)))) {
                    return null;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!methods.isEmpty() && methods.contains(new Member(name, Type.getMethodType(descriptor)))) {
                    return null;
                }
                if (!fields.isEmpty() && (name.equals("<init>") || name.equals("<clinit>"))) {
                    return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (
                                (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) &&
                                    owner.equals(className) &&
                                    fields.contains(new Member(name, Type.getType(descriptor)))
                            ) {
                                visitInsn(descriptor.equals("J") || descriptor.equals("D") ? Opcodes.POP2 : Opcodes.POP);
                                visitInsn(opcode == Opcodes.PUTFIELD ? Opcodes.POP : Opcodes.NOP);
                                visitInsn(Opcodes.NOP);
                                return;
                            }
                            super.visitFieldInsn(opcode, owner, name, descriptor);
                        }
                    };
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
    }

    public static final class Member {
        @NotNull
        private final String name;
        @NotNull
        private final Type type;

        public Member(@NotNull String name, @NotNull Type type) {
            this.name = Objects.requireNonNull(name, "name");
            this.type = Objects.requireNonNull(type, "type");
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public Type getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Member member = (Member)o;
            return Objects.equals(name, member.name) && Objects.equals(type, member.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            if (type.getSort() == Type.METHOD) {
                return name + type;
            }
            return name + ':' + type;
        }
    }
}
