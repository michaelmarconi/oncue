App.module 'Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

  class Navbar.Controller extends Marionette.Controller

    listNavbar: ->
      # TODO rename Controller to controller
      Navbar.List.Controller.listNavbar()

  Navbar.addInitializer ->
    Navbar.controller = new Navbar.Controller()
    Navbar.on('start', ->
      Navbar.controller.listNavbar()
    )