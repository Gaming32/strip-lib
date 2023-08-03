package io.github.prcraftmc.striplib;

import org.objectweb.asm.Type;

import java.util.Objects;

class AnnotationData {
    final String environment;
    final Type annotation;
    final String stripLambdasKey;
    final boolean defaultStripLambdas;

    AnnotationData(String environment, Type annotation, String stripLambdasKey, boolean defaultStripLambdas) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.annotation = Objects.requireNonNull(annotation, "annotation");
        this.stripLambdasKey = stripLambdasKey;
        this.defaultStripLambdas = defaultStripLambdas;
    }

    Type getAnnotation() {
        return annotation;
    }
}
