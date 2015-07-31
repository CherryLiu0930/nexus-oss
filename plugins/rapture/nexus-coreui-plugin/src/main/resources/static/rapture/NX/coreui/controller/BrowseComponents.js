/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
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
 * Browse components controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.BrowseComponents', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list',
    'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list',
    'nx-coreui-browsecomponentfeature nx-coreui-component-asset-list'
  ],
  stores: [
    'Component',
    'ComponentAsset'
  ],
  models: [
    'Component'
  ],

  views: [
    'browse.BrowseComponentFeature',
    'browse.BrowseComponentList',
    'browse.BrowseRepositoryList'
  ],

  refs: [
    {ref: 'feature', selector: 'nx-coreui-browsecomponentfeature'},
    {ref: 'repositoryList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list'},
    {ref: 'componentList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list'},
    {ref: 'assetList', selector: 'nx-coreui-browsecomponentfeature nx-coreui-component-asset-list'},
    {ref: 'componentDetails', selector: 'nx-coreui-browsecomponentfeature nx-coreui-component-details'},
    {ref: 'componentFilter', selector: 'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list #filter'}
  ],

  features: {
    mode: 'browse',
    path: '/Browse/Components',
    text: NX.I18n.get('Browse_Components_Title_Feature'),
    description: NX.I18n.get('Browse_Components_Description_Feature'),
    view: 'NX.coreui.view.browse.BrowseComponentFeature',
    iconConfig: {
      file: 'box_front.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.checkExistsWithPrefix('nexus:repository-view');
    },
    authenticationRequired: false
  },

  icons: {
    'browse-component-default': {file: 'database.png', variants: ['x16', 'x32']},
    'browse-component': {file: 'box_front.png', variants: ['x16', 'x32']},
    'browse-component-detail': {file: 'box_front_open.png', variants: ['x16', 'x32']}
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Repository': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-browsecomponentfeature nx-coreui-browse-repository-list': {
          beforerender: me.onBeforeRender
        },
        'nx-coreui-browsecomponentfeature nx-coreui-browse-component-list #filter': {
          search: me.onSearch,
          searchcleared: me.onSearchCleared
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   * When a list managed by this controller is clicked, route the event to the proper handler
   */
  onSelection: function(list, model) {
    var me = this,
        modelType;

    // Figure out what kind of list we’re dealing with
    modelType = model.id.replace(/^.*?model\./, '').replace(/\-.*$/, '');

    if (modelType === "RepositoryReference") {
      me.onRepositorySelection(model);
    }
    else if (modelType === "Component") {
      me.onComponentSelection(model);
    }
    else if (modelType === "Asset") {
      me.onAssetSelection(model);
    }
  },

  /**
   * @private
   *
   * Load browse results for selected repository.
   *
   * @param {NX.coreui.model.Repository} model selected repository
   */
  onRepositorySelection: function(model) {
    var me = this,
        componentStore = me.getStore('Component'),
        componentList = me.getComponentList(),
        componentFilter = me.getComponentFilter();

    componentStore.filters.removeAtKey('filter');
    componentFilter.clearSearch();
    componentList.getSelectionModel().deselectAll();
    componentStore.addFilter([
      {
        id: 'repositoryName',
        property: 'repositoryName',
        value: model.get('name')
      }
    ]);
  },

  /**
   * @private
   *
   * Show component details and load assets for selected component.
   *
   * @param {NX.coreui.model.Component} model selected component
   */
  onComponentSelection: function(model) {
    var me = this;

    me.getComponentDetails().setComponentModel(model);
    me.getAssetList().setComponentModel(model);
  },

  /**
   * Load asset container for selected asset
   *
   * @private
   * @param {NX.coreui.model.Asset} model selected asset
   */
  onAssetSelection: function(model) {
    this.getController('NX.coreui.controller.Assets').updateAssetContainer(null, null, null, model);
  },

  /**
   * @override
   * Load all of the stores associated with this controller.
   */
  loadStores: function() {
    if (this.getFeature()) {
      if (this.currentIndex === 0) {
        this.getRepositoryList().getStore().load();
      }
      if (this.currentIndex === 1) {
        this.getComponentList().getStore().load();
      }
      if (this.currentIndex === 2) {
        this.getAssetList().getStore().load();
      }
    }
  },

  /**
   * @private
   * Load stores based on the bookmarked URL
   */
  onBeforeRender: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        repoStore = me.getRepositoryList().getStore(),
        repoModel,
        componentStore = me.getComponentList().getStore(),
        componentModel;

    repoStore.load(function() {
      // Load the asset detail view
      if (list_ids[2]) {
        repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, repoModel);
        me.onRepositorySelection(repoModel);
        componentStore.load(function() {
          componentModel = me.getComponentList().getStore().getById(decodeURIComponent(list_ids[1]));
          me.onModelChanged(1, componentModel);
          me.onComponentSelection(componentModel);
          me.getAssetList().getStore().load(function() {
            me.reselect();
          });
        });
      }
      // Load the asset list view
      else if (list_ids[1]) {
        repoModel = repoStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, repoModel);
        me.onRepositorySelection(repoModel);
        componentStore.load(function() {
          me.reselect();
        });
      }
      // Load the component list view
      else if (list_ids[0]) {
        me.reselect();
      }
    });
  },

  /**
   * @private
   * Filter grid.
   *
   * @private
   * @param {NX.ext.SearchBox} searchBox component
   * @param {String} value to be searched
   */
  onSearch: function(searchBox, value) {
    var grid = searchBox.up('grid'),
        store = grid.getStore(),
        emptyText = grid.getView().emptyTextFilter;

    if (!grid.emptyText) {
      grid.emptyText = grid.getView().emptyText;
    }
    grid.getView().emptyText = '<div class="x-grid-empty">' + emptyText.replace(/\$filter/, value) + '</div>';
    grid.getSelectionModel().deselectAll();
    store.addFilter([
      {
        id: 'filter',
        property: 'filter',
        value: value
      }
    ]);
  },

  /**
   * Clear filtering on grid.
   *
   * @private
   * @param {NX.ext.SearchBox} searchBox component
   */
  onSearchCleared: function(searchBox) {
    var grid = searchBox.up('grid'),
        store = grid.getStore();

    if (grid.emptyText) {
      grid.getView().emptyText = grid.emptyText;
    }
    grid.getSelectionModel().deselectAll();
    // we have to remove filter directly as store#removeFilter() does not work when store#remoteFilter = true
    if (store.filters.removeAtKey('filter')) {
      if (store.filters.length) {
        store.filter();
      }
      else {
        store.clearFilter();
      }
    }
  }

});
