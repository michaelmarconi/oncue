App.module "Navbar.List", (List, App, Backbone, Marionette, $, _) ->

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

  class List.NavbarView extends Marionette.CompositeView
    template: '#navbar_list_view'
    className: 'navbar navbar-fixed-top'
    itemView: List.NavbarItemView
    itemViewContainer: 'ul'