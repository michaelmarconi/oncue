App.module "Agents.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new App.Common.Views.LoadingView(message: 'Loading agents...')
      App.contentRegion.show(loadingView)

#    _renderAgents: (agents) ->
#      if agents.length == 0
#        App.contentRegion.show(new List.NoAgentsView())
#      else
#        agentsView = new List.AgentsView(
#          collection: agents
#        )
#        App.contentRegion.show(agentsView)

    listAgents: ->
      @_showLoadingView()
      fetchingAgents = App.request('agent:entities')
      $.when(fetchingAgents).done( (agents) =>
        agentsView = new List.AgentsView(
          collection: agents
        )
        App.contentRegion.show(agentsView)
#        @stopListening(agents)
#        @listenTo(agents, 'add remove', (event) =>
#          @_renderAgents(agents)
#        )
#        @_renderAgents(agents)
      )

  List.addInitializer ->
    List.controller = new List.Controller()

