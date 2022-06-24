package com.etendoerp.sequences;

import com.etendoerp.format.mask.MaskFormatter;
import com.etendoerp.format.mask.MaskKey;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Subclass of {@link MaskFormatter} designed for sequence masks, although its implementation is not sequence dependent.
 * <br>
 * Default use is:
 * <code>
 *     String mask = "y/#####";
 *     var input = 1; // input can be anything
 *     var formatter = new SequenceMaskFormatter(mask);
 *     var result = formatter.valueToString(input); // 2021/00001
 * </code>
 * <br>
 * The main differences are that by default, values will be inserted from right to left, and that date format characters in the mask are supported.
 * <br>
 * For example: <br>
 * Inputs: Date = <code>2021-01-01</code>, Value = <code>789</code>, Placeholder = <code>0</code>, Mask: <code>yy-MM-dd/####</code>
 * <br>
 * Will result in: <br>
 * <code>21-01-01/0789</code>
 * <br>
 * To achieve this, some accepted characters from {@link MaskFormatter} are changed:
 * <table class="striped">
 * <thead>
 *   <tr>
 *     <th scope="col">Original Character
 *     <th scope="col">New Character
 *     <th scope="col">Description
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">'
 *     <th scope="row">!
 *     <td>Escape character, used to escape any of the special formatting
 *     characters.
 *   <tr>
 *     <th scope="row">L
 *     <th scope="row">l
 *     <td>Any character ({@code Character.isLetter}). All upper case letters
 *     are mapped to lower case.
 *   <tr>
 *     <th scope="row">H
 *     <th scope="row">h
 *     <td>Any hex character (0-9, a-f or A-F).
 * </tbody>
 * </table>
 * @see MaskFormatter
 */
public class SequenceMaskFormatter extends MaskFormatter {
    private static final long serialVersionUID = 6371470785789866227L;

    private boolean reverse = true;
    private Date date;

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     *
     */
    public SequenceMaskFormatter() {
        super();
        this.date = new Date();
        setPlaceholderCharacter('0');
        setValueContainsLiteralCharacters(false);
    }

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     *
     * @param mask the mask
     */
    public SequenceMaskFormatter(String mask) {
        this();
        setMask(mask);
    }

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     *
     * @param mask the mask
     * @param reverse whether the input should be placed right to left, or left to right
     * @throws ParseException if mask does not contain valid mask characters
     */
    public SequenceMaskFormatter(String mask, boolean reverse) throws ParseException {
        this();
        this.reverse = reverse;
        setMask(mask);
    }

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     *
     * @param mask the mask
     * @param date the date value to fill in the mask
     * @throws ParseException if mask does not contain valid mask characters
     */
    public SequenceMaskFormatter(String mask, Date date) {
        this();
        this.date = date;
        setMask(mask);
    }

    /**
     * Creates a <code>MaskFormatter</code> with the specified mask.
     * A <code>ParseException</code>
     * will be thrown if <code>mask</code> is an invalid mask.
     *
     * @param mask the mask
     * @param date the date value to fill in the mask
     * @param reverse whether the input should be placed right to left, or left to right
     * @throws ParseException if mask does not contain valid mask characters
     */
    public SequenceMaskFormatter(String mask, Date date, boolean reverse) {
        this();
        this.date = date;
        this.reverse = reverse;
        setMask(mask);
    }

    /**
     * Sets the date to be used when formatting masks which have characters accepted by SimpleDateFormat
     * @param date the date value to fill in the mask
     */
    public void setDate(Date date) {
        this.date = date;
        updateInternalMask(); // update internal mask with new date
    }

    /**
     * Gets the date to be used when formatting masks which have characters accepted by SimpleDateFormat
     * @return the date value to fill in the mask
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param reverse whether the input should be placed right to left, or left to right
     */
    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    /**
     * @return whether the input should be placed right to left, or left to right (default is right to left)
     */
    public boolean isReverse() {
        return reverse;
    }

