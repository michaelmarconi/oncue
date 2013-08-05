App.module "Navbar.List", (List, App, Backbone, Marionette, $, _) ->

  #
  # A navbar layout that renders a connection state/brand item on the left
  # and the list of navbar items next to it
  #
  class List.Layout extends Marionette.Layout
    template: '#navbar_list_layout'
    className: 'navbar navbar-fixed-top'
    regions:
      connectionRegion: '#connection_region'
      navbarItemsRegion: '#navbar_items_region'

  #
  # Render the state of the websocket connection,
  # which is part of the 'brand' element
  #
  class List.ConnectionView extends Marionette.ItemView
    template: '#connection_view'
    tagName: 'a'
    className: 'brand'
    modelEvents:
      'change' : 'render'

    onRender: ->
      state = @model.get('state')
      icon = @$el.find('i')
      icon.removeClass()
      if state == 'connecting'
        icon.addClass('icon-refresh icon-spin text-info')
      else if state == 'connected'
        icon.addClass('icon-off text-info')

  #
  # An individual navbar link item
  #
  class List.NavbarItemView extends Marionette.ItemView
    template: '#navbar_item_view'
    tagName: 'li'
    events:
      'click a' : 'navigate'

    onRender: ->
      if @model.selected
        @$el.addClass('active')

    navigate: (event) ->
      event.preventDefault()
      @trigger('navigate', @model)

  #
  # The collection of navbar items
  #
  class List.NavbarItemCollectionView extends Marionette.CollectionView
    tagName: 'ul'
    className: 'nav'
    itemView: List.NavbarItemView