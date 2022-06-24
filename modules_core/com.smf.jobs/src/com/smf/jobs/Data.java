package com.smf.jobs;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class that holds the result of a Filter. It is used as input for an Action.
 */
public class Data {

    private List<BaseOBObject> contents;
    private JSONObject rawData;

    public Data() { }

    public Data(List<BaseOBObject> contents) {
        this.contents = contents;
    }

    /**
     * Creates an instance from a Process Definition JSON Object.
     * It will get the selected records from the input JSON and store them as a list of BaseOBObject
     * @param json Process Definition content.
     * @param entity The entity class used to make the queries (can be obtained with the ModelProvider class and the entity name).
     * @throws JSONException when the input JSON is malformed.
     */
    public Data(JSONObject json, Class<? extends BaseOBObject> entity) throws JSONException {
        this.rawData = json;

        var selectedRecords = Optional.ofNullable(json.optJSONArray("recordIds"));
        var records = new ArrayList<String>();
        var contents = new ArrayList<BaseOBObject>();
        var entityName = json.optString("_entityName");

        if (selectedRecords.isPresent()) {
            OBCriteria<? extends BaseOBObject> criteria;
            var selected = selectedRecords.get();

            for (int index = 0; index < selected.length(); index++) {
                var recordId = selected.getString(index);
                records.add(recordId);
            }

            if (entityName != null && !entityName.isBlank()) {
                criteria = OBDal.getInstance().createCriteria(entityName);
            } else {
                criteria = OBDal.getInstance().createCriteria(entity);
            }

            criteria.add(Restrictions.in(BaseOBObject.ID, records));
            contents.addAll(criteria.list());
        }

        var keyName = Optional.ofNullable(json.optString("inpKeyName"));
        Optional<String> selectedRecordId = Optional.empty();
        if (keyName.isPresent()) {
            selectedRecordId = Optional.ofNullable(json.optString(keyName.orElseThrow()));
        }

        if (selectedRecordId.isPresent() && !records.contains(selectedRecordId.orElseThrow())) {
            BaseOBObject singleRecord;
            if (entityName != null && !entityName.isBlank()) {
                singleRecord = OBDal.getInstance().get(entityName, selectedRecordId.orElseThrow());
            } else {
                singleRecord = OBDal.getInstance().get(entity, selectedRecordId.orElseThrow());
            }
            Optional.ofNullable(singleRecord).ifPresent(contents::add);
        }

        this.setContents(contents);
    }

    /**
     * Returns the contents typed to the Entity passed as an argument.
     * Note that this will throw a Runtime Exception if the casting fails.
     * @param entity A class that extends BaseOBObject.
     * @param <T> Type of the class that extends BaseOBObject.
     * @return a List of the desired type.
     */
    @SuppressWarnings({"unchecked"})
    public <T extends BaseOBObject> List<T> getContents(Class<T> entity) {
        return (List<T>) getContents();
    }

    /**
     * @return  the contents as a list of BaseOBObject
     */
    public List<BaseOBObject> getContents() {
        return contents;
    }

    /**
     * Set the contents manually
     * @param contents a list of BaseOBObject
     */
    public void setContents(List<BaseOBObject> contents) {
        this.contents = contents;
    }

    /**
     * Use this if you want to get the JSONObject when an instance is created as part of a Process Definition execution.
     * Though it is recommended to make use of the {@link Action#preRun(JSONObject)} method.
     * @return the JSON Object stored when using the {@link Data#Data(JSONObject, Class)} constructor
     */
    public JSONObject getRawData() {
        return rawData;
    }
}
