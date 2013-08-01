window.App = new Marionette.Application()

#--------------------------
#--- Web Socket adapter ---
#--------------------------

connectWebsocket = ->

  App.vent.trigger('websocket:connecting')
  websocket = new WebSocket("ws://#{window.location.hostname}:9000/websocket")

  websocket.onopen = ->
    App.vent.trigger('websocket:connected')

  websocket.onclose = ->
    App.vent.trigger('websocket:closed')
    delete websocket.onopen
    delete websocket.onclose
    delete websocket.onerror
    delete websocket.onmessage
    _.delay(connectWebsocket, 1000)

  websocket.onerror = (error) ->
    App.vent.trigger('websocket:error')
    delete websocket.onopen
    delete websocket.onclose
    delete websocket.onerror
    delete websocket.onmessage
    _.delay(connectWebsocket, 1000)

  websocket.onmessage = (message) ->
    messageData = JSON.parse(message.data)
    eventKey = _.keys(messageData)[0]
    subject = _.first(eventKey.split(':'))
    event = _.last(eventKey.split(':'))
    payload = messageData[eventKey][subject]
#    console.log "Subject: #{subject} , Event: #{event}, Payload: #{payload}" TODO whack this
    App.vent.trigger("#{subject}:#{event}", payload)

#--------------------------

App.navigate = (route, options) ->
  options or (options = {})
  Backbone.history.navigate(route, options)

App.getCurrentRoute = ->
  Backbone.history.fragment

App.addRegions(
  navbarRegion: '#navbar'
  contentRegion: '#content'
)

App.on('initialize:after', ->
  if Backbone.history
    Backbone.history.start({pushState: true})

    if @getCurrentRoute() is ""
      App.trigger('jobs:list')
)

App.addInitializer(connectWebsocket)
