/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Task "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.task.TaskSettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-task-settings-form',
  requires: [
    'NX.Conditions'
  ],

  api: {
    submit: 'NX.direct.coreui_Task.update'
  },
  settingsFormSuccessMessage: function(data) {
    return 'Task updated: ' + data['name'] + ' (' + data['typeName'] + ')';
  },

  editableMarker: 'You do not have permission to update tasks or task is not user manageable',

  items: [
    {
      xtype: 'hiddenfield',
      name: 'id'
    },
    {
      xtype: 'checkbox',
      fieldLabel: NX.I18n.get('ADMIN_TASKS_SETTINGS_ENABLED'),
      helpText: NX.I18n.get('ADMIN_TASKS_SETTINGS_ENABLED_HELP'),
      name: 'enabled',
      allowBlank: false,
      checked: true,
      editable: true
    },
    {
      name: 'name',
      fieldLabel: NX.I18n.get('ADMIN_TASKS_SETTINGS_NAME'),
      helpText: NX.I18n.get('ADMIN_TASKS_SETTINGS_NAME_HELP'),
      emptyText: NX.I18n.get('ADMIN_TASKS_SETTINGS_NAME_PLACEHOLDER')
    },
    {
      xtype: 'nx-email',
      name: 'alertEmail',
      fieldLabel: NX.I18n.get('ADMIN_TASKS_SETTINGS_EMAIL'),
      helpText: NX.I18n.get('ADMIN_TASKS_SETTINGS_EMAIL_HELP'),
      allowBlank: true
    },
    { xtype: 'nx-coreui-formfield-settingsfieldset' }
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.editableCondition = me.editableCondition || NX.Conditions.and(
        NX.Conditions.isPermitted('nexus:tasks', 'update'),
        NX.Conditions.formHasRecord('nx-coreui-task-settings-form', function(model) {
          return model.get('schedule') !== 'internal';
        })
    );

    me.callParent(arguments);

    Ext.override(me.getForm(), {
      /**
       * @override
       * Additionally, gets value of properties.
       */
      getValues: function() {
        var values = this.callParent(arguments);

        values.properties = me.down('nx-coreui-formfield-settingsfieldset').exportProperties(values);
        return values;
      },

      /**
       * @override
       * Additionally, sets properties values.
       */
      loadRecord: function(model) {
        var taskTypeModel = NX.getApplication().getStore('TaskType').getById(model.get('typeId')),
            settingsFieldSet = me.down('nx-coreui-formfield-settingsfieldset');

        this.callParent(arguments);

        if (taskTypeModel) {
          settingsFieldSet.importProperties(model.get('properties'), taskTypeModel.get('formFields'));
        }
      },

      /**
       * @override
       * Additionally, marks invalid properties.
       */
      markInvalid: function(errors) {
        this.callParent(arguments);
        me.down('nx-coreui-formfield-settingsfieldset').markInvalid(errors);
      }
    });
  }

});
