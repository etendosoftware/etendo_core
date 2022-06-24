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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.Datasource.create({
  createClassName: '',
  ID: '090A37D22E61FE94012E621729090048',
  potentiallyShared: true,
  dataURL: OB.Utilities.applicationUrl(
    'org.openbravo.service.datasource/090A37D22E61FE94012E621729090048'
  ),
  requestProperties: {
    params: {
      _contextUrl: OB.Utilities.getLocationUrlWithoutFragment(),
      _skinVersion: 'Default',
      Constants_IDENTIFIER: OB.Constants.IDENTIFIER,
      Constants_FIELDSEPARATOR: OB.Constants.FIELDSEPARATOR
    }
  },
  fields: [
    {
      name: 'id',
      type: '_id_13',
      primaryKey: true
    },
    {
      name: 'client',
      type: '_id_19'
    },
    {
      name: 'client$_identifier'
    },
    {
      name: 'organization',
      type: '_id_19'
    },
    {
      name: 'organization$_identifier'
    },
    {
      name: 'table',
      type: '_id_19'
    },
    {
      name: 'table$_identifier'
    },
    {
      name: 'record',
      type: '_id_10'
    },
    {
      name: 'note',
      type: '_id_14'
    },
    {
      name: 'isactive',
      type: '_id_20'
    },
    {
      name: 'creationDate',
      type: '_id_16'
    },
    {
      name: 'createdBy',
      type: '_id_30'
    },
    {
      name: 'createdBy$_identifier'
    },
    {
      name: 'updated',
      type: '_id_16'
    },
    {
      name: 'updatedBy',
      type: '_id_30'
    },
    {
      name: 'updatedBy$_identifier'
    }
  ]
});
