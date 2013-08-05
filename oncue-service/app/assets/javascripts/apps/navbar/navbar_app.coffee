App.module 'Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

  class Navbar.Controller extends Marionette.Controller

    listNavbar: ->
      Navbar.List.Controller.listNavbar()

  Navbar.addInitializer ->
    Navbar.controller = new Navbar.Controller()
    Navbar.on('start', ->
      Navbar.controller.listNavbar()
    )