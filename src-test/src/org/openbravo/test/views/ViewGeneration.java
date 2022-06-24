/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.views;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.service.OBDal;

/** Checks all views can be generated */
public class ViewGeneration extends ViewGenerationTest {
  final static private Logger log = LogManager.getLogger();

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Test
  public void viewsShouldBeGeneratedAfterADCSCaching() {
    setSystemAdministratorContext();

    adcs.init();

    List<String> allTabs = OBDal.getInstance()
        .getSession()
        .createQuery("select id from ADTab where active = true order by id", String.class)
        .list();

    log.info("Initializing ADCS");
    for (String tab : allTabs) {
      adcs.getTab(tab);
    }

    OBDal.getInstance().getSession().clear();
    List<String> allViews = getAllViewIds();

    log.info("Generating {} views", allViews.size());
    Stats stats = generateViews(allViews);
    log.info(stats.toString());

    assertThat("Errors generating views", stats.getErrors(), is(empty()));
  }

  private Stats generateViews(List<String> viewIds) {
    Stats stats = new Stats();
    for (String viewId : viewIds) {
      try {
        OBDal.getInstance().getSession().clear();

        long t = System.currentTimeMillis();
        String viewDef = generateView(viewId);

        Event e = new Event(viewId, System.currentTimeMillis() - t, viewDef.length());
        stats.add(e);
        // Files.write(Paths.get("/tmp", "view", viewId), viewDef.getBytes());
      } catch (Exception e) {
        stats.addError(viewId);
        log.error("Failed generation of view {}", viewId, e);
      }
    }
    return stats;
  }

  private List<String> getAllViewIds() {
    List<String> allViews = OBDal.getInstance()
        .getSession()
        .createQuery("select id from ADWindow where active = true order by id", String.class)
        .list();

    allViews.addAll(OBDal.getInstance()
        .getSession()
        .createQuery(
            "select concat('processDefinition_', id) from OBUIAPP_Process where active = true order by id",
            String.class)
        .list());

    return allViews;
  }

  private class Stats {
    private List<Event> samples = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    void add(Event event) {
      samples.add(event);
    }

    void addError(String id) {
      errors.add(id);
    }

    private Event getMax() {
      return samples.stream().max(comparing(Event::getTime)).get();
    }

    private Event getMin() {
      return samples.stream().min(comparing(Event::getTime)).get();
    }

    private double getAvg() {
      return samples.stream().mapToLong(Event::getTime).average().getAsDouble();
    }

    private long getPercentile(int n) {
      int nthIdx = ((int) (samples.size() * (n / 100.0))) - 1;

      return samples.stream() //
          .map(Event::getTime) //
          .sorted() //
          .collect(toList()) //
          .get(nthIdx);
    }

    public List<String> getErrors() {
      return errors;
    }

    @Override
    public String toString() {
      if (samples.size() == 1) {
        return "" + samples.get(0).getTime();
      }
      return "Samples: " + samples.size() + "\n" + //
          "    Min: " + getMin() + "\n" + //
          "    Max: " + getMax() + "\n" + //
          "    Avg: " + getAvg() + "\n" + //
          "    80th perc: " + getPercentile(80) + "\n" + //
          "    90th perc: " + getPercentile(90) + "\n" + //
          "    Errors: " + errors.size();
    }
  }

  private static class Event {
    private String viewId;
    private long time;
    private int size;

    public Event(String viewId, long time, int size) {
      this.viewId = viewId;
      this.time = time;
      this.size = size;
    }

    long getTime() {
      return time;
    }

    @Override
    public String toString() {
      return "  view: " + viewId + " - size: " + size + " time:" + time;
    }
  }
}
