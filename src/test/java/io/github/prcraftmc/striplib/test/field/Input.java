package io.github.prcraftmc.striplib.test.field;

import io.github.prcraftmc.striplib.test.Client;
import io.github.prcraftmc.striplib.test.Server;

public class Input {
    public float field1;

    @Server
    public int field2;

    @Client
    public String field3;
}
