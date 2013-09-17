OnCue.module 'Entities.Agent', (Agent, OnCue, Backbone, Marionette, $, _) ->

  #
  # An agent
  #
  class Agent.Model extends Backbone.Model
    urlRoot: '/api/agent'

  #
  # A collection of agents
  #
  class Agent.Collection extends Backbone.Collection
    url: '/api/agents'
    model: Agent.Model

  # ~~~~~~~~~~~

  class Agent.Controller extends Marionette.Controller

    getAgentEntities: ->
      if Agent.collection
        return Agent.collection
      else
        Agent.collection = new Agent.Collection()
        defer = $.Deferred()
        Agent.collection.fetch(
          success: (collection) ->
            defer.resolve(collection)
          error: ->
            defer.reject()
        )
        return defer.promise()

  # ~~~~~~~~~~~

  Agent.addInitializer ->
    Agent.controller = new Agent.Controller()

    # Listen for agents being started and update the collection
    OnCue.vent.on('agent:started', (agentData) ->
      if Agent.collection
        agent = new Agent.Model(agentData)
        Agent.collection.add(agent)
    )

    # Listen for agents being stopped and update the collection
    OnCue.vent.on('agent:stopped', (agentData) ->
      if Agent.collection
        agent = new Agent.Model(agentData)
        Agent.collection.remove(agent)
    )

    # If the websocket reconnects, update the collection
    OnCue.vent.on('websocket:reconnected', ->
      if Agent.collection
        Agent.collection.fetch()
    )

    OnCue.reqres.setHandler('agent:entities', ->
      Agent.controller.getAgentEntities()
    )