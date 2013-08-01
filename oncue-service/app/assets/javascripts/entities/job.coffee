App.module 'Entities.Job', (Job, App, Backbone, Marionette, $, _) ->

  #
  # A job
  #
  class Job.Model extends Backbone.Model
    urlRoot: '/api/jobs'

  #
  # A collection of jobs
  #
  class Job.Collection extends App.Entities.Common.FilteredCollection
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

  class Job.Controller extends Marionette.Controller

    # Create a collection of params from a hash
    _extractParams: (job) ->
      params = job.get('params')
      if params
        jobParams = new Job.ParamsCollection()
        for paramKey in _.keys(params)
          jobParam = new Job.Param(
            key: paramKey
            value: params[paramKey]
          )
          jobParams.add(jobParam)
        job.unset('params')
        job.set('params', jobParams)
        return job

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
        success: (model) =>
          @_extractParams(model)
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

