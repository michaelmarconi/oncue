App.module 'Entities.Agent', (Agent, App, Backbone, Marionette, $, _) ->

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
      defer = $.Deferred()
      Agent.collection.fetch(
        success: (collection) ->
          defer.resolve(collection)
        error: ->
          defer.reject()
      )
      return defer.promise()


  Agent.addInitializer ->
    Agent.controller = new Agent.Controller()

    Agent.collection = new Agent.Collection()

    # Listen for agents being started and update the collection
    App.vent.on('agent:started', (agentData) ->
      console.log "Agent started!"
      agent = new Agent.Model(agentData)
      Agent.collection.add(agent)
    )

    # Listen for agents being stopped and update the collection
    App.vent.on('agent:stopped', (agentData) ->
      agent = new Agent.Model(agentData)
      Agent.collection.remove(agent)
    )

    # When the websocket connects, update the collection
    App.vent.on('websocket:connected', ->
      console.log "Fetching agents!"
      Agent.collection.fetch()
    )

    App.reqres.setHandler('agent:entities', ->
      Agent.controller.getAgentEntities()
    )