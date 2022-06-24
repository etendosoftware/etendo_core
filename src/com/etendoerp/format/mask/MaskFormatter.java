/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.etendoerp.format.mask;

import com.etendoerp.format.DefaultFormatter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Deque;
import static com.etendoerp.format.mask.MaskKey.*;

/**
 * <code>MaskFormatter</code> is used to format and edit strings. The behavior
 * of a <code>MaskFormatter</code> is controlled by way of a String mask
 * that specifies the valid characters that can be contained at a particular
 * location in the <code>Document</code> model. The following characters can
 * be specified:
 *
 * <table class="striped">
 * <caption>Valid characters and their descriptions</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Character
 *     <th scope="col">Description
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">#
 *     <td>Any valid number, uses {@code Character.isDigit}.
 *   <tr>
 *     <th scope="row">'
 *     <td>Escape character, used to escape any of the special formatting
 *     characters.
 *   <tr>
 *     <th scope="row">U
 *     <td>Any character ({@code Character.isLetter}). All lowercase letters are
 *     mapped to upper case.
 *   <tr>
 *     <th scope="row">L
 *     <td>Any character ({@code Character.isLetter}). All upper case letters
 *     are mapped to lower case.
 *   <tr>
 *     <th scope="row">A
 *     <td>Any character or number ({@code Character.isLetter} or
 *     {@code Character.isDigit}).
 *   <tr>
 *     <th scope="row">?
 *     <td>Any character ({@code Character.isLetter}).
 *   <tr>
 *     <th scope="row">*
 *     <td>Anything.
 *   <tr>
 *     <th scope="row">H
 *     <td>Any hex character (0-9, a-f or A-F).
 * </tbody>
 * </table>
 *
 * <p>
 * Typically characters correspond to one char, but in certain languages this
 * is not the case. The mask is on a per character basis, and will thus
 * adjust to fit as many chars as are needed.
 * <p>
 * You can further restrict the characters that can be input by the
 * <code>setInvalidCharacters</code> and <code>setValidCharacters</code>
 * methods. <code>setInvalidCharacters</code> allows you to specify
 * which characters are not legal. <code>setValidCharacters</code> allows
 * you to specify which characters are valid. For example, the following
 * code block is equivalent to a mask of '0xHHH' with no invalid/valid
 * characters:
 * <pre>
 * MaskFormatter formatter = new MaskFormatter("0x***");
 * formatter.setValidCharacters("0123456789abcdefABCDEF");
 * </pre>
 * <p>
 * When initially formatting a value if the length of the string is
 * less than the length of the mask, two things can happen. Either
 * the placeholder string will be used, or the placeholder character will
 * be used. Precedence is given to the placeholder string. For example:
 * <pre>
 *   MaskFormatter formatter = new MaskFormatter("###-####");
 *   formatter.setPlaceholderCharacter('_');
 *   formatter.getDisplayValue(tf, "123");
 * </pre>
 * <p>
 * Would result in the string '123-____'. If
 * <code>setPlaceholder("555-1212")</code> was invoked '123-1212' would
 * result. The placeholder String is only used on the initial format,
 * on subsequent formats only the placeholder character will be used.
 * <p>
 * If a <code>MaskFormatter</code> is configured to only allow valid characters
 * (<code>setAllowsInvalid(false)</code>) literal characters will be skipped as
 * necessary when editing. Consider a <code>MaskFormatter</code> with
 * the mask "###-####" and current value "555-1212". Using the right
 * arrow key to navigate through the field will result in (| indicates the
 * position of the caret):
 * <pre>
 *   |555-1212
 *   5|55-1212
 *   55|5-1212
 *   555-|1212
 *   555-1|212
 * </pre>
 * The '-' is a literal (non-editable) character, and is skipped.
 * <p>
 * Similar behavior will result when editing. Consider inserting the string
 * '123-45' and '12345' into the <code>MaskFormatter</code> in the
 * previous example. Both inserts will result in the same String,
 * '123-45__'. When <code>MaskFormatter</code>
 * is processing the insert at character position 3 (the '-'), two things can
 * happen:
 * <ol>
 *   <li>If the inserted character is '-', it is accepted.
 *   <li>If the inserted character matches the mask for the next non-literal
 *       character, it is accepted at the new location.
 *   <li>Anything else results in an invalid edit
 * </ol>
 * <p>
 * By default <code>MaskFormatter</code> will not allow invalid edits, you can
 * change this with the <code>setAllowsInvalid</code> method, and will
 * commit edits on valid edits (use the <code>setCommitsOnValidEdit</code> to
 * change this).
 * <p>
 * By default, <code>MaskFormatter</code> is in overwrite mode. That is as
 * characters are typed a new character is not inserted, rather the character
 * at the current location is replaced with the newly typed character. You
 * can change this behavior by way of the method <code>setOverwriteMode</code>.
 * <p>
 * This class is taken from Java Swing and modified to remove dependencies to Swing classes.
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MaskFormatter extends DefaultFormatter {

    public static final MaskCharacter[] EmptyMaskChars = new MaskCharacter[0];

    /** The user specified mask. */
    private String mask;

    public transient MaskCharacter[] maskChars;

    /** List of valid characters. */
    private String validCharacters;

    /** List of invalid characters. */
    private String invalidCharacters;

    /** String used for the passed in value if it does not completely
     * fill the mask. */
    private String placeholderString;

    /** String used to represent characters not present. */
    private char placeholder;

    /** Indicates if the value contains the literal characters. */
    private boolean containsLiteralChars;


    /**
     * Creates a MaskFormatter with no mask.
     */
    public MaskFormatter() {
        setAllowsInvalid(false);
        containsLiteralChars = true;
        maskChars = EmptyMaskChars;
        placeholder = ' ';
    }

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     * @param mask the mask
     */
    public MaskFormatter(String mask) {
        this();
        setMask(mask);
    }

    /**
     * Sets the mask dictating the legal characters.
     * This will throw a <code>ParseException</code> if <code>mask</code> is
     * not valid.
     * @param mask the mask
     *
     */
    public void setMask(String mask) {
        this.mask = mask;
        updateInternalMask();
    }

    /**
     * Returns the formatting mask.
     *
     * @return Mask dictating legal character values.
     */
    public String getMask() {
        return mask;
    }

    /**
     * Allows for further restricting of the characters that can be input.
     * Only characters specified in the mask, not in the
     * <code>invalidCharacters</code>, and in
     * <code>validCharacters</code> will be allowed to be input. Passing
     * in null (the default) implies the valid characters are only bound
     * by the mask and the invalid characters.
     *
     * @param validCharacters If non-null, specifies legal characters.
     */
    public void setValidCharacters(String validCharacters) {
        this.validCharacters = validCharacters;
    }

    /**
     * Returns the valid characters that can be input.
     *
     * @return Legal characters
     */
    public String getValidCharacters() {
        return validCharacters;
    }

    /**
     * Allows for further restricting of the characters that can be input.
     * Only characters specified in the mask, not in the
     * <code>invalidCharacters</code>, and in
     * <code>validCharacters</code> will be allowed to be input. Passing
     * in null (the default) implies the valid characters are only bound
     * by the mask and the valid characters.
     *
     * @param invalidCharacters If non-null, specifies illegal characters.
     */
    public void setInvalidCharacters(String invalidCharacters) {
        this.invalidCharacters = invalidCharacters;
    }

    /**
     * Returns the characters that are not valid for input.
     *
     * @return illegal characters.
     */
    public String getInvalidCharacters() {
        return invalidCharacters;
    }

    /**
     * Sets the string to use if the value does not completely fill in
     * the mask. A null value implies the placeholder char should be used.
     *
     * @param placeholder String used when formatting if the value does not
     *        completely fill the mask
     */
    public void setPlaceholder(String placeholder) {
        this.placeholderString = placeholder;
    }

    /**
     * Returns the String to use if the value does not completely fill
     * in the mask.
     *
     * @return String used when formatting if the value does not
     *        completely fill the mask
     */
    public String getPlaceholder() {
        return placeholderString;
    }

    /**
     * Sets the character to use in place of characters that are not present
     * in the value, ie the user must fill them in. The default value is
     * a space.
     * <p>
     * This is only applicable if the placeholder string has not been
     * specified, or does not completely fill in the mask.
     *
     * @param placeholder Character used when formatting if the value does not
     *        completely fill the mask
     */
    public void setPlaceholderCharacter(char placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * Returns the character to use in place of characters that are not present
     * in the value, ie the user must fill them in.
     *
     * @return Character used when formatting if the value does not
     *        completely fill the mask
     */
    public char getPlaceholderCharacter() {
        return placeholder;
    }

    /**
     * If true, the returned value and set value will also contain the literal
     * characters in mask.
     * <p>
     * For example, if the mask is <code>'(###) ###-####'</code>, the
     * current value is <code>'(415) 555-1212'</code>, and
     * <code>valueContainsLiteralCharacters</code> is
     * true <code>stringToValue</code> will return
     * <code>'(415) 555-1212'</code>. On the other hand, if
     * <code>valueContainsLiteralCharacters</code> is false,
     * <code>stringToValue</code> will return <code>'4155551212'</code>.
     *
     * @param containsLiteralChars Used to indicate if literal characters in
     *        mask should be returned in stringToValue
     */
    public void setValueContainsLiteralCharacters(
            boolean containsLiteralChars) {
        this.containsLiteralChars = containsLiteralChars;
    }

    /**
     * Returns true if <code>stringToValue</code> should return literal
     * characters in the mask.
     *
     * @return True if literal characters in mask should be returned in
     *         stringToValue
     */
    public boolean getValueContainsLiteralCharacters() {
        return containsLiteralChars;
    }

    /**
     * Parses the text, returning the appropriate Object representation of
     * the String <code>value</code>. This strips the literal characters as
     * necessary and invokes supers <code>stringToValue</code>, so that if
     * you have specified a value class (<code>setValueClass</code>) an
     * instance of it will be created. This will throw a
     * <code>ParseException</code> if the value does not match the current
     * mask.  Refer to {@link #setValueContainsLiteralCharacters} for details
     * on how literals are treated.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value String to convert
     * @see #setValueContainsLiteralCharacters
     * @return Object representation of text
     */
    @Override
    public Object stringToValue(String value) throws ParseException {
        return stringToValue(value, true);
    }

    /**
     * Returns a String representation of the Object <code>value</code>
     * based on the mask.  Refer to
     * {@link #setValueContainsLiteralCharacters} for details
     * on how literals are treated.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value Value to convert
     * @see #setValueContainsLiteralCharacters
     * @return String representation of value
     */
    @Override
    public String valueToString(Object value) throws ParseException {
        String sValue = (value == null) ? "" : value.toString();
        StringBuilder result = new StringBuilder();
        String localPlaceholder = getPlaceholder();
        int[] valueCounter = { 0 };

        append(result, sValue, valueCounter, localPlaceholder, maskChars);
        return result.toString();
    }

    /**
     * Actual <code>stringToValue</code> implementation.
     * If <code>completeMatch</code> is true, the value must exactly match
     * the mask, on the other hand if <code>completeMatch</code> is false
     * the string must match the mask or the placeholder string.
     */
    protected Object stringToValue(String value, boolean completeMatch) throws
            ParseException {
        int errorOffset;

        if ((errorOffset = getInvalidOffset(value, completeMatch)) == -1) {
            if (!getValueContainsLiteralCharacters()) {
                value = stripLiteralChars(value);
            }
            return super.stringToValue(value);
        }
        throw new ParseException("stringToValue passed invalid value",
                errorOffset);
    }

    /**
     * Returns -1 if the passed in string is valid, otherwise the index of
     * the first bogus character is returned.
     */
    private int getInvalidOffset(String string, boolean completeMatch) {
        int iLength = string.length();

        if (iLength != getMaxLength()) {
            // trivially false
            return iLength;
        }
        for (int counter = 0, max = string.length(); counter < max; counter++){
            char aChar = string.charAt(counter);

            if (!isValidCharacter(counter, aChar) &&
                    (completeMatch || !isPlaceholder(aChar))) {
                return counter;
            }
        }
        return -1;
    }

    /**
     * Invokes <code>append</code> on the mask characters in
     * <code>mask</code>.
     */
    protected void append(StringBuilder result, String value, int[] index,
                        String placeholder, MaskCharacter[] mask)
            throws ParseException {
        for (MaskCharacter maskCharacter : mask) {
            maskCharacter.append(result, value, index, placeholder);
        }
    }

    /**
     * Updates the internal representation of the mask.
     */
    protected void updateInternalMask() {
        String localMask = getMask();
        ArrayList<MaskCharacter> fixed = new ArrayList<>();

        if (localMask != null) {
            boolean ignoreNext = false;
            for (int counter = 0, maxCounter = localMask.length();
                 counter < maxCounter; counter++) {
                if (ignoreNext) {
                    ignoreNext = false;
                    continue;
                }

                char maskChar = localMask.charAt(counter);

                switch (maskChar) {
                    case DIGIT_KEY:
                        fixed.add(new DigitMaskCharacter());
                        break;
                    case LITERAL_KEY:
                        if (counter + 1 < maxCounter) {
                            maskChar = localMask.charAt(counter + 1);
                            fixed.add(new LiteralCharacter(maskChar));
                            ignoreNext = true;
                        }
                        // else: Could actually throw if else
                        break;
                    case UPPERCASE_KEY:
                        fixed.add(new UpperCaseCharacter());
                        break;
                    case LOWERCASE_KEY:
                        fixed.add(new LowerCaseCharacter());
                        break;
                    case ALPHA_NUMERIC_KEY:
                        fixed.add(new AlphaNumericCharacter());
                        break;
                    case CHARACTER_KEY:
                        fixed.add(new CharCharacter());
                        break;
                    case ANYTHING_KEY:
                        fixed.add(new MaskCharacter());
                        break;
                    case HEX_KEY:
                        fixed.add(new HexCharacter());
                        break;
                    default:
                        fixed.add(new LiteralCharacter(maskChar));
                        break;
                }
            }
        }
        if (fixed.isEmpty()) {
            maskChars = EmptyMaskChars;
        }
        else {
            maskChars = new MaskCharacter[fixed.size()];
            fixed.toArray(maskChars);
        }
    }

    /**
     * Returns the MaskCharacter at the specified location.
     */
    private MaskCharacter getMaskCharacter(int index) {
        if (index >= maskChars.length) {
            return null;
        }
        return maskChars[index];
    }

    /**
     * Returns true if the placeholder character matches aChar.
     */
    private boolean isPlaceholder(char aChar) {
        return (getPlaceholderCharacter() == aChar);
    }

    /**
     * Returns true if the passed in character matches the mask at the
     * specified location.
     */
    private boolean isValidCharacter(int index, char aChar) {
        MaskFormatter.MaskCharacter character = getMaskCharacter(index);
        if (character != null) {
            return character.isValidCharacter(aChar);
        } else {
            return false;
        }
    }

    /**
     * Returns true if the character at the specified location is a literal,
     * that is it can not be edited.
     */
    private boolean isLiteral(int index) {
        MaskFormatter.MaskCharacter character = getMaskCharacter(index);
        if (character != null) {
            return character.isLiteral();
        } else {
            return false;
        }
    }

    /**
     * Returns the maximum length the text can be.
     */
    private int getMaxLength() {
        return maskChars.length;
    }

    /**
     * Removes the literal characters from the passed in string.
     */
    private String stripLiteralChars(String string) {
        StringBuilder sb = null;
        int last = 0;

        for (int counter = 0, max = string.length(); counter < max; counter++){
            if (isLiteral(counter)) {
                if (sb == null) {
                    sb = new StringBuilder();
                    if (counter > 0) {
                        sb.append(string, 0, counter);
                    }
                }
                else if (last != counter) {
                    sb.append(string, last, counter);
                }
                last = counter + 1;
            }
        }
        if (sb == null) {
            // Assume the mask isn't all literals.
            return string;
        }
        else if (last != string.length()) {
            sb.append(string.substring(last));
        }
        return sb.toString();
    }

    //
    // Interal classes used to represent the mask.
    //
    public class MaskCharacter {

        public static final String INVALID_CHARACTER = "Invalid character: ";

        /**
         * Subclasses should override this returning true if the instance
         * represents a literal character. The default implementation
         * returns false.
         */
        public boolean isLiteral() {
            return false;
        }

        /**
         * Returns true if <code>aChar</code> is a valid reprensentation of
         * the receiver. The default implementation returns true if the
         * receiver represents a literal character and <code>getChar</code>
         * == aChar. Otherwise, this will return true is <code>aChar</code>
         * is contained in the valid characters and not contained
         * in the invalid characters.
         */
        public boolean isValidCharacter(char aChar) {
            if (isLiteral()) {
                return (getChar(aChar) == aChar);
            }

            aChar = getChar(aChar);

            String filter = getValidCharacters();

            if (filter != null && filter.indexOf(aChar) == -1) {
                return false;
            }
            filter = getInvalidCharacters();
            return filter == null || filter.indexOf(aChar) == -1;
        }

        /**
         * Returns the character to insert for <code>aChar</code>. The
         * default implementation returns <code>aChar</code>. Subclasses
         * that wish to do some sort of mapping, perhaps lower case to upper
         * case should override this and do the necessary mapping.
         */
        public char getChar(char aChar) {
            return aChar;
        }

        /**
         * Appends the necessary character in <code>formatting</code> at
         * <code>index</code> to <code>buff</code>.
         */
        public void append(StringBuilder buff, String formatting, int[] index,
                           String placeholder)
                throws ParseException {
            boolean inString = index[0] < formatting.length();
            char aChar = inString ? formatting.charAt(index[0]) : 0;

            if (isLiteral()) {
                buff.append(getChar(aChar));
                if (getValueContainsLiteralCharacters()) {
                    if (inString && aChar != getChar(aChar)) {
                        throw new ParseException(INVALID_CHARACTER +
                                aChar, index[0]);
                    }
                    index[0] = index[0] + 1;
                }
            }
            else if (index[0] >= formatting.length()) {
                if (placeholder != null && index[0] < placeholder.length()) {
                    buff.append(placeholder.charAt(index[0]));
                }
                else {
                    buff.append(getPlaceholderCharacter());
                }
                index[0] = index[0] + 1;
            }
            else if (isValidCharacter(aChar)) {
                buff.append(getChar(aChar));
                index[0] = index[0] + 1;
            }
            else {
                throw new ParseException(INVALID_CHARACTER + aChar,
                        index[0]);
            }
        }

        /**
         * Inserts the necessary character in <code>formatting</code> at
         * <code>index</code> at the beginning of <code>buff</code>.
         * Note: Index must start from the end of the <code>formatting</code> parameter (aka <code>formatting.length() - 1</code>)
         */
        public void insert(Deque<Character> buff, String formatting, int[] index)
                throws ParseException {
            boolean inString = index[0] >= 0;
            char aChar = inString ? formatting.charAt(index[0]) : 0;
            if (isLiteral()) {
                buff.push(getChar(aChar));
                if (getValueContainsLiteralCharacters()) {
                    if (inString && aChar != getChar(aChar)) {
                        throw new ParseException(INVALID_CHARACTER +
                                aChar, index[0]);
                    }
                    index[0] = index[0] - 1;
                }
            }
            else if (index[0] < 0) {
                buff.push(getPlaceholderCharacter());
                index[0] = index[0] - 1;
            }
            else if (isValidCharacter(aChar)) {
                buff.push(getChar(aChar));
                index[0] = index[0] - 1;
            }
            else {
                throw new ParseException(INVALID_CHARACTER + aChar,
                        index[0]);
            }
        }
    }


    /**
     * Used to represent a fixed character in the mask.
     */
    protected class LiteralCharacter extends MaskCharacter {
        private final char fixedChar;

        public LiteralCharacter(char fixedChar) {
            this.fixedChar = fixedChar;
        }

        @Override
        public boolean isLiteral() {
            return true;
        }

        @Override
        public char getChar(char aChar) {
            return fixedChar;
        }
    }


    /**
     * Represents a number, uses <code>Character.isDigit</code>.
     */
    public class DigitMaskCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return (Character.isDigit(aChar) &&
                    super.isValidCharacter(aChar));
        }
    }


    /**
     * Represents a character, lower case letters are mapped to upper case
     * using <code>Character.toUpperCase</code>.
     */
    public class UpperCaseCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return (Character.isLetter(aChar) &&
                    super.isValidCharacter(aChar));
        }

        @Override
        public char getChar(char aChar) {
            return Character.toUpperCase(aChar);
        }
    }


    /**
     * Represents a character, upper case letters are mapped to lower case
     * using <code>Character.toLowerCase</code>.
     */
    public class LowerCaseCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return (Character.isLetter(aChar) &&
                    super.isValidCharacter(aChar));
        }

        @Override
        public char getChar(char aChar) {
            return Character.toLowerCase(aChar);
        }
    }


    /**
     * Represents either a character or digit, uses
     * <code>Character.isLetterOrDigit</code>.
     */
    public class AlphaNumericCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return (Character.isLetterOrDigit(aChar) &&
                    super.isValidCharacter(aChar));
        }
    }


    /**
     * Represents a letter, uses <code>Character.isLetter</code>.
     */
    public class CharCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return (Character.isLetter(aChar) &&
                    super.isValidCharacter(aChar));
        }
    }


    /**
     * Represents a hex character, 0-9a-fA-F. a-f is mapped to A-F
     */
    public class HexCharacter extends MaskCharacter {
        @Override
        public boolean isValidCharacter(char aChar) {
            return ((aChar == '0' || aChar == '1' ||
                    aChar == '2' || aChar == '3' ||
                    aChar == '4' || aChar == '5' ||
                    aChar == '6' || aChar == '7' ||
                    aChar == '8' || aChar == '9' ||
                    aChar == 'a' || aChar == 'A' ||
                    aChar == 'b' || aChar == 'B' ||
                    aChar == 'c' || aChar == 'C' ||
                    aChar == 'd' || aChar == 'D' ||
                    aChar == 'e' || aChar == 'E' ||
                    aChar == 'f' || aChar == 'F') &&
                    super.isValidCharacter(aChar));
        }

        @Override
        public char getChar(char aChar) {
            if (Character.isDigit(aChar)) {
                return aChar;
            }
            return Character.toUpperCase(aChar);
        }
    }
}