App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  class Show.JobView extends Marionette.ItemView
    id: 'jobs_show'
    template: '#job_view'
    events:
      'click a' : '_listJobs'
    modelEvents:
      'change' : 'render'
    templateHelpers:
      showWorker: ->
        workerType = @worker_type
        lastPackagePos = workerType.lastIndexOf('.')
        if lastPackagePos > -1
          # Strip Java package details
          workerType = workerType.substring(lastPackagePos + 1, workerType.length)
          # Remove camel case
          workerType = workerType.replace(/([A-Z])/g, " $1").replace /^./, (str) -> str.toUpperCase()
        return workerType

      showEnqueuedAt: ->
        return moment(@enqueued_at).format('MMMM Do YYYY, h:mm:ss a')

    _listJobs: (event) ->
      event.preventDefault()
      event.stopPropagation()
      App.trigger('jobs:list')


  class Show.MissingJobView extends Marionette.ItemView
    template: '#missing_job_view'