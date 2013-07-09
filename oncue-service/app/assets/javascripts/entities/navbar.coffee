App.module 'Entities', (Entities, App, Backbone, Marionette, $, _) ->

  class Entities.NavbarItem extends Backbone.Model
    initialize: ->
      selectable = new Backbone.Picky.Selectable(this)
      _.extend(this, selectable)

  class Entities.NavbarItems extends Backbone.Collection
    model: Entities.NavbarItem

    initialize: ->
      singleSelect = new Backbone.Picky.SingleSelect(this)
      _.extend(this, singleSelect)

  API =
    getNavbarItems: ->
      Entities.navbarItems

  Entities.addInitializer ->
    Entities.navbarItems = new Entities.NavbarItems([
      new Entities.NavbarItem(
        name: 'Jobs'
        url: 'jobs'
      )
      new Entities.NavbarItem(
        name: 'Agents'
        url: 'agents'
      )
    ])

  App.reqres.setHandler('navbar:entities', ->
    API.getNavbarItems()
  )