isc.ClassFactory.defineClass('SMFSWSGenerateKeyButton', isc.OBFormButton);
isc.SMFSWSGenerateKeyButton.addProperties({
    title: OB.I18N.getLabel('SMFSWS_GeneratePrivateKey'),
    width:200,
    click: function(){
        let form = this.canvasItem.form;
        let pkField = form.getFieldFromFieldName('privateKey');
        pkField.setValue(OB.Utilities.generateRandomString(64,true,true,true,true));
        pkField.handleChanged();
    }
});