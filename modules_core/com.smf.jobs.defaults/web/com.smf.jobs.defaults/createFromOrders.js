OB = OB || {};
OB.DJOBS = OB.DJOBS || {};
OB.DJOBS.CreateFromOrders = {};

OB.DJOBS.CreateFromOrders.onChangeLineIncludeTaxes = function(item, view, form, grid) {
    const orderGrid = form.getItem('orderGrid').canvas.viewGrid;
    let newCriteria = orderGrid.addSelectedIDsToCriteria(
                            orderGrid.getCriteria(),
                            true
                          )
    newCriteria.criteria = newCriteria.criteria || [];
    // add dummy criterion to force fetch
    newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
    orderGrid.invalidateCache();
    form.redraw();
}