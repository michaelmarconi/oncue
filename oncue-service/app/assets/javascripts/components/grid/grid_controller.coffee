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
        @listenTo(@collection, 'add', @_collectionChanged)
        @listenTo(@collection, 'reset', @_collectionChanged)

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

    initialize: (options) ->
      if not options['model'] then throw 'You must supply a grid model'
      model = options['model']

      # Remove all previous event handlers on this controller
      @stopListening()

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
      @listenTo(@layout, 'show', =>
        if @collection and @collection.length > 0
          @_layoutGrid()
        else if @EmptyView
          @layout.gridRegion.show(@emptyView)
      )

    #
    # Access the layout for this grid, once it has been created.
    #
    getLayout: =>
      if not @layout then throw 'The grid must be created first'
      return @layout

    #
    # Get the list of models that are currently selected
    #
    getSelectedModels: =>
      if not @gridView then throw 'The grid must be created first'
      return @gridView.getSelectedModels()