OnCue.module 'Activator', (Activator, OnCue, Backbone, Marionette, $, _) ->

  #--------------------------------------------------
  # This module is responsible for activating and
  # deactivating apps
  #--------------------------------------------------

  class Activator.Router extends Marionette.AppRouter
    appRoutes:
      'jobs'     : 'listJobs'
      'jobs/:id' : 'showJob'
      'agents'   : 'listAgents'
      'schedule' : 'listTimedJobs'
      ''         : 'listJobs'

  class Activator.Controller extends Marionette.Controller

    listJobs: ->
      OnCue.module('Agents').stop()
      OnCue.module('Schedule').stop()
      OnCue.module('Jobs').start()
      OnCue.trigger('jobs:list')
      @listenToOnce(OnCue, 'agents:list', ->
        Activator.controller.listAgents()
      )
      @listenToOnce(OnCue, 'timed_jobs:list', ->
        Activator.controller.listTimedJobs()
      )

    showJob: (id) ->
      OnCue.module('Agents').stop()
      OnCue.module('TimedJobs').stop()
      OnCue.module('Jobs').start()
      OnCue.trigger('job:show', id)
      @listenToOnce(OnCue, 'agents:list', ->
        Activator.controller.listAgents()
      )
      @listenToOnce(OnCue, 'timed_jobs:list', ->
        Activator.controller.listTimedJobs()
      )

    listAgents: ->
      OnCue.module('Jobs').stop()
      OnCue.module('TimedJobs').stop()
      OnCue.module('Agents').start()
      OnCue.trigger('agents:list')
      @listenToOnce(OnCue, 'jobs:list', ->
        Activator.controller.listJobs()
      )
      @listenToOnce(OnCue, 'timed_jobs:list', ->
        Activator.controller.listTimedJobs()
      )

    listTimedJobs: ->
      OnCue.module('Jobs').stop()
      OnCue.module('Agents').stop()
      OnCue.module('TimedJobs').start()
      OnCue.trigger('timed_jobs:list')
      @listenToOnce(OnCue, 'jobs:list', ->
        Activator.controller.listJobs()
      )
      @listenToOnce(OnCue, 'agents:list', ->
        Activator.controller.listAgents()
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