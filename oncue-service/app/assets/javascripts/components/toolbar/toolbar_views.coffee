App.module "Components.Toolbar", (Toolbar, App, Backbone, Marionette, $, _) ->

  #
  # A layout for presenting filter drop-downs on the left and buttons on
  # the right of a toolbar
  #
  class Toolbar.View extends Marionette.CompositeView

  #
  # A toolbar capable of displaying filter dropdowns and buttons
  #
  class Toolbar.View extends Marionette.CompositeView
    template: '#toolbar_view'
    className: 'well well-small'
    itemViewContainer: '.toolbar-items'
    itemViewEventPrefix: 'toolbar'

    getItemView: (item) ->
      if item instanceof Toolbar.ButtonModel
        return Toolbar.ButtonView
      else if item instanceof Toolbar.ButtonStripModel
        return Toolbar.ButtonStripView
      else if item instanceof Toolbar.FilterModel
        return Toolbar.FilterView
      else
        throw "Cannot determine a view for #{item}"

    # Map model items to collections for some views
    itemViewOptions: (model, index) ->
      options = {}
      if model instanceof Toolbar.FilterModel
        options['collection'] = model.get('menuItems')
      if model instanceof Toolbar.ButtonStripModel
        options['collection'] = model.get('buttons')
      return options

  #
  # Display a toolbar button
  #
  class Toolbar.ButtonView extends Marionette.ItemView
    template: '#toolbar_button_view'
    tagName: 'button'
    className: -> "btn #{@model.get('cssClasses')}"
    events:
      'click' : '_handleClick'
    modelEvents:
      'change' : 'render'
    _handleClick: (event) ->
      if @model.get('enabled')
        @trigger(@model.get('event'))

    onRender: ->
      if @model.get('enabled')
        @$el.removeClass('disabled')
      else
        @$el.addClass('disabled')
      tooltip = @model.get('tooltip')
      if tooltip
        @$el.tooltip(
          title: tooltip
          container: 'body'
          delay: { show: 500, hide: 100 }
        )

    onClose: ->
      @$el.tooltip('destroy')

  #
  # Display a toolbar button strip
  #
  class Toolbar.ButtonStripView extends Marionette.CollectionView
    className: -> "btn-group #{@model.get('cssClasses')}"
    itemView: Toolbar.ButtonView
    itemViewEventPrefix: 'buttonStrip'

  #
  # Display a toolbar filter dropdown menu item
  #
  class Toolbar.FilterItemView extends Marionette.ItemView
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
  class Toolbar.FilterView extends Marionette.CompositeView
    template: '#toolbar_filter_view'
    className: 'btn-group'
    itemView: Toolbar.FilterItemView
    itemViewContainer: 'ul.dropdown-menu'
    itemViewEventPrefix: 'filter'
    modelEvents:
      'change filtered' : '_changeFiltered'

    _changeFiltered: ->
      @render()