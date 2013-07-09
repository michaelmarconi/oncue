App.module "Jobs.List", (List, App, Backbone, Marionette, $, _) ->

  class List.Controller extends Marionette.Controller

    @_showLoadingView: ->
      loadingView = new App.Common.Views.LoadingView(message: 'Loading jobs...')
      App.contentRegion.show(loadingView)

    @_buildToolbar: (jobs) =>

      runTestJobButton = new App.Entities.ToolbarButton(
        title: 'Run test job'
        iconClass: 'icon-play'
        cssClasses: 'btn-info pull-right'
        event: 'run:test:job'
      )

      stateFilter = new App.Entities.ToolbarFilter(
        title: 'State'
        event: 'state:filter:changed'
        menuItems: new App.Entities.ToolbarFilterItems([
          new App.Entities.ToolbarFilterItem(title: 'Queued')
          new App.Entities.ToolbarFilterItem(title: 'Running')
          new App.Entities.ToolbarFilterItem(title: 'Complete')
          new App.Entities.ToolbarFilterItem(title: 'Failed')
        ])
      )

      workers = _.unique(_.map(jobs.models, (job) -> job.get('worker_type')))
      workerFilter = new App.Entities.ToolbarFilter(
        title: 'Worker'
        event: 'workers:filter:changed'
        menuItems: new App.Entities.ToolbarFilterItems(
          _.map(workers, (worker) ->
            new App.Entities.ToolbarFilterItem(title: worker)
          )
        )
      )

      toolbarItems = new App.Entities.ToolbarItems()
      toolbarItems.add(runTestJobButton)
      toolbarItems.add(stateFilter)
      toolbarItems.add(workerFilter)
      return App.Toolbar.List.Controller.createToolbar(toolbarItems)


    @_buildGrid: (jobs) ->
      grid = new List.JobGridView(collection: jobs)
      grid.on('job:show', (id) -> App.trigger('job:show', id))
      return grid

    @_buildGridPaginator: (jobs) ->
      return new List.JobGridPaginatorView(collection: jobs)

    @_updateJob: (jobData, jobs) ->
      updatedJob = new App.Entities.Job(jobData)
      jobs.get(updatedJob.id).set(updatedJob.attributes)

    @_runTestJob: (jobs, layout) ->
      testJob = new App.Entities.Job(
        worker_type: 'oncue.worker.TestWorker'
      )
      savingJob = App.request('job:entity:save', testJob)
      $.when(savingJob).done( (job) ->
        job.set('is_new', true)
        jobs.add(job)
        jobs.fullCollection.sort()
      )
      $.when(savingJob).fail( (job, xhr) ->
        errorView = new App.Common.Views.ErrorView(
          message: "Failed to kick off a test job (#{xhr.statusText})"
        )
        layout.errorRegion.show(errorView)
      )

    # -----------
    # --- API ---
    # -----------

    @listJobs: ->
      @_showLoadingView()
      layout = new List.Layout()
      gridLayout = new List.GridLayout()
      fetchingJobs = App.request('job:entities')
      noJobsView = new List.NoJobsView()
      noJobsView.on('run:test:job', =>
#        layout.jobsRegion.show(gridLayout) TODO restructure this!
        @_runTestJob(jobs, layout)
      )
      $.when(fetchingJobs).done( (jobs) =>
        grid = @_buildGrid(jobs)
        paginator = @_buildGridPaginator(jobs)
        toolbar = @_buildToolbar(jobs)

        toolbar.on('run:test:job', =>
          @_runTestJob(jobs, layout)
        )

        toolbar.on('state:filter:changed', (filterItems) ->
          # TODO Filtering logic!
        )

        toolbar.on('worker:filter:changed', (filterItems) ->
          # TODO Filtering logic!
        )

        App.vent.on('job:progressed', (jobData) =>
          @_updateJob(jobData, jobs)
        )

        layout.on('show', ->
          layout.toolbarRegion.show(toolbar)
          if jobs.length > 0
            layout.jobsRegion.show(gridLayout)
          else
            layout.jobsRegion.show(noJobsView)
        )

        gridLayout.on('show', ->
          gridLayout.gridRegion.show(grid)
          gridLayout.paginatorRegion.show(paginator)
        )

        App.contentRegion.show(layout)
      )