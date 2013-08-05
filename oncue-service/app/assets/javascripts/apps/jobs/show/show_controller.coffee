App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  class Show.Controller extends Marionette.Controller

    showJob: (id) ->
      loadingView = new App.Common.Views.LoadingView(
        message: "Loading job #{id}..."
      )
      App.contentRegion.show(loadingView)

      fetching_job = App.request('job:entity', id)
      $.when(fetching_job).done( (job) ->
        if job isnt undefined
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
        else
          missingJobView = new Show.MissingJobView()

        App.vent.on('job:progressed', (jobData) =>
          if jobData.id == job.id
            job.set(jobData)
        )

        if missingJobView
          App.contentRegion.show(missingJobView)
        else if layout
          App.contentRegion.show(layout)
        else
      )

  Show.addInitializer ->
    Show.controller = new Show.Controller()