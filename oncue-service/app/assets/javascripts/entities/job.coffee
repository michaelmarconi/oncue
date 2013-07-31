App.module 'Entities.Job', (Job, App, Backbone, Marionette, $, _) ->

  class Job.Model extends Backbone.Model
    urlRoot: '/api/jobs'

  class Job.Collection extends App.Entities.Common.FilteredCollection
    url: '/api/jobs'
    model: Job.Model

  class Job.Controller extends Marionette.Controller

    getJobEntities: ->
      jobs = new Job.Collection()
      defer = $.Deferred()
      jobs.fetch(
        success: (model) ->
          defer.resolve(model)
        error: ->
          defer.resolve(undefined)
      )
      return defer.promise()

    getJobEntity: (id) ->
      job = new Job.Model(id: id)
      defer = $.Deferred()
      job.fetch(
        success: (model) ->
          defer.resolve(model)
        error: ->
          defer.resolve(undefined)
      )
      return defer.promise()

    saveJobEntity: (job) ->
      defer = $.Deferred()
      job.save(null,
        success: (model) ->
          defer.resolve(model)
        error: (model, xhr) ->
          defer.reject(model, xhr)
      )
      return defer.promise()


  Job.addInitializer ->

    Job.controller = new Job.Controller()

    App.reqres.setHandler('job:entities', ->
      Job.controller.getJobEntities()
    )
    App.reqres.setHandler('job:entity', (id) ->
      Job.controller.getJobEntity(id)
    )
    App.reqres.setHandler('job:entity:save', (job) ->
      Job.controller.saveJobEntity(job)
    )

