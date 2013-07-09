App.module "Common.Views", (Views, App, Backbone, Marionette, $, _) ->

  #
  # Display a loading view, with an animated progress bar
  #
  #  - message:  An optional custom message to display
  #
  class Views.LoadingView extends Backbone.Marionette.ItemView
    id: 'loading'
    template: '#loading_view'

    serializeData: ->
      message: @options.message || 'Loading...'

    onBeforeRender: ->
      @$el.hide()

    onRender: ->
      _.delay(@_reveal, 250)

    _reveal: =>
      @$el.fadeIn('fast')

  #
  # Display an error
  #
  #  - message:  The error message to display
  #
  class Views.ErrorView extends Backbone.Marionette.ItemView
    id: 'error'
    template: '#error_view'
    className: 'alert alert-error fade'

    serializeData: ->
      message: @options.message || ''

    onShow: ->
      _.delay(@_reveal, 10)

    _reveal: =>
      @$el.addClass('in')