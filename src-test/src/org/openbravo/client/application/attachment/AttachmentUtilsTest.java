package org.openbravo.client.application.attachment;

// QUARANTINE: AttachmentUtils has static initializer that calls
// WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class)
// at class load time. This causes NPE/ExceptionInInitializerError since WeldUtils requires
// a running CDI container (servlet context) which is not available in test context.
// The static field:
//   private static ApplicationDictionaryCachedStructures adcs = WeldUtils
//       .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
// Cannot be bypassed with mockStatic since class loading happens before mock setup.

// Reason: Static initializer calling runtime services (WeldUtils.getInstanceFromStaticBeanManager())
