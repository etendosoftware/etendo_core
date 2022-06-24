package org.openbravo.base.gen;

public class Utilities {

        private Utilities(){}

        public static String toCamelCase(String input) {
                final StringBuilder sb = new StringBuilder();
                for(String oneString : input.split("_")) {
                        sb.append(oneString.substring(0,1));
                        sb.append(oneString.substring(1).toLowerCase());
                }
                return sb.toString();
        }

}
