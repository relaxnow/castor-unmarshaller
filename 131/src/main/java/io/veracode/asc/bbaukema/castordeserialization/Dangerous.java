package io.veracode.asc.bbaukema.castordeserialization;

import java.io.Serializable;

public class Dangerous implements Serializable {
    public void Dangerous() {
        System.out.println("DANGER!");
    }
}
