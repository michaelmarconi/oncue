App.module "Jobs.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    _showLoadingView: ->
      loadingView = new App.Common.Views.LoadingView(message: 'Loading jobs...')
      App.contentRegion.show(loadingView)

    _showErrorView: ->
      errorView = new App.Common.Views.ErrorView(message: 'Failed to load jobs')
      App.contentRegion.show(errorView)

    #
    # Build up a toolbar component from a model and create the controller
    #
    _buildToolbar: (jobs) =>

      runTestJobButton = new App.Components.Toolbar.ButtonModel(
        title: 'Run test'
        tooltip: 'Run a test job'
        iconClass: 'icon-play'
        event: 'run:test:job'
      )

      @rerunJobButton = new App.Components.Toolbar.ButtonModel(
        title: 'Re-run'
        tooltip: 'Re-run complete or failed jobs'
        iconClass: 'icon-repeat'
        enabled: false
        event: 'rerun:job'
      )

      deleteJobButton = new App.Components.Toolbar.ButtonModel(
        title: 'Delete'
        tooltip: 'Delete jobs permanently'
        iconClass: 'icon-trash'
        enabled: false
        event: 'delete:job'
      )

      actionButtons = new App.Components.Toolbar.ButtonStripModel(
        cssClasses: 'pull-right'
        buttons: new App.Components.Toolbar.ButtonCollection([
          runTestJobButton, @rerunJobButton, deleteJobButton
        ])
      )

      stateFilter = new App.Components.Toolbar.FilterModel(
        title: 'State'
        event: 'state:filter:changed'
        menuItems: new App.Components.Toolbar.FilterItemsCollection([
          new App.Components.Toolbar.FilterItemModel(title: 'Queued', value: 'queued')
          new App.Components.Toolbar.FilterItemModel(title: 'Running', value: 'running')
          new App.Components.Toolbar.FilterItemModel(title: 'Complete', value: 'complete')
          new App.Components.Toolbar.FilterItemModel(title: 'Failed', value: 'failed')
        ])
      )

      workers = _.unique(_.map(jobs.models, (job) -> job.get('worker_type')))
      workerFilter = new App.Components.Toolbar.FilterModel(
        title: 'Worker'
        event: 'workers:filter:changed'
        menuItems: new App.Components.Toolbar.FilterItemsCollection(
          _.map(workers, (worker) ->
            new App.Components.Toolbar.FilterItemModel(title: worker, value: worker)
          )
        )
      )

      toolbarItems = new App.Components.Toolbar.ItemsCollection()
      toolbarItems.add(stateFilter)
      toolbarItems.add(workerFilter)
      toolbarItems.add(actionButtons)
      return new App.Components.Toolbar.Controller(
        collection: toolbarItems
      )


    _buildGrid: (jobs) ->
      gridModel = new App.Components.Grid.Model(
        items: jobs
        paginated: true
        emptyView: App.Jobs.List.NoJobsView
        backgrid:
          row: App.Jobs.List.JobStateRow
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
            cell:  App.Jobs.List.WorkerCell
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
            cell: Backgrid.StringCell.extend(className: 'capitalised min-width')
          ,
            name: 'rerun'
            label: ''
            editable: false
            cell: App.Jobs.List.RerunCell
          ,
            name: 'progress'
            label: 'Progress'
            editable: false
            cell: App.Jobs.List.ProgressCell
          ]
      )
      return gridController = new App.Components.Grid.Controller(
        model: gridModel
      )

    _runTestJob: (jobs, layout) ->
      testJob = new App.Entities.Job.Model(
        worker_type: 'oncue.worker.TestWorker'
      )
      savingJob = App.request('job:entity:save', testJob)
      $.when(savingJob).done( (job) ->
        job.set('is_new', true)
        jobs.add(job)
      )
      $.when(savingJob).fail( (job, xhr) ->
        errorView = new App.Common.Views.ErrorView(
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
        savingJob = App.request('job:entity:save', job)
        $.when(savingJob).done( (job) =>
          @_updateRerunButton(jobs)
        )
        $.when(savingJob).fail( (job, xhr) ->
          errorView = new App.Common.Views.ErrorView(
            message: "Failed to re-run job (#{xhr.statusText})"
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

    listJobs: ->

      # Show a loading view
      @_showLoadingView()

      # Fetch the collection of jobs from the server
      fetchingJobs = App.request('job:entities')
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
        @listenTo(toolbarView, 'toolbar:buttonStrip:run:test:job', (filterModels) =>
          @_runTestJob(jobs, layout)
        )
        @listenTo(toolbarView, 'toolbar:buttonStrip:rerun:job', (filterModels) =>
          selectedJobs = gridController.getSelectedModels()
          @_rerunJobs(selectedJobs, layout)
        )

        # Listen to job selections
        debouncedUpdateRerunButton = _.debounce(@_updateRerunButton, 10)
        @listenTo(pageableJobs, 'backgrid:selected', (model, isSelected) =>
          selected = gridController.getSelectedModels()
          debouncedUpdateRerunButton(selected)
        )

        # Layout components when the layout is displayed
        @listenTo(layout, 'show', ->
          layout.toolbarRegion.show(toolbarView)
          layout.jobsRegion.show(gridLayout)
        )

        # Display the layout
        App.contentRegion.show(layout)
      )
      $.when(fetchingJobs).fail( =>
        @_showErrorView()
      )

  # ~~~~~~~~~~~~~

  List.addInitializer ->
    List.controller = new List.Controller()