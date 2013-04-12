# ----------------------------
# ------ web sockets ---------
# ----------------------------

websocket = new WebSocket("ws://localhost:9000/websocket")
websocket.onopen = -> console.log "Web socket connected."
websocket.onerror = -> console.log "Failed to create web socket."
websocket.onmessage = (message) ->
  messageData = JSON.parse(message.data)
  eventKey = _.keys(messageData)[0]
  subject = _.first(eventKey.split(':'))
  event = _.last(eventKey.split(':'))
  payload = messageData[eventKey][subject]
  handleEvent(subject, event, payload)

# ----------------------------
# ----- event handler --------
# ----------------------------

handleEvent = (subject, event, payload) ->
  if subject== 'agent' and event == 'started'
    DS.defaultStore.load(App.Agent, payload)
  else if subject== 'agent' and event == 'stopped'
    DS.defaultStore.deleteRecord(App.Agent.find(payload.id))

# ----------------------------
# -------- ember.js ----------
# ----------------------------

# Start ember app
window.App = Ember.Application.create()

# Set up ember data
App.Store = DS.Store.extend (
  revision: 12,
#  adapter: 'DS.FixtureAdapter'
)

# Index route used to set up nested controllers & views
App.IndexRoute = Ember.Route.extend(
  setupController: ->
    this.controllerFor('agents').set('model', App.Agent.find())
  renderTemplate: ->
    this.render('agents')
)

#
# Models
# ----------------------------

App.Agent = DS.Model.extend(
  url: DS.attr('string')
)

#
# Controllers
# ----------------------------
App.AgentsController = Ember.ArrayController.extend(
  agentsRegistered: ->
    console.log this.length
    this.size() > 0
)

#
# Fixtures
# ----------------------------

#App.Agent.FIXTURES = [
#  {
#  id: 1,
#  type: 'UnlimitedCapacity'
#  url: 'akka://localhost/1'
#  },
#  {
#  id: 2,
#  type: 'UnlimitedCapacity'
#  url: 'akka://localhost/2'
#  }
#];