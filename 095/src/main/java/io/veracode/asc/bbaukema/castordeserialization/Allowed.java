package io.veracode.asc.bbaukema.castordeserialization;

import java.io.Serializable;

public class Allowed implements Serializable {
    public Dangerous dangerous;

    public Dangerous getDangerous() {
        return dangerous;
    }

    public void Allowed() {
        System.out.println("Creating Allowed class");
    }
}
