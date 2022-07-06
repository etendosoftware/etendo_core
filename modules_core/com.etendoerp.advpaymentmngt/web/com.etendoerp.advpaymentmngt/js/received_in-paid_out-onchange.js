OB.EAPM = {};
OB.EAPM.OnChangeFunctions = {};

OB.EAPM.OnChangeFunctions.ReceivedInPaidOutOnChange = function (item, view, form, grid) {
    var itemName = item.name;
    var itemValue = item.getValue();

    if (itemName == "received_in") {
        var paid_out = form.getItem('paid_out')
        paid_out.setValue(!itemValue)
    }

    if (itemName == "paid_out") {
        var received_in = form.getItem('received_in')
        received_in.setValue(!itemValue)
    }
};