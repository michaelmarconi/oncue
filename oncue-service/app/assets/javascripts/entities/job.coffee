App.module 'Entities', (Entities, App, Backbone, Marionette, $, _) ->

  class Entities.Job extends Backbone.Model
    urlRoot: '/api/jobs'

  class Entities.JobCollection extends Backbone.PageableCollection
    url: '/api/jobs'
    model: Entities.Job
    mode: 'client'
    state:
      pageSize: 15
      sortKey: 'id'
      order: 1

  API =
    getJobEntities: ->
      jobs = new Entities.JobCollection()
      defer = $.Deferred()
      jobs.fetch(
        success: (model) ->
          defer.resolve(model)
        error: ->
          defer.resolve(undefined)
      )
      return defer.promise()

    getJobEntity: (id) ->
      job = new Entities.Job(id: id)
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


  App.reqres.setHandler('job:entities', ->
    API.getJobEntities()
  )

  App.reqres.setHandler('job:entity', (id) ->
    API.getJobEntity(id)
  )

  App.reqres.setHandler('job:entity:save', (job) ->
    API.saveJobEntity(job)
  )

  return Entities