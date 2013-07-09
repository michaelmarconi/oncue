App.module "Jobs.List", (List, App, Backbone, Marionette, $, _) ->

  # This is the top-level layout for the jobs list page
  class List.Layout extends Marionette.Layout
    template: '#jobs_list_layout'
    regions:
      toolbarRegion: '#toolbar_region'
      errorRegion: '#error_region'
      jobsRegion: '#jobs_region'

  # This is the layout for Backgrid, including a paginator
  class List.GridLayout extends Marionette.Layout
    template: '#jobs_grid_layout'
    regions:
      gridRegion: '#grid_region'
      paginatorRegion: '#paginator_region'

  class List.NoJobsView extends Marionette.ItemView
    template: '#no_jobs_view'
    className: 'alert alert-info alert-block'
    triggers:
      'click a' : 'run:test:job'

  # A custom grid row that flashes when a new item
  # is added
  class List.FlashingRow extends Backgrid.Row
    cssClass: 'info'
    render: ->
      super
      if @model.get('is_new')
        @$el.hide().toggleClass(@cssClass).fadeIn 800, =>
          setTimeout ( =>
            @$el.toggleClass(@cssClass)
            @model.unset('is_new')
          ), 500
      return this

  class List.JobGridView extends Backgrid.Grid
    className: 'table table-hover'
    events:
      'click td a.js-show': 'showJobItem'

    initialize: (options) ->
      options['row'] = List.FlashingRow
      options['columns'] = [
        name: 'id'
        label: '#'
        editable: false
        cell: List.JobIDCell
      ,
        name: 'worker_type'
        label: 'Worker'
        editable: false
        cell: Backgrid.StringCell.extend(className: 'monospace')
      ,
        name: 'enqueued_at'
        label: 'Enqueued'
        editable: false
        cell: Backgrid.Extension.MomentCell.extend(
          displayFormat: 'MMMM Do YYYY, h:mm:ss a'
        )
      ,
        name: 'state'
        label: 'State'
        editable: false
        cell: Backgrid.StringCell.extend(className: 'capitalised')
      ,
        name: 'progress'
        label: 'Progress'
        editable: false
        cell: List.ProgressCell
      ]
      super(options)

    showJobItem: (event) ->
      event.preventDefault()
      event.stopPropagation()
      @trigger('job:show', event.target.id)

  class List.JobGridPaginatorView extends Backgrid.Extension.Paginator
    className: 'pagination pagination-centered'

  class List.JobIDCell extends Backgrid.Cell
    template: _.template($('#job_list_id').html())
    render: ->
      @$el.html(@template(@model.attributes))
      return this

  class List.ProgressCell extends Backgrid.Cell
    template: _.template($('#job_list_progress').html())
    render: ->
      @$el.html(@template(@model.attributes))
      progress = @$el.find('.progress')
      switch @model.get('state')
        when 'queued' then break
        when 'running' then progress.addClass('progress-striped active')
        when 'complete' then progress.addClass('progress-success')
        when 'failed' then progress.addClass('progress-danger')
      return this

  List.addInitializer( ->
#    Cocktail.mixin(List.JobGridView, Backgrid.Grid)
  )