App.module 'Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

#  class Navbar.Controller extends Marionette.Controller
#
#    listNavbar: ->
#      Navbar.List.controller.listNavbar()
#
#  # ~~~~~~~~~~~~

  Navbar.addInitializer ->
#    Navbar.controller = new Navbar.Controller()
    Navbar.List.controller = new Navbar.List.Controller()
    Navbar.on('start', ->
      Navbar.List.controller.listNavbar()
    )