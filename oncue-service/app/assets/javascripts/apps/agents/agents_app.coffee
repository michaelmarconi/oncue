App.module 'Agents', (Agents, App, Backbone, Marionette, $, _) ->

  # Don't start this module automatically
  @startWithParent = false

  class Agents.Router extends Marionette.AppRouter
    appRoutes:
      'agents' : 'listAgents'

  class Agents.Controller extends Marionette.Controller

    listAgents: ->
      Agents.List.controller.listAgents()
      App.Navbar.List.controller.setActiveNavbarItem('agents')

  # ~~~~~~~~~~~~

  Agents.addInitializer ->
    Agents.controller = new Agents.Controller()
    new Agents.Router(
      controller: Agents.controller
    )
    @listenTo(App, 'agents:list', ->
      App.navigate('agents')
      Agents.controller.listAgents()
    )

  Agents.addFinalizer ->
    @stopListening()
    Agents.controller.close()
    delete Agents.controller