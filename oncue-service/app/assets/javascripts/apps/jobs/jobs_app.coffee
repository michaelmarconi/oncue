App.module 'Jobs', (Jobs, App, Backbone, Marionette, $, _) ->

  class Jobs.Router extends Marionette.AppRouter
    appRoutes:
      'jobs'      : 'listJobs'
      'jobs/:id'  : 'showJob'

  API =
    listJobs: ->
      if not Jobs.List.controller
        Jobs.List.controller = new Jobs.List.Controller()
      Jobs.List.controller.listJobs()
      App.Navbar.List.Controller.setActiveNavbarItem('jobs')

    showJob: (id) ->
      Jobs.Show.Controller.showJob(id)

  #------------------------------------------

  App.on('jobs:list', ->
    App.navigate('jobs')
    API.listJobs()
  )

  App.on('job:show', (id) ->
    App.navigate("jobs/#{id}")
    API.showJob(id)
  )

  Jobs.addInitializer ->
    new Jobs.Router(controller: API)