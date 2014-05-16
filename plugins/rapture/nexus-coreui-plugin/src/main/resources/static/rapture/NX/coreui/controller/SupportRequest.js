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
/**
 * Support Request controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.SupportRequest', {
  extend: 'Ext.app.Controller',

  views: [
    'support.SupportRequest'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Support Request',
      description: 'Make a support request',
      view: { xtype: 'nx-coreui-support-supportrequest' },
      iconConfig: {
        file: 'support.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      component: {
        'nx-coreui-support-supportrequest button[action=makerequest]': {
          click: me.makeRequest
        }
      }
    });
  },

  /**
   * @private
   * Open sonatype support in a new browser window/tab.
   */
  makeRequest: function () {
    var win = window.open('https://support.sonatype.com/anonymous_requests/new');
    if (win == null) {
      alert('Window pop-up are blocked!');
    }
  }

});