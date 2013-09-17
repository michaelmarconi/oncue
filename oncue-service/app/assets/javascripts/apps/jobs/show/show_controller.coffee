OnCue.module "Jobs.Show", (Show, OnCue, Backbone, Marionette, $, _) ->

  class Show.Controller extends Marionette.Controller

    _showLoadingView: (jobId) ->
      loadingView = new OnCue.Common.Views.LoadingView(message: "Loading job #{jobId}...")
      OnCue.contentRegion.show(loadingView)

    _showErrorView: (jobId) ->
      errorView = new OnCue.Common.Views.ErrorView(message: "Failed to load job #{jobId}")
      OnCue.contentRegion.show(errorView)

    _updateToolbarButtons: (job) ->
      state = job.get('state')
      if state is 'queued'
        @rerunJobButton.set('enabled', false)
        @deleteJobButton.set('enabled', true)
      else if state is 'running'
        @rerunJobButton.set('enabled', false)
        @deleteJobButton.set('enabled', false)
      else if state is 'complete' or state is 'failed'
        @rerunJobButton.set('enabled', true)
        @deleteJobButton.set('enabled', true)
      else
        @rerunJobButton.set('enabled', false)
        @deleteJobButton.set('enabled', false)

    _buildToolbar: (job) ->
      state = job.get('state')
      listJobsButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'All jobs'
        tooltip: 'Return to the list of jobs'
        iconClass: 'icon-arrow-left muted'
        event: 'list:jobs'
      )
      @rerunJobButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'Re-run'
        tooltip: 'Re-run this job'
        iconClass: 'icon-repeat'
        event: 'rerun:job'
        enabled: false
      )
      @deleteJobButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'Delete'
        tooltip: 'Delete this job permanently'
        iconClass: 'icon-trash'
        enabled: false
        event: 'delete:job'
      )
      actionButtons = new OnCue.Components.Toolbar.ButtonStripModel(
        cssClasses: 'pull-right'
        buttons: new OnCue.Components.Toolbar.ButtonCollection([
          @rerunJobButton, @deleteJobButton
        ])
      )
      toolbarItems = new OnCue.Components.Toolbar.ItemsCollection()
      toolbarItems.add(listJobsButton)
      toolbarItems.add(actionButtons)
      return new OnCue.Components.Toolbar.Controller(
        collection: toolbarItems
      )

    _rerunJob: (job, layout) ->
      # Saving an existing job will cause an HTTP UPDATE
      savingJob = OnCue.request('job:entity:save', job)
      $.when(savingJob).done( (job) =>
        @_updateToolbarButtons(job)
      )
      $.when(savingJob).fail( (job, xhr) ->
        errorView = new OnCue.Common.Views.ErrorView(
          message: "Failed to re-run job (#{xhr.statusText})"
        )
        layout.errorRegion.show(errorView)
      )

    _deleteJob: (job, layout) ->
      deletingJob = OnCue.request('job:entity:delete', job)
      $.when(deletingJob).done( (job) =>
        OnCue.trigger('jobs:list')
      )
      $.when(deletingJob).fail( (job, xhr) ->
        errorView = new OnCue.Common.Views.ErrorView(
          message: "Failed to delete job (#{xhr.statusText})"
        )
        layout.errorRegion.show(errorView)
      )

    #
    # Show an individual job
    #
    showJob: (id) ->

      # Show a loading view
      @_showLoadingView(id)

      # Fetch the job from the server
      fetchingJob = OnCue.request('job:entity', id)
      $.when(fetchingJob).done( (@job) =>

        # Remove all previous event handlers on this controller
        @stopListening()

        # Create the layout
        layout = new Show.Layout(model: @job)

        # Create the toolbar and set initial button state
        toolbarController = @_buildToolbar(@job)
        toolbarView = toolbarController.getView()
        @_updateToolbarButtons(@job)

        # Create the job details and params views
        detailsView = new Show.JobDetailsView(model: @job)
        if @job.get('params') and @job.get('params').length > 0
          paramsView = new Show.JobParamsView(
            collection: @job.get('params')
          )

        # Listen to toolbar events
        @listenTo(toolbarView, 'toolbar:list:jobs', ->
          OnCue.trigger('jobs:list')
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:rerun:job', ->
          @_rerunJob(@job, layout)
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:delete:job', ->
          @_deleteJob(@job, layout)
        )

        # Layout components when the layout is displayed
        @listenTo(layout, 'show', ->
          layout.toolbarRegion.show(toolbarView)
          layout.detailsRegion.show(detailsView)
          if @job.get('params') and @job.get('params').length > 0
            layout.paramsRegion.show(paramsView)
        )

        # Display the layout
        OnCue.contentRegion.show(layout)
      )
      $.when(fetchingJob).fail( =>
        @_showErrorView(id)
      )

    #
    # Update the display of an existing job
    #
    updateJob: (data) =>
      if not @job then return
      if data.id == @job.id
        @job.set(data)
      @_updateToolbarButtons(@job)

  # ~~~~~~~~~~~~~

  Show.addInitializer ->
    Show.controller = new Show.Controller()

  Show.addFinalizer ->
    Show.controller.close()
    delete Show.controller