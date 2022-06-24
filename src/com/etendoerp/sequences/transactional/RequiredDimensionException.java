package com.etendoerp.sequences.transactional;

public class RequiredDimensionException extends Exception {
    private static final long serialVersionUID = 4461884121608343642L;

    private final String requiredDimension;

    public RequiredDimensionException(String requiredDimension) {
        this.requiredDimension = requiredDimension;
    }

    public String getRequiredDimension() {
        return requiredDimension;
    }
}
