App.module 'Jobs', (Jobs, App, Backbone, Marionette, $, _) ->

  class Jobs.Router extends Marionette.AppRouter
    appRoutes:
      'jobs'      : 'listJobs'
      'jobs/:id'  : 'showJob'

  class Jobs.Controller extends Marionette.Controller

    listJobs: ->
      Jobs.List.controller.listJobs()
      App.Navbar.List.controller.setActiveNavbarItem('jobs')

    showJob: (id) ->
      Jobs.Show.controller.showJob(id)


  Jobs.addInitializer ->
    Jobs.controller = new Jobs.Controller()
    new Jobs.Router(
      controller: Jobs.controller
    )
    App.on('jobs:list', ->
      App.navigate('jobs')
      Jobs.controller.listJobs()
    )
    App.on('job:show', (id) ->
      App.navigate("jobs/#{id}")
      Jobs.controller.showJob(id)
    )