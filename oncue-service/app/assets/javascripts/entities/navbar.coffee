App.module 'Entities.Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

  #
  # A navigation link on the navigation bar
  #
  class Navbar.Item extends Backbone.Model
    initialize: ->
      selectable = new Backbone.Picky.Selectable(this)
      _.extend(this, selectable)

  #
  # The collection of navigation links on the navigation bar
  #
  class Navbar.ItemCollection extends Backbone.Collection
    model: Navbar.Item

    initialize: ->
      singleSelect = new Backbone.Picky.SingleSelect(this)
      _.extend(this, singleSelect)

  # ~~~~~~~~~~~

  class Navbar.Controller extends Marionette.Controller
    getNavbarEntities: ->
      return Navbar.itemCollection

  Navbar.addInitializer ->
    Navbar.itemCollection = new Navbar.ItemCollection([
      new Navbar.Item(
        name: 'Jobs'
        url: 'jobs'
      )
      new Navbar.Item(
        name: 'Agents'
        url: 'agents'
      )
    ])

    Navbar.controller = new Navbar.Controller()

    App.reqres.setHandler('navbar:entities', ->
      Navbar.controller.getNavbarEntities()
    )