OnCue.module 'Entities.Job', (Job, OnCue, Backbone, Marionette, $, _) ->

  #
  # A job
  #
  class Job.Model extends Backbone.Model
    urlRoot: '/api/jobs'
    params: ->
      params = @get('params')
      paramsCollection = new Job.ParamsCollection()
      for paramKey in _.keys(params)
        jobParam = new Job.Param(
          key: paramKey
          value: params[paramKey]
        )
        paramsCollection.add(jobParam)
      return paramsCollection


  #
  # A collection of jobs
  #
  class Job.Collection extends OnCue.Entities.Common.FilteredCollection
    url: '/api/jobs'
    model: Job.Model

  #
  # A parameter (key/value) for a job
  #
  class Job.Param extends Backbone.Model
    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.key then return 'A key attribute is required'
      if not attrs.value then return 'A value attribute is required'

  #
  # A collection of job params
  #
  class Job.ParamsCollection extends Backbone.Collection

  # ~~~~~~~~~~~

  class Job.Controller extends Marionette.Controller

    getJobEntities: ->
      if Job.collection
        return Job.collection
      else
        Job.collection = new Job.Collection()
        defer = $.Deferred()
        Job.collection.fetch(
          success: (collection) =>
            defer.resolve(collection)
          error: ->
            defer.reject()
        )
        return defer.promise()

    getJobEntity: (id) ->
      if Job.collection and Job.collection.get(id)
        return Job.collection.get(id)
      else
        job = new Job.Model(id: id)
        defer = $.Deferred()
        job.fetch(
          success: (model) =>
            defer.resolve(model)
          error: ->
            defer.reject()
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

    deleteJobEntity: (job) ->
      defer = $.Deferred()
      job.destroy(
        success: (model) ->
          defer.resolve(model)
        error: (model, xhr) ->
          defer.reject(model, xhr)
      )
      return defer.promise()

    addJobEntity: (jobData) ->
      if Job.collection
        jobData.is_new = true
        job = new Job.Model(jobData)
        Job.collection.add(job)

    updateJobEntity: (jobData) ->
      if Job.collection
        job = Job.collection.get(jobData.id)
        if job
          job.set(jobData)
        else
          throw "Cannot update unrecognised job #{jobData.id}"

    updateJobEntities: ->
      if Job.collection
        Job.collection.fetch()


  # ~~~~~~~~~~~

  Job.addInitializer ->

    Job.controller = new Job.Controller()

    OnCue.reqres.setHandler('job:entities', ->
      Job.controller.getJobEntities()
    )
    OnCue.reqres.setHandler('job:entity', (id) ->
      Job.controller.getJobEntity(id)
    )
    OnCue.reqres.setHandler('job:entity:save', (job) ->
      Job.controller.saveJobEntity(job)
    )
    OnCue.reqres.setHandler('job:entity:delete', (job) ->
      Job.controller.deleteJobEntity(job)
    )

    # If the websocket reconnects, update the collection
    OnCue.vent.on('websocket:reconnected', ->
      if Job.collection
        Job.collection.fetch()
    )

    # Update the collection if a job is enqueued
    OnCue.vent.on('job:enqueued', (jobData) ->
      Job.controller.addJobEntity(jobData)
    )

    # Update the collection if a job progresses
    OnCue.vent.on('job:progressed', (jobData) ->
      Job.controller.updateJobEntity(jobData)
    )

    # Update the collection if a job fails
    OnCue.vent.on('job:failed', (jobData) ->
      Job.controller.updateJobEntity(jobData)
    )

    # Update the collection if the jobs have been
    # cleaned up by the maintenance worker
    OnCue.vent.on('jobs:cleanup', ->
      Job.controller.updateJobEntities()
    )

