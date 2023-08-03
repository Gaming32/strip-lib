package io.github.prcraftmc.striplib.test.lambda;

import io.github.prcraftmc.striplib.test.Client;
import io.github.prcraftmc.striplib.test.Server;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public class Input {
    @Client
    public void testLambda1() {
        final Predicate<String> b = x -> true;
    }

    @Server
    public void testLambda2() {
        final Function<String, BooleanSupplier> b = x -> () -> true;
    }
}
