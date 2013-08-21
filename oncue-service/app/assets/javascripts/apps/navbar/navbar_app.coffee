App.module 'Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

  class Navbar.Controller extends Marionette.Controller

    listNavbar: ->
      if not Navbar.List.controller
        Navbar.List.controller = new Navbar.List.Controller()
      Navbar.List.controller.listNavbar()

  # ~~~~~~~~~~~~

  Navbar.addInitializer ->
    Navbar.controller = new Navbar.Controller()
    Navbar.on('start', ->
      Navbar.controller.listNavbar()
    )