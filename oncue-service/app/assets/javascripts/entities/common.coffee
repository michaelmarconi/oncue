OnCue.module "Entities.Common", (Common, OnCue, Backbone, Marionette, $, _) ->

  #
  # A collection decorator that allows a filter funtion to be applied.
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
