package io.github.prcraftmc.striplib;

import org.objectweb.asm.*;

import java.lang.invoke.LambdaMetafactory;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClassStripper extends ClassVisitor {
    private static final String LAMBDA_CLASS_NAME = Type.getInternalName(LambdaMetafactory.class);
    private static final String LAMBDA_METHOD_DESCRIPTOR =
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

    private final Map<Type, AnnotationData> annotations;

    private boolean complete;
    private String className, superName;
    private String[] interfaces;

    private boolean stripEntireClass;
    private final Set<StripData.Member> stripFields = new HashSet<>();
    private final Set<StripData.Member> stripMethods = new HashSet<>();
    private final Map<StripData.Member, AnnotationData> toCheckForLambdas = new HashMap<>();
    private final Set<String> stripInterfaces = new HashSet<>();

    private ClassStripper(ClassVisitor delegate, Map<Type, AnnotationData> annotations) {
        super(Opcodes.ASM9, delegate);
        this.annotations = annotations;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (complete) {
            throw new IllegalStateException("ClassStripper instance may only be used once. If you wish to use your config multiple times, hold onto a ClassStripper.Builder instance as a factory.");
        }
        this.className = name;
        this.superName = superName;
        this.interfaces = interfaces != null ? Arrays.copyOf(interfaces, interfaces.length) : null;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (annotations.containsKey(Type.getType(descriptor))) {
            stripEntireClass = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        final TypeReference ref = new TypeReference(typeRef);
        if (ref.getSort() == TypeReference.CLASS_EXTENDS && annotations.containsKey(Type.getType(descriptor))) {
            if (ref.getSuperTypeIndex() == -1) {
                throw new IllegalArgumentException("Cannot strip superclass " + superName + " from class " + className);
            } else {
                stripInterfaces.add(interfaces[ref.getSuperTypeIndex()]);
            }
        }
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String fieldDescriptor, String signature, Object value) {
        return new FieldVisitor(api, super.visitField(access, name, fieldDescriptor, signature, value)) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (annotations.containsKey(Type.getType(descriptor))) {
                    stripFields.add(new StripData.Member(name, Type.getType(fieldDescriptor)));
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature, String[] exceptions) {
        return new MethodVisitor(api, super.visitMethod(access, name, methodDescriptor, signature, exceptions)) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                final AnnotationData annotation = annotations.get(Type.getType(descriptor));
                if (annotation != null) {
                    final StripData.Member member = new StripData.Member(name, Type.getMethodType(methodDescriptor));
                    stripMethods.add(member);
                    if (annotation.stripLambdasKey != null) {
                        return new AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
                            boolean hasFoundParam = false;

                            @Override
                            public void visit(String name, Object value) {
                                if (name.equals(annotation.stripLambdasKey)) {
                                    hasFoundParam = true;
                                    if ((boolean)value) {
                                        toCheckForLambdas.put(member, annotation);
                                    }
                                }
                                super.visit(name, value);
                            }

                            @Override
                            public void visitEnd() {
                                if (!hasFoundParam && annotation.defaultStripLambdas) {
                                    toCheckForLambdas.put(member, annotation);
                                }
                                super.visitEnd();
                            }
                        };
                    } else if (annotation.defaultStripLambdas) {
                        toCheckForLambdas.put(member, annotation);
                    }
                }
                return super.visitAnnotation(descriptor, visible);
            }
        };
    }

    @Override
    public void visitEnd() {
        complete = true;
        super.visitEnd();
    }

    public boolean needsLambdaStripping() {
        if (!complete) {
            throw new IllegalStateException("Cannot call needsLambdaStripping() on an incomplete ClassStripper");
        }
        return !toCheckForLambdas.isEmpty();
    }

    public ClassVisitor findLambdasToStrip() {
        return findLambdasToStrip(null);
    }

    public ClassVisitor findLambdasToStrip(ClassVisitor delegate) {
        return new ClassVisitor(api, delegate) {
            private boolean closed;
            private String className;

            private final Map<StripData.Member, AnnotationData> additionalToStrip = new HashMap<>();
            private final Map<StripData.Member, AnnotationData> additionalToNotStrip = new HashMap<>();

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (closed) {
                    throw new IllegalStateException("Cannot reuse ClassStripper.findLambdasToStrip visitor");
                }
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                final AnnotationData origin = toCheckForLambdas.remove(
                    new StripData.Member(name, Type.getMethodType(descriptor))
                );
                final Map<StripData.Member, AnnotationData> addTo = origin != null ? additionalToStrip : additionalToNotStrip;
                return new MethodVisitor(api, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        if (bootstrapMethodArguments.length != 3) return;
                        if (!(bootstrapMethodArguments[1] instanceof Handle)) return;
                        if (bootstrapMethodHandle.getTag() != Opcodes.H_INVOKESTATIC) return;
                        if (!bootstrapMethodHandle.getName().equals("metafactory")) return;
                        if (!bootstrapMethodHandle.getOwner().equals(LAMBDA_CLASS_NAME)) return;
                        if (!bootstrapMethodHandle.getDesc().equals(LAMBDA_METHOD_DESCRIPTOR)) return;
                        final Handle lambdaTarget = (Handle)bootstrapMethodArguments[1];
                        if (lambdaTarget.getOwner().equals(className)) {
                            addTo.put(
                                new StripData.Member(lambdaTarget.getName(), Type.getMethodType(lambdaTarget.getDesc())),
                                origin
                            );
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                closed = true;
                additionalToStrip.keySet().removeAll(additionalToNotStrip.keySet());
                stripMethods.addAll(additionalToStrip.keySet());
                toCheckForLambdas.putAll(additionalToStrip);
                super.visitEnd();
            }
        };
    }

    public StripData getResult() {
        if (!complete) {
            throw new IllegalStateException("Cannot call getResult() on an incomplete ClassStripper");
        }
        if (!toCheckForLambdas.isEmpty()) {
            throw new IllegalStateException("Must visit class with findLambdasToStrip() while needsLambdaStripping() returns true");
        }
        return new StripData(stripEntireClass, stripFields, stripMethods, stripInterfaces, annotations.keySet());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<AnnotationData> annotations = new ArrayList<>();
        private boolean defaultStripLambdas = true;

        private Builder() {
        }

        public Builder annotation(String environment, Class<?> annotation) {
            return annotation(environment, annotation, null);
        }

        public Builder annotation(String environment, Class<?> annotation, String stripLambdasKey) {
            annotations.add(new AnnotationData(environment, annotation, stripLambdasKey, defaultStripLambdas));
            return this;
        }

        public Builder defaultStripLambdas(boolean defaultStripLambdas) {
            this.defaultStripLambdas = defaultStripLambdas;
            return this;
        }

        public ClassStripper build(String environment) {
            return build(environment, null);
        }

        public ClassStripper build(String environment, ClassVisitor delegate) {
            return new ClassStripper(
                delegate,
                annotations.stream()
                    .filter(a -> !a.environment.equals(environment))
                    .collect(Collectors.toMap(AnnotationData::getAnnotation, Function.identity()))
            );
        }
    }
}
