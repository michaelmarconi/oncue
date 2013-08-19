App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  class Show.Controller extends Marionette.Controller

    _showLoadingView: (jobId) ->
      loadingView = new App.Common.Views.LoadingView(message: "Loading job #{jobId}...")
      App.contentRegion.show(loadingView)

    _showErrorView: (jobId) ->
      errorView = new App.Common.Views.ErrorView(message: "Failed to load job #{jobId}")
      App.contentRegion.show(errorView)

    _updateToolbarButtons: (job) ->
      state = job.get('state')
      if state is 'complete' or state is 'failed'
        @rerunJobButton.set('enabled', true)
        @deleteJobButton.set('enabled', true)
      else
        @rerunJobButton.set('enabled', false)
        @deleteJobButton.set('enabled', false)

    _buildToolbar: (job) ->
      state = job.get('state')
      listJobsButton = new App.Components.Toolbar.ButtonModel(
        title: 'All jobs'
        tooltip: 'Return to the list of jobs'
        iconClass: 'icon-arrow-left muted'
        event: 'list:jobs'
      )
      @rerunJobButton = new App.Components.Toolbar.ButtonModel(
        title: 'Re-run'
        tooltip: 'Re-run this job'
        iconClass: 'icon-repeat'
        event: 'rerun:job'
        enabled: false
      )
      @deleteJobButton = new App.Components.Toolbar.ButtonModel(
        title: 'Delete'
        tooltip: 'Delete this job permanently'
        iconClass: 'icon-trash'
        enabled: false
        event: 'delete:job'
      )
      actionButtons = new App.Components.Toolbar.ButtonStripModel(
        cssClasses: 'pull-right'
        buttons: new App.Components.Toolbar.ButtonCollection([
          @rerunJobButton, @deleteJobButton
        ])
      )
      toolbarItems = new App.Components.Toolbar.ItemsCollection()
      toolbarItems.add(listJobsButton)
      toolbarItems.add(actionButtons)
      toolbarController = new App.Components.Toolbar.Controller()
      return toolbarController.createToolbar(toolbarItems)

    _rerunJob: (job, layout) ->
      # Saving an existing job will cause an HTTP UPDATE
      savingJob = App.request('job:entity:save', job)
      $.when(savingJob).done( (job) =>
        @_updateToolbarButtons(job)
      )
      $.when(savingJob).fail( (job, xhr) ->
        errorView = new App.Common.Views.ErrorView(
          message: "Failed to re-run job (#{xhr.statusText})"
        )
        layout.errorRegion.show(errorView)
      )

    showJob: (id) ->
      @_showLoadingView(id)

      fetchingJob = App.request('job:entity', id)
      $.when(fetchingJob).done( (job) =>
        layout = new Show.Layout(model: job)
        layout.on('show', ->
          layout.toolbarRegion.show(toolbarView)
          layout.detailsRegion.show(detailsView)
          if job.get('params') and job.get('params').length > 0
            layout.paramsRegion.show(paramsView)
        )

        toolbarView = @_buildToolbar(job)
        @_updateToolbarButtons(job)
        toolbarView.on('toolbar:list:jobs', ->
          App.trigger('jobs:list')
        )
        toolbarView.on('toolbar:buttonStrip:rerun:job', =>
          @_rerunJob(job, layout)
        )
        toolbarView.on('toolbar:buttonStrip:delete:job', =>
          # TODO
        )

        detailsView = new Show.JobDetailsView(model: job)
        if job.get('params') and job.get('params').length > 0
          paramsView = new Show.JobParamsView(
            collection: job.get('params')
          )

        # TODO This callback needs to be detached when this controller is closed!
        App.vent.on('job:progressed', (jobData) =>
          if jobData.id == job.id
            job.set(jobData)
          @_updateToolbarButtons(job)
        )

        App.contentRegion.show(layout)
      )
      $.when(fetchingJob).fail( =>
        @_showErrorView(id)
      )

  Show.addInitializer ->
    Show.controller = new Show.Controller()