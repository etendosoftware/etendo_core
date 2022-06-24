package com.etendoerp.sequences.transactional;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Organization;

import java.util.UUID;

public class SequenceTestUtils {

    public static Sequence createTransactionalSequence(String clientId, String orgId) {
        long t = System.currentTimeMillis();
        System.out.println("Creating Sequence.");
        final Sequence testSequence = OBProvider.getInstance().get(Sequence.class);

        Client client = OBDal.getInstance().get(Client.class, clientId);
        Organization organization = OBDal.getInstance().get(Organization.class, orgId);
        testSequence.setClient(client);
        testSequence.setOrganization(organization);
        testSequence.setName("TransactionalSeqTest-" + UUID.randomUUID().toString());
        testSequence.setPrefix("");
        testSequence.setSuffix("");
        testSequence.setMask("#######");
        testSequence.setActive(true);
        testSequence.setAutoNumbering(true);
        testSequence.setNextAssignedNumber(1000000L);
        testSequence.setIncrementBy(1L);
        OBDal.getInstance().save(testSequence);
        OBDal.getInstance().commitAndClose();

        System.out.println("Sequence '"+testSequence.getName()+"' created after "+(System.currentTimeMillis() - t)+" ms.");
        return testSequence;
    }

    public static void deleteSequence(Sequence sequence) {
        System.out.println("Deleting Sequence '"+sequence.getName()+"'.");
        OBDal.getInstance().remove(sequence);
        OBDal.getInstance().commitAndClose();
    }

}
