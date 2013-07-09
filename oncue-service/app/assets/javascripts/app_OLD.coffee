
# ----------------------
# ---- Application -----
# ----------------------

App = new Backbone.Marionette.Application();

#
# Intialize the Web Socket
#
App.addInitializer( ->
  websocket = new WebSocket("ws://#{window.location.hostname}:9000/websocket")
#  websocket.onopen = -> console.log "Web socket connected."
#  websocket.onerror = ->
#    console.log "Failed to create web socket."
#    App.vent.trigger("websocket:failed")
#  websocket.onmessage = (message) ->
#    messageData = JSON.parse(message.data)
#    eventKey = _.keys(messageData)[0]
#    subject = _.first(eventKey.split(':'))
#    event = _.last(eventKey.split(':'))
#    payload = messageData[eventKey][subject]
#    App.vent.trigger("#{subject}:#{event}", payload)
##    console.log "Subject: #{subject} , Event: #{event}, Payload: #{payload}"
)

App.vent.on("websocket:failed", ->
  console.log "Web socket failed :-("
)

# ------------------
# --- Web Socket ---
# ------------------

App.module("WebSocket", (WebSocket, App, Backbone, Marionette, $, _) ->

  WebSocket.addInitializer( ->
#    websocket = new WebSocket("ws://#{window.location.hostname}:9000/websocket")
#    websocket.onopen = -> console.log "Web socket connected."
#    websocket.onerror = ->
#      console.log "Failed to create web socket."
#      App.vent.trigger("websocket:failed")
#    websocket.onmessage = (message) ->
#      messageData = JSON.parse(message.data)
#      eventKey = _.keys(messageData)[0]
#      subject = _.first(eventKey.split(':'))
#      event = _.last(eventKey.split(':'))
#      payload = messageData[eventKey][subject]
#      App.vent.trigger("#{subject}:#{event}", payload)
    console.log "WTF wheres me sockit?"
  )
)

# ----------------
# --- Entities ---
# ----------------

App.module("Entities", (Entities, App, Backbone, Marionette, $, _) ->

  class Entities.Agent extends Backbone.Model

  class Entities.AgentCollection extends Backbone.Collection
    model: Entities.Agent

  Entities.addInitializer( ->
    agents = new Entities.AgentCollection()

    App.vent.on("agent:started", (agentData) ->
      console.log "Agent started!"
      console.log agentData
      agents.add(new Entities.Agent(agentData))
    )
  )
)

App.start()