    /**
     * Invokes <code>append</code> on the mask characters in
     * <code>mask</code>.
     */
    @Override
    protected void append(StringBuilder result, String value, int[] index,
                          String placeholder, MaskCharacter[] mask)
            throws ParseException {
        if (reverse) {
            // The default way to fill a mask (for example ###-##, with placeholder 0) will be:
            // Input: 1 -> Result: 000-01
            // Input: 1234 -> Result: 012-34
            // Input: 12345 -> Result: 123-45
            int[] valueCounter = { value.length() - 1 };
            Deque<Character> characterBuffer = new ArrayDeque<>();
            for (int i = mask.length - 1; i >= 0; i--) {
                var maskCharacter = mask[i];
                maskCharacter.insert(characterBuffer, value, valueCounter);
            }
            if (!characterBuffer.isEmpty()) {
                characterBuffer.forEach(result::append);
            }
        } else {
            // The alternative way is the standard for MaskFormatter:
            // Input: 1 -> Result: 100-00
            // Input: 1234 -> Result: 123-40
            // Input: 12345 -> Result: 123-45
            for (MaskCharacter maskCharacter : mask) {
                maskCharacter.append(result, value, index, placeholder);
            }

        }
    }

    /**
     * Updates the internal representation of the mask.
     */
    @Override
    protected void updateInternalMask() {
        String localMask = getMask();
        ArrayList<MaskCharacter> fixed = new ArrayList<>();

        if (localMask != null) {
            // While this allows to support date formats in the mask, 
            // performance may be better if the mask characters that correspond to a date format are be handled in the switch control below
            var dateFormatter = new SimpleDateFormat(localMask);
            localMask = dateFormatter.format(date);

            boolean ignoreNext = false;
            for (int counter = 0, maxCounter = localMask.length();
                 counter < maxCounter; counter++) {
                if (ignoreNext) {
                    ignoreNext = false;
                    continue;
                }

                char maskChar = localMask.charAt(counter);

                switch (maskChar) {
                    case MaskKey.DIGIT_KEY:
                        fixed.add(new DigitMaskCharacter());
                        break;
                    case SequenceMaskKey.LITERAL_KEY:
                        if (counter + 1 < maxCounter) {
                            maskChar = localMask.charAt(counter + 1);
                            fixed.add(new LiteralCharacter(maskChar));
                            ignoreNext = true;
                        }
                        // else: Could actually throw if else
                        break;
                    case MaskKey.UPPERCASE_KEY:
                        fixed.add(new UpperCaseCharacter());
                        break;
                    case SequenceMaskKey.LOWERCASE_KEY:
                        fixed.add(new LowerCaseCharacter());
                        break;
                    case MaskKey.ALPHA_NUMERIC_KEY:
                        fixed.add(new AlphaNumericCharacter());
                        break;
                    case MaskKey.CHARACTER_KEY:
                        fixed.add(new CharCharacter());
                        break;
                    case MaskKey.ANYTHING_KEY:
                        fixed.add(new MaskCharacter());
                        break;
                    case SequenceMaskKey.HEX_KEY:
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
        if(sValue.length() > countHashesAndAsterisks(getMask())){
            throw new InputTooLongParseException("Mask is shorter than the input generated",  0 );
        }
        StringBuilder result = new StringBuilder();
        String localPlaceholder = getPlaceholder();
        int[] valueCounter = { 0 };


        append(result, sValue, valueCounter, localPlaceholder, maskChars);

        return result.toString();
    }

    public int countHashesAndAsterisks(String mask) {
        int count =0;
        int i =0;
            while(mask.length() > 0 && i < mask.length()){
                if (mask.charAt(i) == SequenceMaskKey.LITERAL_KEY) {
                    i++;
                } else if (mask.charAt(i) == MaskKey.DIGIT_KEY || mask.charAt(i) == MaskKey.ANYTHING_KEY) {
                    count++;
                }
                i++;
            }
        return count;

    }

}
