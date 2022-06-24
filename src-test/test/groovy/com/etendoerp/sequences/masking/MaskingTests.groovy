package com.etendoerp.sequences.masking

import com.etendoerp.sequences.InputTooLongParseException
import com.etendoerp.sequences.SequenceMaskFormatter
import spock.lang.Issue
import spock.lang.Narrative
import spock.lang.Title
import spock.lang.Specification
import java.text.SimpleDateFormat


@Title("Sequence Test")
@Narrative("""Is the class to test the masking to create sequences""")
class MaskingTests extends Specification {

    @Issue("ERP-496")
    def "create new mask (y/#####) and check if generate the sequence well"() {

        given: "a mask (y/#####), an input and a SequenceMaskFormatter instance "
        String mask = "y/#####"
        def input = 1 // input can be anything
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("y");
        String stringYear= DateFor.format(date);
        result == stringYear + "/00001"

    }

    @Issue("ERP-496")
    def "create a mask with a full date format (yyyy-MM-dd/#####) and check if generate the sequence well"() {

        given: "a mask (yyyy-MM-dd/#####), an input and a SequenceMaskFormatter instance "
        String mask = "yyyy-MM-dd/#####"
        def input = 1 // input can be anything
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate= DateFor.format(date);

        result == stringDate + "/00001"

    }

    @Issue("ERP-496")
    def "create a mask with a full date format ('arg'/yyyy-MM-dd/#####) and check if generate the sequence well"() {

        given: "a mask ('arg'/yyyy-MM-dd/#####), an input and a SequenceMaskFormatter instance "
        String mask = "'arg'/yyyy-MM-dd/#####"
        def input = 1 // input can be anything
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate= DateFor.format(date);

        result == "arg/" + stringDate + "/00001"

    }

    @Issue("ERP-496")
    def "create a mask with a full date format (#/yyyy-MM-dd/##) and check if generate the sequence well"() {

        given: "a mask (#/yyyy-MM-dd/##), an input and a SequenceMaskFormatter instance "
        String mask = "#/yyyy-MM-dd/##"
        def input = 123 // input can be anything
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate= DateFor.format(date);

        result == "1/" + stringDate + "/23"

    }

    @Issue("ERP-496")
    def "create a mask with only full date format (yyyy-MM-dd) and check if generate the sequence well"() {

        given: "a mask (yyyy-MM-dd) and a SequenceMaskFormatter instance "
        String mask = "yyyy-MM-dd"
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString()

        then: "the sequence generated follow the masking"
        Date date = new Date();
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate= DateFor.format(date);
        result == stringDate

    }

    @Issue("ERP-496")
    def "create a mask, update date and check if generate the sequence well"() {

        given: "a mask (yyyy-MM-dd/#####), an input, a new date and a SequenceMaskFormatter instance "

        String mask = "yyyy-MM-dd/#####"
        Date d = new SimpleDateFormat("yyyy-MM-dd").parse("2021-07-20");
        def input = 1 // input can be anything
        def formatter = new SequenceMaskFormatter(mask)
        formatter.setDate(d)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2021-07-20");
        SimpleDateFormat DateFor = new SimpleDateFormat("yyyy-MM-dd");
        String stringDate= DateFor.format(date);
        result == "2021-07-20/00001"

    }


    @Issue("ERP-496")
    def "create new mask (#mask) and check if generate the sequence well"() {

        given: "a mask (#mask), an input (#input) and a SequenceMaskFormatter instance "
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String inputGenerated = formatter.valueToString(input)

        then: "the sequence generated follow the masking"
        inputGenerated == result

        where:
        mask            | input | result
        "##-##"         | 001   | "00-01"
        "##-##"         | null  | "00-00"
        "##-##"         | 0     | "00-00"
        "##-##"         | 1     | "00-01"
        "##-##"         | 9999  | "99-99"
        ""              | null  | ""
        "##!!!#"        | 20    | "20!#"
        "#!##"          | 12    | "1#2"
        "##!!#"         | 123   | "12!3"
        "#'r'#"         | 12    | "1r2"
        "#3#"           | 12    | "132"
        "#-123-#"       | 45    | "4-123-5"
        "#!!!!#"        | 12    | "1!!2"
        "##-@-##"       | 1234  | "12-@-34"
        "##-*-##"       | 1234  | "01-2-34"
        "***"           | 123   | "123"
        "#-'f!ly'-*"    | 12    | "1-fly-2"
        "##-*-##"       | 12345 | "12-3-45"
        "*!**"          | 12    | "1*2"

    }

    @Issue("ERP-556")
    def "Mask Formatter throw an exception when the input (#input) overflows the mask (#mask)"() {

        given: "a mask (#mask), an input longer than the mask digits (#input) , and  SequenceMaskFormatter instance "
        def formatter = new SequenceMaskFormatter(mask)

        when: "running formatter to generate the sequence"
        String result = formatter.valueToString(input)

        then: "A parse exception is thrown"
        thrown InputTooLongParseException

        where:
        mask                | input
        "###"               | 1234
        "##-##"             | 123456
        "##-##"             | 12345
        "yyyy-MM-dd/###"    | 1234
        ""                  | 9999
        "##!##"             | 1234
        "#!!!#"             | 12
        "#!!!!#"            | 123
        "*!**"              | 123

    }

}

