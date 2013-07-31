App.module "Components.Toolbar", (Toolbar, App, Backbone, Marionette, $, _) ->

  class Toolbar.Controller extends Marionette.Controller

    createToolbar: (toolbarItems) ->
      toolbarView = new Toolbar.View(
        collection: toolbarItems
      )
      toolbarView.on('toolbar:button:clicked', (buttonView) ->
        button = buttonView.model
        @trigger(button.get('event'))
      )

      # Listen to toolbar filter item selection changes and update the filter (filtered state)
      # and filter item (checked/unchecked) views
      toolbarView.on('toolbar:filter:item:changed', (filterView, filterItemView) ->
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

        @trigger(filter.get('event'), filterModels)
        )
      return toolbarView