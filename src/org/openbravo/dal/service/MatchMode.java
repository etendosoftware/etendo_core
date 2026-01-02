package org.openbravo.dal.service;

public enum MatchMode {
  EXACT {
    public String toMatchString(String pattern) {
      return pattern;
    }
  },
  START {
    public String toMatchString(String pattern) {
      return pattern + '%';
    }
  },
  END {
    public String toMatchString(String pattern) {
      return '%' + pattern;
    }
  },
  ANYWHERE {
    public String toMatchString(String pattern) {
      return '%' + pattern + '%';
    }
  };

  public abstract String toMatchString(String pattern);
}
