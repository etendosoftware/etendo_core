package com.etendoerp.sequences;

import com.etendoerp.format.mask.MaskKey;

/**
 * Extension of the potential values in a mask, to be used in sequence masks.
 * Renames those Mask Characters that collide with date format characters.
 */
public class SequenceMaskKey extends MaskKey {
    private SequenceMaskKey() {}

    public static final char LOWERCASE_KEY = 'l'; // Original was L, but corresponds to "Month Standalone" in a date format.
    public static final char HEX_KEY = 'x'; // Original was X, but corresponds to "ISO ZONE" in a date format.
    public static final char LITERAL_KEY = '!'; // Original was ', but SimpleDateFormat parses quotes and requires them to be in pairs
}
