OnCue.module "TimedJobs.List", (List, OnCue, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new OnCue.Common.Views.LoadingView(message: 'Loading schedule...')
      OnCue.contentRegion.show(loadingView)

    _showErrorView: ->
      errorView = new OnCue.Common.Views.ErrorView(message: 'Failed to load schedule')
      OnCue.contentRegion.show(errorView)

    listTimedJobs: ->
      @_showLoadingView()
      fetchingTimedJobs = OnCue.request('timed_job:entities')
      $.when(fetchingTimedJobs).done( (timed_jobs) =>
#        TODO implement this
#        agentsView = new List.AgentsView(
#          collection: schedule
#        )
#        OnCue.contentRegion.show(agentsView)
      )
      $.when(fetchingTimedJobs).fail( =>
        @_showErrorView()
      )

  List.addInitializer ->
    List.controller = new List.Controller()

  List.addFinalizer ->
    List.controller.close()
    delete List.controller