package io.github.prcraftmc.striplib.test.lambda;

import io.github.prcraftmc.striplib.test.Client;

import java.util.function.Predicate;

public class OutputClient {
    @Client
    public void testLambda1() {
        final Predicate<String> b = x -> true;
    }





}
