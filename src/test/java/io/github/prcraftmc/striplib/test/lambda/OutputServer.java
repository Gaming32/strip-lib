package io.github.prcraftmc.striplib.test.lambda;


import io.github.prcraftmc.striplib.test.Server;

import java.util.function.BooleanSupplier;
import java.util.function.Function;


public class OutputServer {





    @Server
    public void testLambda2() {
        final Function<String, BooleanSupplier> b = x -> () -> true;
    }
}
