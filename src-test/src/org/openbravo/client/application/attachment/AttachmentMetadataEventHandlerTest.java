package org.openbravo.client.application.attachment;

// QUARANTINE: AttachmentMetadataEventHandler has static initializer that calls
// ModelProvider.getInstance().getEntity(Parameter.class) at class load time.
// This causes ExceptionInInitializerError in test context since ModelProvider is not initialized.
// The static field:
// Cannot be bypassed with mockStatic since class loading happens before mock setup.

// Reason: Static initializer calling runtime services (ModelProvider.getInstance())
