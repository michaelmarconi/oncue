OnCue.module "Jobs.Show", (Show, OnCue, Backbone, Marionette, $, _) ->

  #
  # Template helpers to simplify presentation logic, designed to be mixed in
  #
  #     showWorker:  Remove camel case and Java package from worker type
  # showEnqueuedAt:  Present human-readable job enqueue time
  #
  class TemplateHelpers
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

  #
  # This is the top-level layout for the jobs show page
  #
  class Show.Layout extends Marionette.Layout
    id: 'jobs_show'
    template: '#jobs_show_layout'
    regions:
      toolbarRegion: '#toolbar_region'
      errorRegion: '#error_region'
      detailsRegion: '#details_region'
      paramsRegion: '#params_region'

    initialize: ->
      _.extend(this, new TemplateHelpers())

  class Show.JobDetailsView extends Marionette.ItemView
    template: '#job_details_view'
    modelEvents:
      'change' : 'render'

    initialize: ->
      _.extend(this, new TemplateHelpers())

  class Show.JobParamView extends Marionette.ItemView
    template: '#job_param_view'

  class Show.JobParamsView extends Marionette.CompositeView
    template: '#job_params_view'
    itemViewContainer: 'table tbody'
    itemView: Show.JobParamView

  #
  # Display a notice when a lookup of a job ID fails
  #
  class Show.MissingJobView extends Marionette.ItemView
    template: '#missing_job_view'