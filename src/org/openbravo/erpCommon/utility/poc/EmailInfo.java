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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility.poc;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class contains all info required to generate an email to send. This includes data to
 * generate the header (recipient, CC, BCC, replyTo, date and extra fields) and the body (subject,
 * content, contentType and attachments)
 *
 * To create a new instance of this class use {@link EmailInfo.Builder}
 */
public class EmailInfo {
  private String recipientTO;
  private String recipientCC;
  private String recipientBCC;
  private String replyTo;
  private String subject;
  private String content;
  private String contentType;
  private List<File> attachments;
  private Date sentDate;
  private List<String> headerExtras;

  private EmailInfo(String recipientTO, String recipientCC, String recipientBCC, String replyTo,
      String subject, String content, String contentType, List<File> attachments, Date sentDate,
      List<String> headerExtras) {
    this.recipientTO = recipientTO;
    this.recipientCC = recipientCC;
    this.recipientBCC = recipientBCC;
    this.replyTo = replyTo;
    this.subject = subject;
    this.content = content;
    this.contentType = contentType;
    this.attachments = attachments;
    this.sentDate = sentDate;
    this.headerExtras = headerExtras;
  }

  public String getRecipientTO() {
    return recipientTO;
  }

  public String getRecipientCC() {
    return recipientCC;
  }

  public String getRecipientBCC() {
    return recipientBCC;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public String getSubject() {
    return subject;
  }

  public String getContent() {
    return content;
  }

  public String getContentType() {
    return contentType;
  }

  public List<File> getAttachments() {
    return attachments;
  }

  public Date getSentDate() {
    return sentDate;
  }

  public List<String> getHeaderExtras() {
    return headerExtras;
  }

  /**
   * Builder class used to create and initialize a EmailInfo instance.
   */
  public static class Builder {
    private String recipientTO = null;
    private String recipientCC = null;
    private String recipientBCC = null;
    private String replyTo = null;
    private String subject = null;
    private String content = null;
    private String contentType = null;
    private List<File> attachments = new ArrayList<>();
    private Date sentDate = null;
    private List<String> headerExtras = new ArrayList<>();

    public Builder() {
    }

    public Builder setRecipientTO(String recipientTO) {
      this.recipientTO = recipientTO;
      return this;
    }

    public Builder setRecipientCC(String recipientCC) {
      this.recipientCC = recipientCC;
      return this;
    }

    public Builder setRecipientBCC(String recipientBCC) {
      this.recipientBCC = recipientBCC;
      return this;
    }

    public Builder setReplyTo(String replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder setSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder setContent(String content) {
      this.content = content;
      return this;
    }

    public Builder setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder setAttachments(List<File> attachments) {
      this.attachments = attachments;
      return this;
    }

    public Builder setSentDate(Date sentDate) {
      this.sentDate = sentDate;
      return this;
    }

    public Builder setHeaderExtras(List<String> headerExtras) {
      this.headerExtras = headerExtras;
      return this;
    }

    /**
     * Generates a EmailInfo instance initialized with all fields set using the set* methods.
     * Otherwise they will be initialized as null or as an empty array for the List fields.
     * 
     * @return a new EmailInfo instance
     */
    public EmailInfo build() {
      return new EmailInfo(recipientTO, recipientCC, recipientBCC, replyTo, subject, content,
          contentType, attachments, sentDate, headerExtras);
    }
  }
}
