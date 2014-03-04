OnCue.module 'Entities.TimedJob', (TimedJob, OnCue, Backbone, Marionette, $, _) ->

  #
  # A timed job
  #
  class TimedJob.Model extends Backbone.Model
    urlRoot: '/api/timed_job'

  #
  # A collection of timed jobs
  #
  class TimedJob.Collection extends Backbone.Collection
    url: '/api/timed_jobs'
    model: TimedJob.Model

  # ~~~~~~~~~~~

  class TimedJob.Controller extends Marionette.Controller

    getTimedJobEntities: ->
      if TimedJob.collection
        return TimedJob.collection
      else
        TimedJob.collection = new TimedJob.Collection()
        defer = $.Deferred()
        TimedJob.collection.fetch(
          success: (collection) ->
            defer.resolve(collection)
          error: ->
            defer.reject()
        )
        return defer.promise()

  # ~~~~~~~~~~~

  TimedJob.addInitializer ->
    TimedJob.controller = new TimedJob.Controller()

    OnCue.reqres.setHandler('timed_job:entities', ->
      TimedJob.controller.getTimedJobEntities()
    )