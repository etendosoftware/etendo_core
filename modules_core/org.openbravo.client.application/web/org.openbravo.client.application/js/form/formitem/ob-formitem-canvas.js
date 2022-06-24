/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

// == OBClientClassCanvasItem ==
// Extends CanvasItem, support usage of Canvas in a grid/form editor
// and in the grid itself
isc.ClassFactory.defineClass('OBClientClassCanvasItem', isc.CanvasItem);

isc.OBClientClassCanvasItem.addProperties({
  autoDestroy: true,
  // See issue https://issues.openbravo.com/view.php?id=26555
  // http://www.smartclient.com/docs/9.1/a/b/c/go.html#attr..CanvasItem.shouldSaveValue
  shouldSaveValue: true,
  // if the canvas is used somewhere else (in the statusbar) then
  // don't do placeCanvas.
  placeCanvas: function() {
    if (this.canvas && !this.canvas.inStatusBar) {
      this.Super('placeCanvas', arguments);
    }
  },

  showValue: function(displayValue, dataValue, form, item) {
    if (this.canvas && this.canvas.showValue) {
      this.canvas.showValue(displayValue, dataValue, form, item);
    }
  },

  createCanvas: function() {
    var canvas, clientClassArray, clientClass, clientClassProps;

    clientClassArray = OB.Utilities.clientClassSplitProps(this.clientClass);
    clientClass = clientClassArray[0];
    clientClassProps = clientClassArray[1];

    canvas = isc.ClassFactory.newInstance(
      clientClass,
      {
        canvasItem: this
      },
      clientClassProps
    );

    if (!canvas) {
      return isc.Label.create({
        contents: 'Invalid Type ' + clientClass,
        width: 1,
        height: 1,
        overflow: 'visible',
        autoDraw: false
      });
    }

    if (canvas.noTitle) {
      this.showTitle = false;
    }

    if (this.form.itemChanged && canvas.onItemChanged) {
      canvas.observe(
        this.form,
        'itemChanged',
        'observer.onItemChanged(observed)'
      );
    }

    return canvas;
  },

  destroy: function() {
    if (this.canvas && this.form) {
      this.canvas.ignore(this.form, 'itemChanged');
    }
    return this.Super('destroy', arguments);
  },

  redrawing: function() {
    if (this.canvas.redrawingItem) {
      this.canvas.redrawingItem();
    }
    this.Super('redrawing', arguments);
  }
});

// == OBGridFormLabel ==
// Base component to add label fields in the grid. For styling purposes.
isc.defineClass('OBGridFormLabel', isc.Label);

isc.defineClass('OBTruncAddMinusDisplay', isc.OBGridFormLabel);

isc.OBTruncAddMinusDisplay.addProperties({
  height: 1,
  width: 1,
  overflow: 'visible',

  setRecord: function(record) {
    var val = record[this.field.name];
    if (
      this.field &&
      this.field.type &&
      isc.SimpleType.getType(this.field.type).normalDisplayFormatter
    ) {
      this.showValue(
        isc.SimpleType.getType(this.field.type).normalDisplayFormatter(val),
        val
      );
    } else {
      this.showValue(String(record[this.field.name]));
    }
  },

  showValue: function(displayValue, dataValue, form, item) {
    if (!dataValue || displayValue === '0') {
      this.setContents(displayValue);
    } else if (!displayValue) {
      this.setContents('');
    } else if (displayValue.startsWith('-')) {
      this.setContents(displayValue.substring(1));
    } else {
      this.setContents('-' + displayValue);
    }
  }
});

isc.defineClass('OBAddPercentageSign', isc.OBGridFormLabel);

isc.OBAddPercentageSign.addProperties({
  height: 1,
  width: 1,
  overflow: 'visible',

  setRecord: function(record) {
    var val = record[this.field.name];
    if (
      this.field &&
      this.field.type &&
      isc.SimpleType.getType(this.field.type).normalDisplayFormatter
    ) {
      this.showValue(
        isc.SimpleType.getType(this.field.type).normalDisplayFormatter(val),
        val
      );
    } else {
      this.showValue(String(record[this.field.name]));
    }
  },

  showValue: function(displayValue, dataValue, form, item) {
    if (!displayValue) {
      this.setContents('0 %');
    } else {
      this.setContents(displayValue + ' %');
    }
    if (this.grid && this.grid.body) {
      this.grid.body.markForRedraw();
    }
  }
});

isc.defineClass('OBLevelImg', isc.Img);

isc.OBLevelImg.addProperties({
  cellAlign: 'center',
  canEdit: false,
  isShownInGridEdit: true,
  // The valueList should be overridden if this canvas item is applied to a different scale, for example ['none', 'weak', 'small', 'medium', 'high']
  valuesList: [0, 1, 2, 3, 4],

  src: OB.Styles.skinsPath + 'Default/smartclient/images/blank.gif',
  width: 1,
  height: 1,

  setState: function() {
    //To avoid add "_Disabled" to the src image if the grid row is disabled
    return '';
  },

  click: function() {
    if (this.grid) {
      this.grid.selectSingleRecord(this.record);
    }
    return this.Super('click', arguments);
  },

  setValue: function(value) {
    var i;
    if (!value) {
      if (this.value) {
        value = this.value;
      } else {
        return;
      }
    }
    for (i = 0; i < this.valuesList.length; i++) {
      if (value.toString() === this.valuesList[i].toString()) {
        value = i;
      }
    }
    if (
      typeof value === 'number' &&
      value >= 0 &&
      value < this.valuesList.length
    ) {
      this.value = value;
      this.setWidth(41);
      this.setHeight(20);
      this.setSrc(
        OB.Styles.skinsPath +
          'Default/org.openbravo.client.application/images/form/levelImg_' +
          value.toString() +
          '_' +
          (this.valuesList.length - 1).toString() +
          '.png'
      );
    } else {
      this.value = null;
      this.setWidth(1);
      this.setHeight(1);
      this.setSrc(OB.Styles.skinsPath + 'Default/smartclient/images/blank.gif');
    }
  },

  showValue: function(displayValue, dataValue, form, item) {
    this.setValue(dataValue);
  },

  setRecord: function(record) {
    if (this.field && this.field.name && record[this.field.name]) {
      this.setValue(record[this.field.name]);
    }
  }
});

isc.defineClass('OBColorField', isc.OBGridFormLabel);

isc.OBColorField.addProperties({
  height: 22,
  cellAlign: 'center',
  overflow: 'visible',
  initWidget: function() {
    if (this.record && this.record.hexColor) {
      this.createField(this.record.hexColor);
    }
    this.Super('initWidget', arguments);
  },

  createField: function(hexColor) {
    if (hexColor) {
      this.setBackgroundColor(hexColor);
    }
  },

  setRecord: function(record) {
    const hexColor = record.hexColor;
    if (hexColor) {
      this.showValue(hexColor);
    }
  },

  showValue: function(displayValue, dataValue, form, item) {
    const backgroundColor = displayValue || (form && form.getValue('hexColor'));
    if (backgroundColor) {
      this.setBackgroundColor(backgroundColor);
    } else {
      this.setBackgroundColor(null);
    }

    if (this.grid && this.grid.body) {
      this.grid.body.markForRedraw();
    }
  }
});
