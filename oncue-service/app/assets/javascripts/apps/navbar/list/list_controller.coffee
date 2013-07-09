App.module "Navbar.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    @listNavbar: ->
      navbarItems = App.reqres.request('navbar:entities')
      navbarView = new List.NavbarView(
        collection: navbarItems
      )
      navbarView.on('itemview:navigate', (navbarItemView, navbarItem) ->
        url = navbarItem.get('url')
        if url is 'jobs'
          App.trigger('jobs:list')
        else
          throw 'No such module: ' + url
      )
      App.navbarRegion.show(navbarView)

    @setActiveNavbarItem: (navbarUrl) ->
      navbarItems = App.reqres.request('navbar:entities')
      navbarItemToSelect = navbarItems.find( (navbarItem) ->
        navbarItem.get('url') is navbarUrl
      )
      navbarItemToSelect.select() # Using Backbone.picky
      navbarItems.trigger('reset')