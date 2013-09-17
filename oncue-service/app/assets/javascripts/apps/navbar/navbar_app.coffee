OnCue.module 'Navbar', (Navbar, OnCue, Backbone, Marionette, $, _) ->

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

  Navbar.addFinalizer ->
    Navbar.controller.close()
    delete Navbar.controller