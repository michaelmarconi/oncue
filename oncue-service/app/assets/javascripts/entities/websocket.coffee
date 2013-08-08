App.module 'Entities.Websocket', (Websocket, App, Backbone, Marionette, $, _) ->

  #
  # The state of the web socket connection
  #
  class Websocket.Connection extends Backbone.Model
    defaults:
      state: 'connecting'
      attempts: 0  # Number of connection attempts

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError
      @_connect()

    validate: (attrs, options) ->
      if not _.contains(['connecting', 'connected'], attrs.state) then return 'Connection state is invalid'

    _connect: =>
      @set('state', 'connecting')
      App.vent.trigger('websocket:connecting')
      if @websocket then delete @websocket
      @websocket = new WebSocket("ws://#{window.location.hostname}:#{window.location.port}/websocket")
      @websocket.onopen = @onOpen
      @websocket.onclose = @onClose
      @websocket.onerror = @onError
      @websocket.onmessage = @onMessage

    onOpen: =>
      @set('state', 'connected')
      @set('attempts', @get('attempts') + 1 )
      App.vent.trigger('websocket:connected')
      if @get('attempts') > 1
        App.vent.trigger('websocket:reconnected')

    onClose: =>
      @set('state', 'connecting')
      App.vent.trigger('websocket:closed')
      _.delay(@_connect, 1000)

    onError: (error) =>
      @set('state', 'connecting')
      App.vent.trigger('websocket:error', error)

    onMessage: (message) =>
      messageData = JSON.parse(message.data)
      eventKey = _.keys(messageData)[0]
      subject = _.first(eventKey.split(':'))
      event = _.last(eventKey.split(':'))
      payload = messageData[eventKey][subject]
      console.log "Subject: #{subject} , Event: #{event}, Payload: #{payload}" #TODO whack this
      App.vent.trigger("#{subject}:#{event}", payload)

  # ~~~~~~~~~~~

  class Websocket.Controller extends Marionette.Controller
    getConnection: ->
      if not Websocket.connection
        Websocket.connection = new Websocket.Connection()
      return Websocket.connection

  Websocket.addInitializer ->
    Websocket.controller = new Websocket.Controller()

    App.reqres.setHandler('websocket:connection', ->
      Websocket.controller.getConnection()
    )