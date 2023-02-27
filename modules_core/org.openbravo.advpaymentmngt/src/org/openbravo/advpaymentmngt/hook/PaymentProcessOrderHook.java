package org.openbravo.advpaymentmngt.hook;

import javax.enterprise.inject.Instance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class PaymentProcessOrderHook {

  public abstract int getPriority();

  public static List<PaymentProcessHook> sortHooksByPriority(Instance<PaymentProcessHook> hooks){
    List<PaymentProcessHook> hookList = new ArrayList<>();
    for (PaymentProcessHook hookToAdd : hooks) {
      hookList.add(hookToAdd);
    }

    hookList.sort((Comparator<Object>) (o1, o2) -> {
      int o1Priority = ((PaymentProcessOrderHook) o1).getPriority();
      int o2Priority = ((PaymentProcessOrderHook) o2).getPriority();
      return (int) Math.signum((float) o1Priority - (float) o2Priority);
    });

    return hookList;
  }

}

