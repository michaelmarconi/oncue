OnCue.module "Jobs.List", (List, OnCue, Backbone, Marionette, $, _) ->

  #
  # This is the top-level layout for the jobs list page
  #
  class List.Layout extends Marionette.Layout
    id: 'jobs_list'
    template: '#jobs_list_layout'
    regions:
      toolbarRegion: '#toolbar_region'
      errorRegion: '#error_region'
      jobsRegion: '#jobs_region'

  #
  # The view to display when there are no jobs in the collection
  #
  class List.NoJobsView extends Marionette.ItemView
    template: '#no_jobs_view'
    className: 'alert alert-info alert-block'
    events:
      'click a' : '_runTestJob'

    _runTestJob: ->
      OnCue.vent.trigger('run:test:job')

  #
  # A custom Backgrid row that changes the row
  # background colour depending on job state
  #
  class List.JobStateRow extends Backgrid.Row
    className: =>
      if @model.get('state') == 'complete' then 'muted success'
      else if @model.get('state') == 'failed' then 'muted error'
      else if @model.get('state') == 'running' then 'info'

    render: ->
      super
      if @model.get('is_new')
        @$el.hide().toggleClass('info').fadeIn 500, =>
          setTimeout ( =>
            @$el.toggleClass('info')
            @model.unset('is_new')
          ), 250
      return this

  #
  # A custom Backgrid cell that enables the display of an individual job
  #
  class List.JobIDCell extends Backgrid.Cell
    template: _.template($('#job_list_id').html())
    events:
      'click a': '_showJobItem'

    _showJobItem: (event) ->
      event.preventDefault()
      event.stopPropagation()
      OnCue.trigger('job:show', event.target.id)

    render: ->
      @$el.html(@template(@model.attributes))
      if @model.get('state') == 'complete'
        @$el.find('a').addClass('text-success')
      else if @model.get('state') == 'failed'
        @$el.find('a').addClass('text-error')
      return this

  #
  # A custom Backgrid cell that displays truncated worker types
  #
  class List.WorkerCell extends Backgrid.StringCell
    className: 'bold'
    render: ->
      workerType = @model.get('worker_type')
      lastPackagePos = workerType.lastIndexOf('.')
      if lastPackagePos > -1
        # Strip Java package details
        workerType = workerType.substring(lastPackagePos + 1, workerType.length)
        # Remove camel case
        workerType = workerType.replace(/([A-Z])/g, " $1").replace /^./, (str) -> str.toUpperCase()
      @$el.html(workerType)
      return this

  #
  # A custom Backgrid cell that displays the state of a job,
  # including a label when it has been re-run
  #
  class List.RerunCell extends Backgrid.Cell
    render: ->
      if(@model.get('rerun') is true)
        @$el.append("<span class='label label-info'><i class='icon-repeat'></i>rerun</span>")
      return this

  #
  # A custom Backgrid cell that displays job progress
  #
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