App.module "Entities.Common", (Common, App, Backbone, Marionette, $, _) ->

  #
  # A collection decorator that allows a filter funtion to be applied.  This decorator can
  # cope with a Backbone.PageableCollection, in which case it will reset the underlying collection.
  #
  class Common.FilteredCollection extends Backbone.Collection

    initialize: (options) ->
      @filtered = new Backbone.Collection()
      @listenToOnce(this, 'reset', (collection) ->
        @filtered.reset(collection.models)
      )
      @listenTo(this, 'add', (model) ->
        @_applyFilter()
      )
      @listenTo(this, 'remove', (model) ->
        @_applyFilter()
      )
      @listenTo(this, 'change', (model) ->
        @_applyFilter()
      )

    _applyFilter: =>
      if @filterFunction
        @filtered.reset(_.filter(@models, @filterFunction))
      else
        @filtered.reset(@models)

    setFilter: (@filterFunction) ->
      @_applyFilter()

    clearFilter: =>
      if filterFunction
        delete filterFunction
        @filtered.reset(@models)
