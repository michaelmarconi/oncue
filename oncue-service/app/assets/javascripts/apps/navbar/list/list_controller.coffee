OnCue.module "Navbar.List", (List, OnCue, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    listNavbar: ->
      connection = OnCue.reqres.request('websocket:connection')
      connectionView = new List.ConnectionView(
        model: connection
      )

      navbarItems = OnCue.reqres.request('navbar:entities')
      navbarItemsView = new List.NavbarItemCollectionView(
        collection: navbarItems
      )
      navbarItemsView.on('itemview:navigate', (navbarItemView, navbarItem) ->
        url = navbarItem.get('url')
        if url is 'jobs'
          OnCue.trigger('jobs:list')
        else if url is 'agents'
          OnCue.trigger('agents:list')
        else
          throw 'No such module: ' + url
      )

      layout = new List.Layout()
      layout.on('show', ->
        layout.connectionRegion.show(connectionView)
        layout.navbarItemsRegion.show(navbarItemsView)
      )
      OnCue.navbarRegion.show(layout)

    setActiveNavbarItem: (navbarUrl) ->
      navbarItems = OnCue.reqres.request('navbar:entities')
      navbarItemToSelect = navbarItems.find( (navbarItem) ->
        navbarItem.get('url') is navbarUrl
      )
      navbarItemToSelect.select() # Using Backbone.picky
      navbarItems.trigger('reset')