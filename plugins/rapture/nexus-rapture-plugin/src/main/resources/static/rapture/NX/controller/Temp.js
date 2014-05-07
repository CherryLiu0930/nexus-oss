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
 * FIXME: remove me
 * @since
 */
Ext.define('NX.controller.Temp', {
  extend: 'Ext.app.Controller',
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    // HACK: Show some items only if user is logged in for testing
    var visibleIfLoggedIn = function () {
      return Ext.isDefined(NX.State.getUser());
    };

    me.getApplication().getFeaturesController().registerFeature([
      {
        mode: 'browse',
        path: '/Repository/Trash',
        description: 'Browse repository trash',
        iconConfig: {
          file: 'bin.png',
          variants: ['x16', 'x32']
        },
        visible: visibleIfLoggedIn,
        weight: 500
      },

      // user mode
      {
        mode: 'user',
        path: '/Notifications',
        iconConfig: {
          file: 'emails.png',
          variants: ['x16', 'x32']
        },
        visible: visibleIfLoggedIn
      },
      {
        mode: 'user',
        path: '/Client Settings',
        group: true,
        weight: 200,
        iconConfig: {
          file: 'setting_tools.png',
          variants: ['x16', 'x32']
        }
      },
      {
        mode: 'user',
        path: '/Client Settings/Apache Maven',
        description: 'Settings for use with Apache Maven',
        iconConfig: {
          file: 'apache_handlers.png',
          variants: ['x16', 'x32']
        },
        visible: visibleIfLoggedIn
      },
      {
        mode: 'user',
        path: '/Client Settings/Apache Ivy',
        description: 'Settings for use with Apache Ivy',
        iconConfig: {
          file: 'apache_handlers.png',
          variants: ['x16', 'x32']
        },
        visible: visibleIfLoggedIn
      }
    ]);
  }
});