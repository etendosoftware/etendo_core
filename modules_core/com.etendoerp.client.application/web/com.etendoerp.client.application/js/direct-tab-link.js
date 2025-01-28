isc.ClassFactory.defineClass('DirectTabLink', isc.OBGridFormLabel);

isc.DirectTabLink.addProperties({
  height: 1,
  width: 1,
  overflow: 'visible',

  // Method that subclasses must override
  getTabAndRecordId: function(record, callback) {
    callback({ tabId: null, recordId: null });
  },

  setRecord: function(record) {
    var value = record[this.field.name];

    // Call the asynchronous function from the subclass
    this.getTabAndRecordId(record, function(result) {
      var tabId = result.tabId;
      var recordId = result.recordId;

      if (!value || !tabId || !recordId) {
        this.setContents("");
        return;
      }

      // Create the inline function for onclick
      var linkHTML =
        "<a href='#' style='color:blue; text-decoration:underline;' " +
        "onclick='OB.Utilities.openDirectTab(\"" + tabId + "\", \"" + recordId + "\"); return false;'>" +
        value + "</a>";

      this.setContents(linkHTML);

      // Force grid redraw
      if (this.grid && this.grid.body) {
        this.grid.body.markForRedraw();
      }
    }.bind(this));
  }
});
