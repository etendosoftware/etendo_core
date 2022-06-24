package com.etendoerp.sequences.transactional.parameter

import com.etendoerp.sequences.SequenceUtils
import com.etendoerp.sequences.parameters.SequenceParameter
import com.etendoerp.sequences.parameters.SequenceParameterList
import com.etendoerp.sequences.parameters.SequenceParametersUtils
import com.etendoerp.sequences.transactional.RequiredDimensionException
import spock.lang.Issue
import spock.lang.Specification

class ParameterListWhereClauseTest extends Specification{

    @Issue("ERP-574")
    def "Where clause generated from a SequenceParameterList"() {

        given: "A SequenceParameterList"
        def sequenceParameterList = new SequenceParameterList();

        when: "The list is filled with the required properties of the Sequence class"
        fillWithRequiredProperties(sequenceParameterList)

        and: "The where clause of the SequenceParameterList is generated"
        String whereClause = sequenceParameterList.generateWhereClause()

        then: "The HQL where clause should have the required properties of the Sequence class"
        containsRequiredProperties(whereClause)

    }

    @Issue("ERP-574")
    def "Where clause generated from a SequenceParameterList With extra parameter"() {

        given: "A SequenceParameterList"
        def sequenceParameterList = new SequenceParameterList();

        when: "The list is filled with the required properties of the Sequence class"
        fillWithRequiredProperties(sequenceParameterList)

        and: "An extra property is set in the SequenceParameterList"
        SequenceParameter extra = SequenceParametersUtils.generateTableDirParameter("idextra")
        sequenceParameterList.setParameter('documentType', extra)

        and: "The where clause of the SequenceParameterList is generated"
        String whereClause = sequenceParameterList.generateWhereClause()

        then: "The HQL where clause should have the required properties of the Sequence class"
        containsRequiredProperties(whereClause)
        whereClause.contains("ent.documentType.id=:documentType")
    }

    @Issue("ERP-574")
    def "Where clause generated from a SequenceParameterList missing required property"() {

        given: "A SequenceParameterList"
        def sequenceParameterList = new SequenceParameterList();

        when: "The list is filled with properties of the Sequence class, but is missing the required property 'column'."
        SequenceParameter p1 = SequenceParametersUtils.generateTableDirParameter("idp1")
        sequenceParameterList.setParameter('client', p1)

        SequenceParameter p2 = SequenceParametersUtils.generateTableDirParameter("idp2")
        sequenceParameterList.setParameter('organization', p2)

        and: "The where clause of the SequenceParameterList is generated"
        String whereClause = sequenceParameterList.generateWhereClause()

        then: "A Exception 'RequiredDimensionException' should be throw, containing the missing property"
        RequiredDimensionException e = thrown(RequiredDimensionException)
        e.getRequiredDimension() == "column"
    }

    static void containsRequiredProperties(String clause) {
        assert clause.contains("ent.client.id=:client")
        assert clause.contains("ent.organization.id=:organization")
        assert clause.contains("ent.column.id=:column")
    }

    static def fillWithRequiredProperties(SequenceParameterList sequenceParameterList) {
        SequenceParameter p1 = SequenceParametersUtils.generateTableDirParameter("idp1")
        sequenceParameterList.setParameter('client', p1)

        SequenceParameter p2 = SequenceParametersUtils.generateTableDirParameter("idp2")
        sequenceParameterList.setParameter('organization', p2)

        SequenceParameter p3 = SequenceParametersUtils.generateTableDirParameter("idp3")
        sequenceParameterList.setParameter('column', p3)
    }

}
