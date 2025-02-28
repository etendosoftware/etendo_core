package com.etendoerp.sequences;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;

public class SequenceUtils {

    public static final String NON_TRANSACTIONAL_SEQUENCE_ID = "4148378344A04A50AC7AED3B05F34B4D";

    public final static String INP = "inp";
    public final static String EM = "em";
    public final static String DELIMITER = "_";

    private SequenceUtils() {}

    /**
     * Replaces the 'EM_DBPREFIX' of a column name
     * @param columnName
     * @return The columnName without the external module and database prefix
     */
    public static String truncateColumn(String columnName) {
        final int limit = 3;

        if (columnName.toLowerCase().startsWith(EM.toLowerCase() + DELIMITER)) {
            var split = columnName.split(DELIMITER, limit);
            if (split.length >= 2) {
                return split[2];
            }
        }
        return columnName;
    }

    /**
     * Replaces the 'emDbprefix' of a RequestContext parameter
     * Ex: 'inpemDbprefCustomColumnId' will be converted to 'inpcustomColumnId'
     * @param parameter
     * @return The parameter without the external module and database prefix
     */
    public static String truncateParameter(String parameter) {

        // Is not a parameter
        if (!parameter.startsWith(INP)) {
            return parameter;
        }

        String parameterWithoutInp = StringUtils.replaceOnce(parameter,INP,"");

        // Is not external module parameter
        if (!parameterWithoutInp.startsWith(EM)) {
            return parameter;
        }

        String parameterWithoutEm = StringUtils.replaceOnce(parameterWithoutInp, EM, "");

        // If the next character after 'em' is not in uppercase, then is not a external module parameter.
        // The next character after em should be the DBPREFIX
        if (!Character.isUpperCase(parameterWithoutEm.charAt(0))) {
            return parameter;
        }

        // Replace the DBPREFIX
        StringBuilder dbPrefix = new StringBuilder();
        dbPrefix.append(parameterWithoutEm.charAt(0));

        // Start the loop after the uppercase character
        for (int i = 1; i < parameterWithoutEm.length(); i++) {
            char c = parameterWithoutEm.charAt(i);
            // Break on the first occurrence of a uppercase character
            if (Character.isUpperCase(c)) {
                break;
            }
            dbPrefix.append(c);
        }

        String parameterWithoutPrefix = StringUtils.replaceOnce(parameterWithoutEm, dbPrefix.toString(),"");
        return INP + StringUtils.uncapitalize(parameterWithoutPrefix);
    }


    public static Boolean isSequence(Column column) {
        Reference reference = column.getReferenceSearchKey();
        if (reference == null) {
            reference = column.getReference();
        }
        return referenceHasSequenceConfiguration(reference);
    }

    private static Boolean referenceHasSequenceConfiguration(Reference reference) {
        var sequenceConfig = SequenceDatabaseUtils.getSequenceConfiguration(reference);
        return sequenceConfig != null;
    }

}
