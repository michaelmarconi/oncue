OnCue.module "Agents.List", (List, OnCue, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new OnCue.Common.Views.LoadingView(message: 'Loading agents...')
      OnCue.contentRegion.show(loadingView)

    _showErrorView: ->
      errorView = new OnCue.Common.Views.ErrorView(message: 'Failed to load registered agents')
      OnCue.contentRegion.show(errorView)

    listAgents: ->
      @_showLoadingView()
      fetchingAgents = OnCue.request('agent:entities')
      $.when(fetchingAgents).done( (agents) =>
        agentsView = new List.AgentsView(
          collection: agents
        )
        OnCue.contentRegion.show(agentsView)
      )
      $.when(fetchingAgents).fail( =>
        @_showErrorView()
      )

  List.addInitializer ->
    List.controller = new List.Controller()

  List.addFinalizer ->
    List.controller.close()
    delete List.controller