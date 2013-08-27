App.module "Agents.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new App.Common.Views.LoadingView(message: 'Loading agents...')
      App.contentRegion.show(loadingView)

    _showErrorView: ->
      errorView = new App.Common.Views.ErrorView(message: 'Failed to load registered agents')
      App.contentRegion.show(errorView)

    listAgents: ->
      @_showLoadingView()
      fetchingAgents = App.request('agent:entities')
      $.when(fetchingAgents).done( (agents) =>
        agentsView = new List.AgentsView(
          collection: agents
        )
        App.contentRegion.show(agentsView)
      )
      $.when(fetchingAgents).fail( =>
        @_showErrorView()
      )

  List.addInitializer ->
    List.controller = new List.Controller()

  List.addFinalizer ->
    List.controller.close()
    delete List.controller