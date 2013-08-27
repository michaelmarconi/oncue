OnCue.module "Components.Grid", (Grid, OnCue, Backbone, Marionette, $, _) ->

  #
  # A model that describes a data grid
  #
  #      items:  A Backbone collection of grid items (mandatory)
  #  emptyView:  A view to display when the collection of items is empty (optional)
  #  paginated:  Determines whether to paginate the grid (boolean; mandatory)
  #
  #   backgrid:  A top-level container for Backgrid options (see:  http://backgridjs.com)
  #
  class Grid.Model extends Backbone.Model
    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.items then return 'An items attribute is required'
      if not attrs.paginated then return 'An paginated attribute is required'
      if not attrs.backgrid.columns then return 'A Backgrid columns attribute is required'