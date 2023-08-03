package io.github.prcraftmc.striplib.test.simple;

import io.github.prcraftmc.striplib.test.Client;
import io.github.prcraftmc.striplib.test.Server;

import java.io.Serializable;

public class Input implements @Server Serializable {
    @Client
    public void method1() {
    }

    @Server
    public void method2() {
    }

    public void method3() {
    }
}
