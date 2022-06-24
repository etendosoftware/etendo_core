package com.etendoerp.sequences;

import java.text.ParseException;

public class InputTooLongParseException extends ParseException {
    /**
     * Constructs a ParseException with the specified detail message and
     * offset.
     * A detail message is a String that describes this particular exception.
     *
     * @param s           the detail message
     * @param errorOffset the position where the error is found while parsing.
     */
    public InputTooLongParseException(String s, int errorOffset) {
        super(s, errorOffset);
    }
}
