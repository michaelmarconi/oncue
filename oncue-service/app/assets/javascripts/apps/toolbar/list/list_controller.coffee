App.module "Toolbar.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    @createToolbar: (toolbarItems) ->
      toolbarView = new List.ToolbarView(
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
        @trigger(filter.get('event'), filterItems)
        )
      return toolbarView