OnCue.module "Components.Toolbar", (Toolbar, OnCue, Backbone, Marionette, $, _) ->

  class Toolbar.Controller extends Marionette.Controller

    initialize: (options) ->
      if not options['collection'] then throw 'You must supply a collection of toolbar items'
      toolbarItems = options['collection']

      # Remove all previous event handlers on this controller
      @stopListening()

      @view = new Toolbar.View(
        collection: toolbarItems
      )

      # Listen to toolbar filter item selection changes and update the filter (filtered state)
      # and filter item (checked/unchecked) views
      @listenTo(@view, 'toolbar:filter:item:changed', (filterView, filterItemView) ->
        filter = filterView.model
        filterItem = filterItemView.model
        filterItems = filterView.collection.models
        filterItem.set('selected', !filterItem.get('selected'))
        allItemsSelected = _.every(filterItems, (item) ->
          item.get('selected') is true
        )
        if allItemsSelected
          filter.set('filtered', false)
        else
          filter.set('filtered', true)

        # Emit all filter models as part of the event, to allow
        # the consumer to perform filtering based on the state of
        # ALL filters, not just this one!
        filterModels = []
        for toolbarItem in toolbarItems.models
          if toolbarItem instanceof Toolbar.FilterModel
            filterModels.push(toolbarItem)
        @view.trigger(filter.get('event'), filterModels)
      )
      return this

    #
    # Access the view for this toolbar, once it has been created.
    #
    getView: ->
      if not @view then throw 'No toolbar view exists!'
      return @view