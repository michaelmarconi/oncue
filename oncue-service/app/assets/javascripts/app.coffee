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
  if subject == 'agent' and event == 'started'
    DS.defaultStore.load(App.Agent, payload)
  else if subject == 'agent' and event == 'stopped'
    DS.defaultStore.deleteRecord(App.Agent.find(payload.id))
  else if subject == 'job'
    DS.defaultStore.load(App.Job, payload)
  else
    console.log "Unrecognised event: #{subject}, #{event}"
    console.log payload


# ----------------------------
# -------- ember.js ----------
# ----------------------------

# Start ember app
window.App = Ember.Application.create()

# Set up ember data
App.Store = DS.Store.extend
  revision: 12

# Customise the REST adapter to use the API endpoint
DS.RESTAdapter.reopen
  namespace: 'api'

DS.RESTAdapter.registerTransform('params',
  deserialize: (value) ->
    params = []
    for key, param of value
      params.push(App.Param.createRecord(key: key, value: param))
    return params
)

#
# ROUTES
# -----------------------------

App.Router.reopen
  location: 'history'
  rootURL: '/'

App.Router.map ->
  @resource 'jobs'
  @resource 'job', { path: '/jobs/:job_id' }
  @resource 'agents'

App.IndexRoute = Ember.Route.extend
  redirect: ->
    @transitionTo 'jobs'

App.AgentsRoute = Ember.Route.extend
  model: ->
    return App.Agent.find()

App.JobsRoute = Ember.Route.extend
  model: ->
    return App.Job.find()


#
# MODELS
# ----------------------------

# A navigation item on the navbar
App.NavItem = DS.Model.extend
  name: DS.attr('string')
  count: DS.attr('number')
  active: DS.attr('boolean')
  link: DS.attr('string')

App.Agent = DS.Model.extend
  url: DS.attr('string')

App.Job = DS.Model.extend(
  enqueuedAt: DS.attr('date')
  workerType: DS.attr('string')
  params: DS.attr('params')
  progress: DS.attr('number')
  state: DS.attr('string')
  errorMessage: DS.attr('string')

  order: (->
    parseInt(@get('id'))
  ).property('id')

  isQueued: ( ->
    @get('state') == 'queued'
  ).property('state')

  isRunning: ( ->
    @get('state') == 'running'
  ).property('state')

  isComplete: ( ->
    @get('state') == 'complete'
  ).property('state')

  isFailed: ( ->
    @get('state') == 'failed'
  ).property('state')
)

App.Param = DS.Model.extend(
  key: DS.attr('string')
  value: DS.attr('string')
)

#
# VIEWS
# ----------------------------

App.ApplicationView = Ember.View.extend
  didInsertElement: ->
    $('a.brand').tooltip()

App.JobsView = Ember.View.extend
  templateName: 'jobs'
  click: (event) ->
    if event.target.id == 'run-first-job'
      @get('controller').send('runTestJob')

App.JobToolsView = Ember.View.extend
  templateName: '_job-tools'
  click: (event) ->
    if event.target.id == 'run-test-job'
      @get('controller').send('runTestJob')
    else if event.target.id == 'clear-old-jobs'
      @get('controller').send('clearOldJobs')


# TODO Prevent check-bx drop-downs from closing too early
#$('li').click(function(event){
#event.stopPropagation();
#});


#
# VIEW HELPERS
# ----------------------------

# Display a progress bar for a specific work percentage
Ember.Handlebars.registerBoundHelper 'showProgress', (value, options) ->
  progress = value
  new Handlebars.SafeString(
    "<div class='progress progress-striped active'>" +
      "<div class='bar' style='width: #{progress * 100}%;'></div>" +
    "</div>"
  )

# Display 'time ago' in words
Ember.Handlebars.registerBoundHelper 'showLocalDateTime', (value, options) ->
  moment(value).format('LLL');

# Display a grid of parameters
Ember.Handlebars.registerBoundHelper 'showParams', (value, options) ->
  params = value
  if params? and params.toArray().length > 0
    paramString = ""
    params.forEach (item, index) ->
      paramString = paramString + "<div><span class='key'>#{item.get('key')}</span> &rarr; #{item.get('value')}</div>"
    new Handlebars.SafeString(paramString)
  else
    new Handlebars.SafeString("&ndash;")


#
# CONTROLLERS
# ----------------------------

App.AgentsController = Ember.ArrayController.extend()

App.JobsController = Ember.ArrayController.extend(
  sortProperties: ['order']
  sortAscending: false

  runTestJob: ->
    # Jobs API doesn't follow ember data conventions
    # hence the custom AJAX call.
    jQuery.ajax
      url: '/api/jobs'
      type: 'POST'
      data: JSON.stringify(
        {
          worker_type: 'oncue.worker.TestWorker'
          params : {
            key1 : "Value 1"
            key2 : "Value 2"
          }
        }
      )
      contentType: 'application/json'
      success: ->
        $('#job-tools-error .alert').fadeOut('fast')
      error: ->
        $('#job-tools-error .alert').fadeIn('fast').delay(3000).fadeOut('fast')

  clearOldJobs: ->
    console.log "Clearing old jobs..."
)
