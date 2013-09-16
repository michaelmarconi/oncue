OnCue.module 'Jobs', (Jobs, OnCue, Backbone, Marionette, $, _) ->

  # Don't start this module automatically
  @startWithParent = false

  class Jobs.Router extends Marionette.AppRouter
    appRoutes:
      'jobs'      : 'listJobs'
      'jobs/:id'  : 'showJob'

  class Jobs.Controller extends Marionette.Controller

    listJobs: ->
      Jobs.Show.stop()
      Jobs.List.start()
      Jobs.List.controller.listJobs()
      OnCue.Navbar.List.controller.setActiveNavbarItem('jobs')

    showJob: (id) ->
      Jobs.List.stop()
      Jobs.Show.start()
      Jobs.Show.controller.showJob(id)
      OnCue.Navbar.List.controller.setActiveNavbarItem('jobs')

    updateJob: (data) ->
      if Jobs.Show.controller
        Jobs.Show.controller.updateJob(data)

  # ~~~~~~~~~~~~~

  Jobs.addInitializer ->
    Jobs.controller = new Jobs.Controller()
    new Jobs.Router(
      controller: Jobs.controller
    )
    @listenTo(OnCue, 'jobs:list', ->
      OnCue.navigate('jobs')
      Jobs.controller.listJobs()
    )
    @listenTo(OnCue, 'job:show', (id) ->
      OnCue.navigate("jobs/#{id}")
      Jobs.controller.showJob(id)
    )
    @listenTo(OnCue.vent, 'job:progressed', (data) ->
      Jobs.controller.updateJob(data)
    )
    @listenTo(OnCue.vent, 'job:failed', (data) ->
      Jobs.controller.updateJob(data)
    )

  Jobs.addFinalizer ->
    @stopListening()
    Jobs.controller.close()
    delete Jobs.controller