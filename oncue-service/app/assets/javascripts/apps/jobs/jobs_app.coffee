App.module 'Jobs', (Jobs, App, Backbone, Marionette, $, _) ->

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
      App.Navbar.List.controller.setActiveNavbarItem('jobs')

    showJob: (id) ->
      Jobs.List.stop()
      Jobs.Show.start()
      Jobs.Show.controller.showJob(id)
      App.Navbar.List.controller.setActiveNavbarItem('jobs')

    updateJob: (data) ->
      if Jobs.Show.controller
        Jobs.Show.controller.updateJob(data)

  # ~~~~~~~~~~~~~

  Jobs.addInitializer ->
    Jobs.controller = new Jobs.Controller()
    new Jobs.Router(
      controller: Jobs.controller
    )
    @listenTo(App, 'jobs:list', ->
      App.navigate('jobs')
      Jobs.controller.listJobs()
    )
    @listenTo(App, 'job:show', (id) ->
      App.navigate("jobs/#{id}")
      Jobs.controller.showJob(id)
    )
    @listenTo(App.vent, 'job:progressed', (data) ->
      Jobs.controller.updateJob(data)
    )

  Jobs.addFinalizer ->
    @stopListening()
    Jobs.controller.close()
    delete Jobs.controller