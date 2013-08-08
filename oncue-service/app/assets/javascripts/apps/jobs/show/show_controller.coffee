App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  class Show.Controller extends Marionette.Controller

    _showLoadingView: (jobId) ->
      loadingView = new App.Common.Views.LoadingView(message: "Loading job #{jobId}...")
      App.contentRegion.show(loadingView)

    _showErrorView: (jobId) ->
      errorView = new App.Common.Views.ErrorView(message: "Failed to load job #{jobId}")
      App.contentRegion.show(errorView)

    showJob: (id) ->
      @_showLoadingView(id)

      fetchingJob = App.request('job:entity', id)
      $.when(fetchingJob).done( (job) ->
        detailsView = new Show.JobDetailsView(model: job)
        if job.get('params') and job.get('params').length > 0
          paramsView = new Show.JobParamsView(
            collection: job.get('params')
          )
        layout = new Show.Layout(model: job)
        layout.on('show', ->
          layout.detailsRegion.show(detailsView)
          if job.get('params') and job.get('params').length > 0
            layout.paramsRegion.show(paramsView)
        )

        # TODO This callback needs to be detached when this controller is closed!
        App.vent.on('job:progressed', (jobData) =>
          if jobData.id == job.id
            job.set(jobData)
        )

        App.contentRegion.show(layout)
      )
      $.when(fetchingJob).fail( =>
        @_showErrorView(id)
      )

  Show.addInitializer ->
    Show.controller = new Show.Controller()