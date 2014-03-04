OnCue.module 'TimedJobs', (TimedJobs, OnCue, Backbone, Marionette, $, _) ->

  # Don't start this module automatically
  @startWithParent = false

  class TimedJobs.Router extends Marionette.AppRouter
    appRoutes:
      'agents' : 'listTimedJobs'

  class TimedJobs.Controller extends Marionette.Controller

    listTimedJobs: ->
      TimedJobs.List.controller.listTimedJobs()
      OnCue.Navbar.List.controller.setActiveNavbarItem('schedule')

  # ~~~~~~~~~~~~

  TimedJobs.addInitializer ->
    TimedJobs.controller = new TimedJobs.Controller()
    new TimedJobs.Router(
      controller: TimedJobs.controller
    )
    @listenTo(OnCue, 'timed_jobs:list', ->
      OnCue.navigate('schedule')
      TimedJobs.controller.listTimedJobs()
    )

  TimedJobs.addFinalizer ->
    @stopListening()
    TimedJobs.controller.close()
    delete TimedJobs.controller