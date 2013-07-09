App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  Show.Controller =
    showJob: (id) ->
      loadingView = new App.Common.Views.LoadingView(
        message: "Loading job #{id}..."
      )
      App.contentRegion.show(loadingView)

      fetching_job = App.request('job:entity', id)
      $.when(fetching_job).done( (job) ->
        jobView = undefined
        if job isnt undefined
          jobView = new Show.JobView(model: job)
        else
          jobView = new Show.MissingJobView()
        App.contentRegion.show(jobView)
      )