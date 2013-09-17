OnCue.module "Jobs.List", (List, OnCue, Backbone, Marionette, $, _) ->

  # Don't start this module automatically
  @startWithParent = false

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new OnCue.Common.Views.LoadingView(message: 'Loading jobs...')
      OnCue.contentRegion.show(loadingView)

    _showErrorView: ->
      errorView = new OnCue.Common.Views.ErrorView(message: 'Failed to load jobs')
      OnCue.contentRegion.show(errorView)

    #
    # Build up a toolbar component from a model and create the controller
    #
    _buildToolbar: (jobs) =>

      runTestJobButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'Run test'
        tooltip: 'Run a test job'
        iconClass: 'icon-play'
        event: 'run:test:job'
      )

      @rerunJobButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'Re-run'
        tooltip: 'Re-run complete or failed jobs'
        iconClass: 'icon-repeat'
        enabled: false
        event: 'rerun:job'
      )

      @deleteJobButton = new OnCue.Components.Toolbar.ButtonModel(
        title: 'Delete'
        tooltip: 'Delete jobs permanently'
        iconClass: 'icon-trash'
        enabled: false
        event: 'delete:job'
      )

      actionButtons = new OnCue.Components.Toolbar.ButtonStripModel(
        cssClasses: 'pull-right'
        buttons: new OnCue.Components.Toolbar.ButtonCollection([
          runTestJobButton, @rerunJobButton, @deleteJobButton
        ])
      )

      stateFilter = new OnCue.Components.Toolbar.FilterModel(
        title: 'State'
        event: 'state:filter:changed'
        menuItems: new OnCue.Components.Toolbar.FilterItemsCollection([
          new OnCue.Components.Toolbar.FilterItemModel(title: 'Queued', value: 'queued')
          new OnCue.Components.Toolbar.FilterItemModel(title: 'Running', value: 'running')
          new OnCue.Components.Toolbar.FilterItemModel(title: 'Complete', value: 'complete')
          new OnCue.Components.Toolbar.FilterItemModel(title: 'Failed', value: 'failed')
        ])
      )

      workers = _.unique(_.map(jobs.models, (job) -> job.get('worker_type')))
      workerFilter = new OnCue.Components.Toolbar.FilterModel(
        title: 'Worker'
        event: 'workers:filter:changed'
        menuItems: new OnCue.Components.Toolbar.FilterItemsCollection(
          _.map(workers, (worker) ->
            new OnCue.Components.Toolbar.FilterItemModel(title: worker, value: worker)
          )
        )
      )

      toolbarItems = new OnCue.Components.Toolbar.ItemsCollection()
      toolbarItems.add(stateFilter)
      toolbarItems.add(workerFilter)
      toolbarItems.add(actionButtons)
      return new OnCue.Components.Toolbar.Controller(
        collection: toolbarItems
      )


    _buildGrid: (jobs) ->
      gridModel = new OnCue.Components.Grid.Model(
        items: jobs
        paginated: true
        emptyView: OnCue.Jobs.List.NoJobsView
        backgrid:
          row: OnCue.Jobs.List.JobStateRow
          columns: [
            name: 'selector'
            cell: 'select-row'
            headerCell: 'select-all'
          ,
            name: 'id'
            label: 'ID'
            editable: false
            cell: List.JobIDCell
          ,
            name: 'worker_type'
            label: 'Worker'
            editable: false
            cell:  OnCue.Jobs.List.WorkerCell
          ,
            name: 'enqueued_at'
            label: 'Enqueued at'
            editable: false
            cell: Backgrid.Extension.MomentCell.extend(
              displayFormat: 'lll'
            )
          ,
            name: 'started_at'
            label: 'Started at'
            editable: false
            cell: Backgrid.Extension.MomentCell.extend(
              displayFormat: 'lll'
            )
          ,
            name: 'completed_at'
            label: 'Completed at'
            editable: false
            cell: Backgrid.Extension.MomentCell.extend(
              displayFormat: 'lll'
            )
          ,
            name: 'state'
            label: 'State'
            editable: false
            cell: Backgrid.StringCell.extend(className: 'capitalised min-width')
          ,
            name: 'rerun'
            label: ''
            editable: false
            cell: OnCue.Jobs.List.RerunCell
          ,
            name: 'progress'
            label: 'Progress'
            editable: false
            cell: OnCue.Jobs.List.ProgressCell
          ]
      )
      return gridController = new OnCue.Components.Grid.Controller(
        model: gridModel
      )

    _runTestJob: (jobs, layout) ->
      testJob = new OnCue.Entities.Job.Model(
        worker_type: 'oncue.worker.TestWorker'
      )
      savingJob = OnCue.request('job:entity:save', testJob)
      $.when(savingJob).done( (job) ->
        job.set('is_new', true)
        jobs.add(job)
      )
      $.when(savingJob).fail( (job, xhr) ->
        errorView = new OnCue.Common.Views.ErrorView(
          message: "Failed to kick off a test job (#{xhr.statusText})"
        )
        layout.errorRegion.show(errorView)
      )

    # Using the state of all drop-down filter menus, filter the
    # jobs in the grid
    _filterJobs: (jobs, filterModels) ->
      jobs.setFilter( (job) ->
        showJob = true
        for filterModel in filterModels
          if filterModel.get('title') is 'State'
            state = job.get('state')
            for menuItem in filterModel.get('menuItems').models
              if menuItem.get('value') == state and menuItem.get('selected') is not true
                showJob = false
          else if filterModel.get('title') is 'Worker'
            worker = job.get('worker_type')
            for menuItem in filterModel.get('menuItems').models
              if menuItem.get('value') == worker and menuItem.get('selected') is not true
                showJob = false
        return showJob
      )

    _rerunJobs: (jobs, layout) =>
      for job in jobs
        # Saving an existing job will cause an HTTP UPDATE
        savingJob = OnCue.request('job:entity:save', job)
        $.when(savingJob).done( (job) =>
          @_updateRerunButton(jobs)
          @_updateDeleteButton(jobs)
        )
        $.when(savingJob).fail( (job, xhr) ->
          errorView = new OnCue.Common.Views.ErrorView(
            message: "Failed to re-run job (#{xhr.statusText})"
          )
          layout.errorRegion.show(errorView)
        )

    _deleteJobs: (jobs, layout) =>
      for job in jobs
        deletingJob = OnCue.request('job:entity:delete', job)
        $.when(deletingJob).fail( (job, xhr) ->
          errorView = new OnCue.Common.Views.ErrorView(
            message: "Failed to delete job (#{xhr.statusText})"
          )
          layout.errorRegion.show(errorView)
        )

    # Determine whether the re-run button should be enabled
    _updateRerunButton: (selectedJobs) =>
      if not @rerunJobButton then throw 'Re-run job button undefined'
      if selectedJobs.length == 0
        @rerunJobButton.set('enabled', false)
        return
      rerunnable = true
      for job in selectedJobs
        state = job.get('state')
        unless state is 'complete' or state is 'failed'
          rerunnable = false
      @rerunJobButton.set('enabled', rerunnable)

    # Determine whether the delete button should be enabled
    _updateDeleteButton: (selectedJobs) =>
      if not @deleteJobButton then throw 'Delete job button undefined'
      if selectedJobs.length == 0
        @deleteJobButton.set('enabled', false)
        return
      deletable = true
      for job in selectedJobs
        state = job.get('state')
        unless state is 'queued' or state is 'complete' or state is 'failed'
          deletable = false
      @deleteJobButton.set('enabled', deletable)

    listJobs: ->

      # Show a loading view
      @_showLoadingView()

      # Fetch the collection of jobs from the server
      fetchingJobs = OnCue.request('job:entities')
      $.when(fetchingJobs).done( (jobs) =>

        # Remove all previous event handlers on this controller
        @stopListening()

        # Create a pageable collection for the grid
        pageableJobs = new Backbone.PageableCollection(jobs.clone().models,
          mode: 'client'
          state:
            pageSize: 15
            sortKey: 'id'
            order: 1
        )

        # Create the layout
        layout = new List.Layout()

        # Create the toolbar
        toolbarController = @_buildToolbar(jobs)
        toolbarView = toolbarController.getView()

        # Create the grid
        gridController = @_buildGrid(pageableJobs)
        gridLayout = gridController.getLayout()

        # Listen to changes in the underlying job collection
        # and reset and sort the pageable collection
        @listenTo(jobs.filtered, 'reset', (collection) ->
          pageableJobs.fullCollection.reset(jobs.filtered.models)
          pageableJobs.fullCollection.sort()
        )

        # Listen to toolbar events
        @listenTo(toolbarView, 'state:filter:changed', (filterModels) =>
          @_filterJobs(jobs, filterModels)
        )
        @listenTo(toolbarView, 'workers:filter:changed', (filterModels) =>
          @_filterJobs(jobs, filterModels)
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:run:test:job', =>
          @_runTestJob(jobs, layout)
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:rerun:job', =>
          selectedJobs = gridController.getSelectedModels()
          @_rerunJobs(selectedJobs, layout)
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:delete:job', =>
          selectedJobs = gridController.getSelectedModels()
          @_deleteJobs(selectedJobs, layout)
        )

        # Listen to job selections
        debouncedUpdateRerunButton = _.debounce(@_updateRerunButton, 10)
        debouncedUpdateDeleteButton = _.debounce(@_updateDeleteButton, 10)
        @listenTo(pageableJobs, 'backgrid:selected', (model, isSelected) =>
          selected = gridController.getSelectedModels()
          debouncedUpdateRerunButton(selected)
          debouncedUpdateDeleteButton(selected)
        )

        # Layout components when the layout is displayed
        @listenTo(layout, 'show', ->
          layout.toolbarRegion.show(toolbarView)
          layout.jobsRegion.show(gridLayout)
        )

        # This is for the 'No Jobs' view, which offers a link to create a test job
        @listenTo(OnCue.vent, 'run:test:job', =>
          @_runTestJob(jobs, layout)
        )

        # Display the layout
        OnCue.contentRegion.show(layout)
      )
      $.when(fetchingJobs).fail( =>
        @_showErrorView()
      )

  # ~~~~~~~~~~~~~

  List.addInitializer ->
    List.controller = new List.Controller()

  List.addFinalizer ->
    List.controller.close()
    delete List.controller