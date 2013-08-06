App.module 'Agents', (Agents, App, Backbone, Marionette, $, _) ->

  class Agents.Router extends Marionette.AppRouter
    appRoutes:
      'agents' : 'listAgents'

  class Agents.Controller extends Marionette.Controller

    listAgents: ->
      Agents.List.controller.listAgents()
      App.Navbar.List.Controller.setActiveNavbarItem('agents')

  # ~~~~~~~~~~~~

  Agents.addInitializer ->
    Agents.controller = new Agents.Controller()
    new Agents.Router(
      controller: Agents.controller
    )
    App.on('agents:list', ->
      App.navigate('agents')
      Agents.controller.listAgents()
    )