isc.ClassFactory.defineClass('SalesOrderTabLink', DirectTabLink);

isc.SalesOrderTabLink.addProperties({
  getTabAndRecordId: function(record, callback) {
    var tabId = "186"; // Fixed tab ID for "Sales Order"
    var recordId = record.id;

    if (!recordId) {
      console.error("Error: Record ID not found in the record.");
      callback({ tabId: null, recordId: null });
      return;
    }

    callback({ tabId: tabId, recordId: recordId });
  }
});
