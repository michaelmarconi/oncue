OnCue.module 'Activator', (Activator, OnCue, Backbone, Marionette, $, _) ->

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
      OnCue.module('Agents').stop()
      OnCue.module('Jobs').start()
      OnCue.trigger('jobs:list')
      @listenToOnce(OnCue, 'agents:list', ->
        Activator.controller.listAgents()
      )

    showJob: (id) ->
      OnCue.module('Agents').stop()
      OnCue.module('Jobs').start()
      OnCue.trigger('job:show', id)
      @listenToOnce(OnCue, 'agents:list', ->
        Activator.controller.listAgents()
      )

    listAgents: ->
      OnCue.module('Jobs').stop()
      OnCue.module('Agents').start()
      OnCue.trigger('agents:list')
      @listenToOnce(OnCue, 'jobs:list', ->
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