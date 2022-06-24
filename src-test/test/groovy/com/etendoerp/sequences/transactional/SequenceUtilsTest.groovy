package com.etendoerp.sequences.transactional

import com.etendoerp.sequences.SequenceUtils
import org.openbravo.data.Sqlc
import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.Specification
import spock.lang.Title

@Title("Sequence Utils Test")
@Narrative(""" Implementation to test the SequenceUtils static methods """)
class SequenceUtilsTest extends Specification {

    @Issue("ERP-494")
    def "The truncated value of the column '#columnName' is '#truncatedColumn'"() {

        expect:
        SequenceUtils.truncateColumn(columnName) == truncatedColumn

        where:
        columnName             || truncatedColumn
        "em_dbpref_col_name"   || "col_name"
        "em_dbpref_emcol"      || "emcol"
        "col_name_id"          || "col_name_id"
        "em_seqt_m_product_id" || "m_product_id"
        "m_product_id"         || "m_product_id"
    }

    @Issue("ERP-494")
    def "The truncated value of the parameter '#parameterName' is '#truncatedParameter'"() {

        expect:
        SequenceUtils.truncateParameter(parameterName) == truncatedParameter

        where:
        parameterName               || truncatedParameter
        "inpemDbprefColumnName"     || "inpcolumnName"
        "inpemSeqtMProductId"       || "inpmProductId"
        "inpemcolumn"               || "inpemcolumn"
        "inpmProductId"             || "inpmProductId"
        "noParameter"               || "noParameter"

    }


}
