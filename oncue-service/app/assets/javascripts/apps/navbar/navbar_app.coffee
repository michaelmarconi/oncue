App.module 'Navbar', (Navbar, App, Backbone, Marionette, $, _) ->

  API =
    listNavbar: ->
      Navbar.List.Controller.listNavbar()

  #------------------------------------------

  Navbar.on('start', ->
    API.listNavbar()
  )