OnCue.module "Schedule.List", (List, OnCue, Backbone, Marionette, $, _) ->

#  #
#  # The view to display when there are no agents registered
#  #
#  class List.NoAgentsView extends Marionette.ItemView
#    template: '#no_agents_view'
#    className: 'alert alert-info alert-block'
#
#  #
#  # Render an individual agent in the collection
#  #
#  class List.AgentView extends Marionette.ItemView
#    tagName: 'tr'
#    template: '#agents_item_view'
#
#  #
#  # A view to render a table of the collection of agents
#  #
#  class List.AgentsView extends Marionette.CompositeView
#    template: '#agents_list_view'
#    emptyView: List.NoAgentsView
#    itemView: List.AgentView
#
#    # We want to replace the table with a 'no agents' view
#    # when there are no registered agents, rather than rendering the
#    # 'emptyView' into the table.
#    appendHtml: (collectionView, itemView, index) ->
#      if itemView instanceof List.NoAgentsView
#        @originalCollectionView = collectionView.$el.clone()
#        collectionView.$el.html(itemView.el)
#      else
#        if index == 0
#          collectionView.$el.html(@originalCollectionView)
#        collectionView.$el.find('tbody').append(itemView.el)