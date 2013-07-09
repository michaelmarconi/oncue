App.module "Entities", (Entities, App, Backbone, Marionette, $, _) ->

  #
  # A model for a button
  #
  #      title: The text that appears on the button (mandatory)
  #       icon: The class name of the icon that appears on the button (optional)
  # cssClasses: A list of extra classes to apply to the button (optional)
  #      event: The name of the event to fire when the button is clicked (mandatory)
  #
  class Entities.ToolbarButton extends Backbone.Model
    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.title then return 'A title attribute is required'
      if not attrs.event then return 'An event attribute is required'

  #
  # Model a toolbar filter dropdown menu
  #
  #      title: The text that appears on the dropdown button (mandatory)
  #  className: A list of extra classes to apply to the dropdown button (optional)
  #      event: The name of the event to fire when the filter changes (mandatory)
  #
  class Entities.ToolbarFilter extends Backbone.Model
    defaults:
      filtered: false

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.title then return 'A title attribute is required'
      if not attrs.event then return 'An event attribute is required'
      if not attrs.menuItems then return 'A collection of dropdown menu items is required'

  #
  # A dropdown menu item
  #
  #   title: The display title of this menu item
  #
  class Entities.ToolbarFilterItem extends Backbone.Model
    defaults:
      selected: true

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.title then return 'A title attribute is required'

  #
  # A collection of filter dropdown menu items
  #
  class Entities.ToolbarFilterItems extends Backbone.Collection
    model: Entities.ToolbarFilterItem

  #
  # A collection of ToolbarButton and ToolbarFilter items
  #
  class Entities.ToolbarItems extends Backbone.Collection


