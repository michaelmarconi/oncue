App.module "Components.Grid", (Grid, App, Backbone, Marionette, $, _) ->

  class Grid.Controller extends Marionette.Controller

    _buildEmptyView: =>
      @emptyView = new @EmptyView()

    _closeEmptyView: ->
      if @emptyView
        @emptyView.close()

    _collectionChanged: (item) =>
      @_closeEmptyView()
      if not @gridView
        @_buildGrid()
        @_layoutGrid()

    _initialEvents: ->
      if @collection
        @listenTo(@collection, 'add', @_collectionChanged, this)
        @listenTo(@collection, 'reset', @_collectionChanged, this)

    _layoutGrid: ->
      @layout.gridRegion.show(@gridView)
      if @paginatorView
        @layout.paginatorRegion.show(@paginatorView)

    _buildGrid: =>
      @backgridOptions['collection'] = @collection
      @gridView = new Grid.View(@backgridOptions)
      if @paginated
        @paginatorView = new Grid.PaginatorView(
          collection: @collection
        )

    #
    # Show a grid, with optional pagination and empty view.
    #
    # Returns App.Components.Grid.Layout
    #
    createGrid: (model) ->

      @EmptyView = model.get('emptyView')
      @collection = model.get('items')
      @paginated = model.get('paginated')
      @backgridOptions = model.get('backgrid')

      @_initialEvents()

      if @collection and @collection.length > 0
        @_buildGrid()
      else if @EmptyView
        @_buildEmptyView()
      else
        throw 'Collection is empty and no empty view was specified'

      @layout = new Grid.Layout()
      @layout.on('show', =>
        if @collection and @collection.length > 0
          @_layoutGrid()
        else if @EmptyView
          @layout.gridRegion.show(@emptyView)
      )
      return @layout