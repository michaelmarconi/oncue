App.module "Components.Grid", (Grid, App, Backbone, Marionette, $, _) ->

  class Grid.Layout extends Marionette.Layout
    template: '#grid_layout'
    regions:
      gridRegion: '#grid_region'
      paginatorRegion: '#paginator_region'

  class Grid.View extends Backgrid.Grid
    className: 'table table-hover'

  class Grid.PaginatorView extends Backgrid.Extension.Paginator
    className: 'pagination pagination-centered'
