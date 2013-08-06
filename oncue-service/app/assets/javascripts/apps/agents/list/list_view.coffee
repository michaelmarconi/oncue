App.module "Agents.List", (List, App, Backbone, Marionette, $, _) ->

  #
  # The view to display when there are no agents registered
  #
  class List.NoAgentsView extends Marionette.ItemView
    template: '#no_agents_view'
    className: 'alert alert-info alert-block'

  #
  # Render an individual agent in the collection
  #
  class List.AgentView extends Marionette.ItemView
    tagName: 'tr'
    template: '#agents_item_view'

  #
  # A view to render a table of the collection of agents
  #
  class List.AgentsView extends Marionette.CompositeView
    template: '#agents_list_view'
    emptyView: List.NoAgentsView
    itemView: List.AgentView

    appendHtml: (collectionView, itemView, index) ->
      if @collection.length > 0
        collectionView.$('tbody').append(itemView.el)
      else
        # TODO fix this! (note the itemView)
        collectionView.$el.html(itemView.el)