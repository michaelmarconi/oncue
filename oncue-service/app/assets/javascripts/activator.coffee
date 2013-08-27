App.module 'Activator', (Activator, App, Backbone, Marionette, $, _) ->

  #--------------------------------------------------
  # This module is responsible for activating and
  # deactivating apps
  #--------------------------------------------------

  class Activator.Router extends Marionette.AppRouter
    appRoutes:
      'jobs'      : 'listJobs'
      'jobs/:id'  : 'showJob'
      'agents'    : 'listAgents'
      ''          : 'listJobs'

  class Activator.Controller extends Marionette.Controller

    listJobs: ->
      App.module('Agents').stop()
      App.module('Jobs').start()
      App.trigger('jobs:list')
      @listenToOnce(App, 'agents:list', ->
        Activator.controller.listAgents()
      )

    showJob: (id) ->
      App.module('Agents').stop()
      App.module('Jobs').start()
      App.trigger('job:show', id)
      @listenToOnce(App, 'agents:list', ->
        Activator.controller.listAgents()
      )

    listAgents: ->
      App.module('Jobs').stop()
      App.module('Agents').start()
      App.trigger('agents:list')
      @listenToOnce(App, 'jobs:list', ->
        Activator.controller.listJobs()
      )

  # ~~~~~~~~~~~~~

  Activator.addInitializer ->
    Activator.controller = new Activator.Controller()
    new Activator.Router(
      controller: Activator.controller
    )

  Activator.addFinalizer ->
    @stopListening()
    Activator.controller.close()
    delete Activator.controller