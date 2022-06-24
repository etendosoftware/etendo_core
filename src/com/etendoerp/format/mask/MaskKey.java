package com.etendoerp.format.mask;

/**
 * Potential values in a mask.
  */
public class MaskKey {
    protected MaskKey() {
        throw new IllegalStateException("MaskKey should not be instantiated as it is a collection of constants.");
    }

    public static final char DIGIT_KEY = '#';
    public static final char LITERAL_KEY = '\'';
    public static final char UPPERCASE_KEY = 'U';
    public static final char LOWERCASE_KEY = 'L';
    public static final char ALPHA_NUMERIC_KEY = 'A';
    public static final char CHARACTER_KEY = '?';
    public static final char ANYTHING_KEY = '*';
    public static final char HEX_KEY = 'H';
}