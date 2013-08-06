App.module 'Entities.Websocket', (Websocket, App, Backbone, Marionette, $, _) ->

  #
  # The state of the web socket connection
  #
  class Websocket.Connection extends Backbone.Model
    defaults:
      state: 'connecting'

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError
      @_connect()

    validate: (attrs, options) ->
      if not _.contains(['connecting', 'connected'], attrs.state) then return 'Connection state is invalid'

    _connect: =>
      @set('state', 'connecting')
      if @websocket
        delete @websocket.onopen
        delete @websocket.onclose
        delete @websocket.onerror
        delete @websocket.onmessage
      @websocket = new WebSocket("ws://#{window.location.hostname}:#{window.location.port}/websocket")
      @websocket.onopen = @onOpen
      @websocket.onclose = @onClose
      @websocket.onerror = @onError
      @websocket.onmessage = @onMessage

    onOpen: =>
      @set('state', 'connected')

    onClose: =>
      @set('state', 'connecting')
      _.delay(@_connect, 1000)

    onError: (error) =>
      @set('state', 'connecting')
      _.delay(@_connect, 1000)

    onMessage: (message) =>
      messageData = JSON.parse(message.data)
      eventKey = _.keys(messageData)[0]
      subject = _.first(eventKey.split(':'))
      event = _.last(eventKey.split(':'))
      payload = messageData[eventKey][subject]
#      console.log "Subject: #{subject} , Event: #{event}, Payload: #{payload}" #TODO whack this
      App.vent.trigger("#{subject}:#{event}", payload)

  # ~~~~~~~~~~~

  class Websocket.Controller extends Marionette.Controller
    getConnection: ->
      return Websocket.connection

  Websocket.addInitializer ->
    Websocket.controller = new Websocket.Controller()

    Websocket.connection = new Websocket.Connection()
    App.reqres.setHandler('websocket:connection', ->
      Websocket.controller.getConnection()
    )