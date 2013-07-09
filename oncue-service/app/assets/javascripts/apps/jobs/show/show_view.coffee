App.module "Jobs.Show", (Show, App, Backbone, Marionette, $, _) ->

  class Show.JobView extends Marionette.ItemView
    template: '#job_view'

  class Show.MissingJobView extends Marionette.ItemView
    template: '#missing_job_view'