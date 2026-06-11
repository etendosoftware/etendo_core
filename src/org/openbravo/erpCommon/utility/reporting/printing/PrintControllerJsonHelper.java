package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.ad.access.User;

@SuppressWarnings("java:S00120")
final class PrintControllerJsonHelper {
  private static final Logger log = LogManager.getLogger();
  private static final String JSON_KEY_ERROR = "error";

  private PrintControllerJsonHelper() {
  }

  static JSONObject buildBPContactsJson(VariablesSecureApp vars, PocData[] pocData) {
    JSONObject result = new JSONObject();
    try {
      JSONArray contacts = new JSONArray();
      if (isMultiCustomer(pocData)) {
        List<String> seenEmails = new ArrayList<>();
        for (PocData doc : pocData) {
          if (StringUtils.isBlank(doc.contactEmail) || seenEmails.contains(doc.contactEmail)) {
            continue;
          }
          seenEmails.add(doc.contactEmail);
          JSONObject json = new JSONObject();
          json.put("contactId", StringUtils.defaultString(doc.contactUserId));
          json.put("name", StringUtils.defaultString(doc.contactName));
          json.put("email", doc.contactEmail);
          json.put("isDefault", false);
          json.put("isActive", true);
          contacts.put(json);
        }
      } else {
        String bpartnerId = pocData != null && pocData.length > 0
            ? pocData[0].bpartnerId
            : vars.getStringParameter("bpartnerId");
        List<User> bpContacts = BPContactEmailSelector.getBPContactsWithEmail(bpartnerId);
        if (bpContacts != null) {
          for (User contact : bpContacts) {
            contacts.put(buildContactJson(contact));
          }
        }
      }
      result.put("contacts", contacts);
    } catch (Exception exception) {
      log.error("Error building BP contacts JSON", exception);
      result = buildErrorJson();
    }
    return result;
  }

  static JSONObject buildErrorJson() {
    JSONObject error = new JSONObject();
    try {
      error.put(JSON_KEY_ERROR, true);
    } catch (JSONException ignored) {
      // No-op: an empty JSON object is still a valid fallback response.
    }
    return error;
  }

  static void writeJsonResponse(HttpServletResponse response, JSONObject json) throws IOException {
    response.setContentType(PrintController.CONTENT_TYPE_JSON);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(json.toString());
  }

  private static boolean isMultiCustomer(PocData[] pocData) {
    if (pocData == null || pocData.length <= 1) {
      return false;
    }
    String businessPartnerId = pocData[0].bpartnerId;
    for (PocData data : pocData) {
      if (!businessPartnerId.equals(data.bpartnerId)) {
        return true;
      }
    }
    return false;
  }

  private static JSONObject buildContactJson(User contact) throws JSONException {
    JSONObject contactJson = new JSONObject();
    contactJson.put("id", contact.getId());
    contactJson.put("name", contact.getName());
    contactJson.put("email", contact.getEmail());
    contactJson.put("isDefault", false);
    contactJson.put("isActive", contact.isActive());
    return contactJson;
  }
}
