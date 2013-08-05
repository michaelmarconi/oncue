window.App = new Marionette.Application()

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