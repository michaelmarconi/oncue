App.module "Toolbar.List", (List, App, Backbone, Marionette, $, _) ->

  #
  # A toolbar capable of displaying filter dropdowns and buttons
  #
  class List.ToolbarView extends Marionette.CompositeView
    template: '#toolbar_view'
    className: 'well well-small'
    itemViewContainer: '.toolbar-items'
    itemViewEventPrefix: 'toolbar'

    getItemView: (item) ->
      if item instanceof App.Entities.ToolbarButton
        return List.ToolbarButtonView
      else if item instanceof App.Entities.ToolbarFilter
        return List.ToolbarFilterView
      else
        throw "Cannot determine a view for #{item}"

    # Inject the toolbar filter menu items into
    # a ToolbarFilterView as a collection on view creation
    itemViewOptions: (model, index) ->
      options = {}
      if model instanceof App.Entities.ToolbarFilter
        options['collection'] = model.get('menuItems')
      return options

  #
  # Display a toolbar button
  #
  class List.ToolbarButtonView extends Marionette.ItemView
    template: '#toolbar_button_view'
    tagName: 'button'
    className: -> "btn #{@model.get('cssClasses')}"
    triggers:
      'click' : 'button:clicked'

  #
  # Display a toolbar filter dropdown menu item
  #
  class List.ToolbarFilterItemView extends Marionette.ItemView
    template: '#toolbar_filter_item_view'
    tagName: 'li'
    events:
      'click' : '_handleClick'
    modelEvents:
      'change selected' : '_changeSelection'

    _handleClick: (event) ->
      event.preventDefault()
      event.stopPropagation()
      @trigger('item:changed',
        item: @model
      )

    _changeSelection: ->
      if @model.get('selected') is true
        @$el.find('input').prop("checked", true)
      else
        @$el.find('input').prop("checked", false)

  #
  # Display a toolbar filter dropdown menu, with a collection of
  # dropdown items
  #
  class List.ToolbarFilterView extends Marionette.CompositeView
    template: '#toolbar_filter_view'
    className: 'btn-group'
    itemView: List.ToolbarFilterItemView
    itemViewContainer: 'ul.dropdown-menu'
    itemViewEventPrefix: 'filter'
    modelEvents:
      'change filtered' : '_changeFiltered'

    _changeFiltered: ->
      @render()