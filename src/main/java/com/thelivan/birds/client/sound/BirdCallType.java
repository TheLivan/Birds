package com.thelivan.birds.client.sound;

public enum BirdCallType {

    SINGLE("single"),
    FLOCK("flock");

    /**
     * Used both as the resource sub-folder ({@code assets/birds/sounds/<species>/<subdir>/...}) and as the
     * sounds.json event suffix ({@code <species>.<subdir>}).
     */
    public final String subdir;

    BirdCallType(String subdir) {
        this.subdir = subdir;
    }
}
