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
 * Metrics controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Metrics', {
  extend: 'Ext.app.Controller',
  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'support.Metrics'
  ],
  refs: [
    {
      ref: 'metrics',
      selector: 'nx-coreui-support-metrics'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Support/Metrics',
      view: { xtype: 'nx-coreui-support-metrics' },
      description: 'Provides server metrics',
      iconConfig: {
        file: 'chart_pie.png',
        variants: ['x16', 'x32']
      },
      visible: function () {
        return NX.Permissions.check('nexus:metrics-endpoints');
      }
    });

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.load
        }
      },
      component: {
        'nx-coreui-support-metrics': {
          afterrender: me.load
        },
        'nx-coreui-support-metrics button[action=download]': {
          'click': me.downloadMetrics
        },
        'nx-coreui-support-metrics button[action=threads]': {
          'click': me.downloadThreads
        }
      }
    });
  },

  /**
   * Load metrics information and update charts.
   *
   * @private
   */
  load: function () {
    var me = this,
        panel = me.getMetrics();

    if (!panel) {
      return;
    }

    panel.getEl().mask('Loading...');

    Ext.Ajax.request({
      url: NX.util.Url.urlOf('internal/metrics'),
      method: 'GET',
      headers: {
        'accept': 'application/json'
      },
      scope: me,
      suppressStatus: true,

      callback: function (response) {
        panel.getEl().unmask();
      },

      failure: function (response) {
        NX.Messages.add({ type: 'warning', text: 'Failed to refresh metrics data' });
      },

      success: function (response) {
        var data = Ext.decode(response.responseText);

        // update memory charts
        var memory = data.jvm.memory;
        panel.setTotalData([
          { value: Math.round((memory.totalUsed / memory.totalMax) * 100) }
        ]);
        panel.setMemoryDistData([
          { name: 'Heap', data: memory.heapUsed },
          { name: 'Non-Heap', data: memory.totalUsed - memory.heapUsed },
          { name: 'Available', data: memory.totalMax - memory.totalUsed }
        ]);

        // update threads charts
        var threads = data.jvm['thread-states'];
        panel.setThreadStatesData([
          { name: 'New', data: threads.new },
          { name: 'Terminated', data: threads.terminated },
          { name: 'Blocked', data: threads.blocked },
          { name: 'Runnable', data: threads.runnable },
          { name: 'Timed Waiting', data: threads.timed_waiting },
          { name: 'Waiting', data: threads.waiting }
        ]);
      }
    });
  },

  /**
   * @private
   * Download metrics data.
   */
  downloadMetrics: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('internal/metrics?pretty=true&download=true'));
  },

  /**
   * @private
   * Download thread dump.
   */
  downloadThreads: function () {
    NX.util.DownloadHelper.downloadUrl(NX.util.Url.urlOf('internal/threads?download=true'));
  }

});