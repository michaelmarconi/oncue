App.module "Navbar.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    @listNavbar: ->
      connection = App.reqres.request('websocket:connection')
      connectionView = new List.ConnectionView(
        model: connection
      )

      navbarItems = App.reqres.request('navbar:entities')
      navbarItemsView = new List.NavbarItemCollectionView(
        collection: navbarItems
      )
      navbarItemsView.on('itemview:navigate', (navbarItemView, navbarItem) ->
        url = navbarItem.get('url')
        if url is 'jobs'
          App.trigger('jobs:list')
        else if url is 'agents'
          App.trigger('agents:list')
        else
          throw 'No such module: ' + url
      )

      layout = new List.Layout()
      layout.on('show', ->
        layout.connectionRegion.show(connectionView)
        layout.navbarItemsRegion.show(navbarItemsView)
      )
      App.navbarRegion.show(layout)

    @setActiveNavbarItem: (navbarUrl) ->
      navbarItems = App.reqres.request('navbar:entities')
      navbarItemToSelect = navbarItems.find( (navbarItem) ->
        navbarItem.get('url') is navbarUrl
      )
      navbarItemToSelect.select() # Using Backbone.picky
      navbarItems.trigger('reset')