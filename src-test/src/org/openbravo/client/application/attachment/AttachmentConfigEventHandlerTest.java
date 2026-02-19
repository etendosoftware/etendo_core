package org.openbravo.client.application.attachment;

// QUARANTINE: AttachmentConfigEventHandler has static initializer that calls
// ModelProvider.getInstance().getEntity(AttachmentConfig.ENTITY_NAME) at class load time.
// This causes ExceptionInInitializerError in test context since ModelProvider is not initialized.
// The static field:
//   private static Entity[] entities = { ModelProvider.getInstance().getEntity(AttachmentConfig.ENTITY_NAME) };
//   private static Property propActive = entities[0].getProperty(AttachmentConfig.PROPERTY_ACTIVE);
// Cannot be bypassed with mockStatic since class loading happens before mock setup.

// Reason: Static initializer calling runtime services (ModelProvider.getInstance())
