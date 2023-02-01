package org.openbravo.advpaymentmngt.hook;

import javax.enterprise.inject.Instance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class PaymentProcessOrderHook {

  public abstract int getPriority();

  public static List<PaymentProcessHook> sortHooksByPriority(Instance<PaymentProcessHook> hooks){
    List<PaymentProcessHook> hookList = new ArrayList<>();
    for (PaymentProcessHook hookToAdd : hooks) {
      hookList.add(hookToAdd);
    }

    Collections.sort(hookList, new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        int o1Priority = ((PaymentProcessOrderHook) o1).getPriority();
        int o2Priority = ((PaymentProcessOrderHook) o2).getPriority();
        return (int) Math.signum(o1Priority - o2Priority);
      }
    });

    return hookList;
  }

}