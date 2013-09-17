window.OnCue = new Marionette.Application()

OnCue.navigate = (route, options) ->
  options or (options = {})
  Backbone.history.navigate(route, options)

OnCue.getCurrentRoute = ->
  Backbone.history.fragment

OnCue.addRegions(
  navbarRegion: '#navbar'
  contentRegion: '#content'
)

OnCue.on('initialize:after', ->
  if Backbone.history
    Backbone.history.start({pushState: true})
)