package com.etendoerp.sequences;

import com.etendoerp.sequences.transactional.TransactionalSequenceUtils;
import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.ResponseActionsBuilder;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

import java.util.List;
import java.util.Set;

import static org.openbravo.dal.core.OBContext.*;


public class SequencesGenerator extends Action {
  Logger log = LogManager.getLogger(SequencesGenerator.class);

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    var result = new ActionResult();
    int count = 0;
    try {
      setAdminMode();
      //Get Current Client
      Client client = OBContext.getOBContext().getCurrentClient();

      //Get Organizations from client
      Set<String> organizations = new OrganizationStructureProvider().getChildTree(parameters.getString("ad_org_id"),
          true);

      int count = generateSequenceCombination(client, organizations);
      result.setType(Result.Type.SUCCESS);
      String message = OBMessageUtils.getI18NMessage("SequencesWereCreated", new String[]{ String.valueOf(count) });
      result.setMessage(message);

    } catch (Exception e) {
      log.error("Error in process", e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    } finally {
      restorePreviousMode();
    }

    return result;
  }

  public int generateSequenceCombination(Client client, Set<String> organizations) {
    //Get transactional Reference
    Reference reference = OBDal.getInstance().get(Reference.class,
        TransactionalSequenceUtils.TRANSACTIONAL_SEQUENCE_ID);
    //Filter columns by transactional sequence references
    OBCriteria<Column> columnOBCriteria = OBDal.getInstance().createCriteria(Column.class);
    columnOBCriteria.add(Restrictions.eq(Column.PROPERTY_REFERENCE, reference));
    List<Column> sequenceColumns = columnOBCriteria.list();

    int count = 0;
      for (Column column : sequenceColumns) {
        //Get parents organization
        Set<String> parentOrganizations = new OrganizationStructureProvider().getParentTree(
            parameters.getString("ad_org_id"),
            true);

      //Get Document Type
      OBCriteria<DocumentType> documentTypeOBCriteria = OBDal.getInstance().createCriteria(DocumentType.class);
      documentTypeOBCriteria.add(Restrictions.eq(DocumentType.PROPERTY_TABLE, column.getTable()));
        documentTypeOBCriteria.add(Restrictions.in(DocumentType.PROPERTY_ORGANIZATION + ".id", parentOrganizations));
        List<DocumentType> documentTypes = documentTypeOBCriteria.list();

      for (String orgId : organizations) {
          Organization org = OBDal.getInstance().get(Organization.class, orgId);
          String name = column.getTable().getName().substring(0,
              Math.min(column.getTable().getName().length(), 29)) + "-"
              + column.getName().substring(0, Math.min(column.getName().length(), 30));

          if (!documentTypes.isEmpty() && hasDocType(column.getTable())) {
            for (DocumentType docType : documentTypes) {
              count = count + createSequence(client, org, name, column, docType);
            }
          } else {
            count = count + createSequence(client, org, name, column, null);
          }
      }
      }
      result.setType(Result.Type.SUCCESS);
      String message = OBMessageUtils.getI18NMessage("SequencesWereCreated", new String[]{ String.valueOf(count) });
      ResponseActionsBuilder responseActions = result.getResponseActionsBuilder().orElse(getResponseBuilder());
      responseActions.retryExecution();
      responseActions.showResultsInProcessView();
      responseActions.showMsgInProcessView(ResponseActionsBuilder.MessageType.SUCCESS, message);
      result.setResponseActionsBuilder(responseActions);
    } catch (Exception e) {
      log.error("Error in process", e);
      result.setType(Result.Type.ERROR);
      result.setMessage(e.getMessage());
    } finally {
      restorePreviousMode();
    }
    return result;
  }

  public boolean hasDocType(Table t) {
    Entity entity = ModelProvider.getInstance().getEntity(t.getName());
    return entity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE) || entity.hasProperty(
        SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE_TARGET);
  }

  public int createSequence(Client client, Organization organization, String name, Column column,
      DocumentType documentType) {
    if (!existsSequence(column, client, organization, documentType)) {
      setSequenceValues(client, organization, name, column, documentType);
      return 1;
    }
    return 0;
  }

  public Sequence setSequenceValues(Client client, Organization organization, String name, Column column,
      DocumentType documentType) {
      final Sequence sequence = OBProvider.getInstance().get(Sequence.class);
      // set values
      sequence.setClient(client);
      sequence.setOrganization(organization);
      sequence.setName(name);
      sequence.setPrefix("");
      sequence.setSuffix("");
      sequence.setMask("#######");
      sequence.setActive(true);
      sequence.setColumn(column);
      sequence.setTable(column.getTable());
      sequence.setDocumentType(documentType);
      sequence.setAutoNumbering(true);
      sequence.setNextAssignedNumber(1000000L);
      sequence.setIncrementBy(1L);

      // store it in the database
      OBDal.getInstance().save(sequence);
      return 1;
    }
    return 0;
  }

  public boolean existsSequence(Column column, Client client, Organization organization, DocumentType documentType) {
    OBCriteria<Sequence> sequenceOBCriteria = OBDal.getInstance().createCriteria(Sequence.class);
    sequenceOBCriteria.add(Restrictions.eq(Sequence.PROPERTY_CLIENT, client));
    sequenceOBCriteria.add(Restrictions.eq(Sequence.PROPERTY_COLUMN, column));
    if (documentType != null) {
      sequenceOBCriteria.add(Restrictions.eq(Sequence.PROPERTY_DOCUMENTTYPE, documentType));
    }
    sequenceOBCriteria.add(Restrictions.eq(Sequence.PROPERTY_ORGANIZATION, organization));
    sequenceOBCriteria.setMaxResults(1);
    return sequenceOBCriteria.uniqueResult() != null;
  }

  @Override
  protected Class<?> getInputClass() {
    return Sequence.class;
  }
}
