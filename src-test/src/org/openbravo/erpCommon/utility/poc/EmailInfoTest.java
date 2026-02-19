package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmailInfoTest {

  @Test
  public void testBuilderWithAllFields() {
    Date sentDate = new Date();
    List<File> attachments = new ArrayList<>();
    List<String> headers = Arrays.asList("X-Custom: value");

    EmailInfo info = new EmailInfo.Builder()
        .setRecipientTO("to@example.com")
        .setRecipientCC("cc@example.com")
        .setRecipientBCC("bcc@example.com")
        .setReplyTo("reply@example.com")
        .setSubject("Test Subject")
        .setContent("Test Content")
        .setContentType("text/html")
        .setAttachments(attachments)
        .setSentDate(sentDate)
        .setHeaderExtras(headers)
        .build();

    assertEquals("to@example.com", info.getRecipientTO());
    assertEquals("cc@example.com", info.getRecipientCC());
    assertEquals("bcc@example.com", info.getRecipientBCC());
    assertEquals("reply@example.com", info.getReplyTo());
    assertEquals("Test Subject", info.getSubject());
    assertEquals("Test Content", info.getContent());
    assertEquals("text/html", info.getContentType());
    assertEquals(attachments, info.getAttachments());
    assertEquals(sentDate, info.getSentDate());
    assertEquals(headers, info.getHeaderExtras());
  }

  @Test
  public void testBuilderWithDefaults() {
    EmailInfo info = new EmailInfo.Builder().build();

    assertNull(info.getRecipientTO());
    assertNull(info.getRecipientCC());
    assertNull(info.getRecipientBCC());
    assertNull(info.getReplyTo());
    assertNull(info.getSubject());
    assertNull(info.getContent());
    assertNull(info.getContentType());
    assertNotNull(info.getAttachments());
    assertTrue(info.getAttachments().isEmpty());
    assertNull(info.getSentDate());
    assertNotNull(info.getHeaderExtras());
    assertTrue(info.getHeaderExtras().isEmpty());
  }

  @Test
  public void testBuilderMethodChaining() {
    EmailInfo.Builder builder = new EmailInfo.Builder();
    EmailInfo.Builder result = builder.setRecipientTO("test@example.com");
    assertEquals(builder, result);
  }

  @Test
  public void testBuilderWithOnlyRecipientTO() {
    EmailInfo info = new EmailInfo.Builder()
        .setRecipientTO("to@example.com")
        .build();

    assertEquals("to@example.com", info.getRecipientTO());
    assertNull(info.getRecipientCC());
    assertNull(info.getSubject());
  }

  @Test
  public void testBuilderWithAttachments() {
    File file1 = new File("/tmp/file1.txt");
    File file2 = new File("/tmp/file2.pdf");
    List<File> attachments = Arrays.asList(file1, file2);

    EmailInfo info = new EmailInfo.Builder()
        .setAttachments(attachments)
        .build();

    assertEquals(2, info.getAttachments().size());
    assertEquals(file1, info.getAttachments().get(0));
    assertEquals(file2, info.getAttachments().get(1));
  }

  @Test
  public void testBuilderWithMultipleHeaderExtras() {
    List<String> headers = Arrays.asList("X-Header1: val1", "X-Header2: val2");

    EmailInfo info = new EmailInfo.Builder()
        .setHeaderExtras(headers)
        .build();

    assertEquals(2, info.getHeaderExtras().size());
  }
}
