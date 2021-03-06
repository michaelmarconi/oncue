OnCue.module "Components.Toolbar", (Toolbar, OnCue, Backbone, Marionette, $, _) ->

  #
  # A model for a button
  #
  #      title: The text that appears on the button (mandatory)
  #    tooltip: Tooltip text on hover (optional)
  #       icon: The class name of the icon that appears on the button (optional)
  # cssClasses: A list of extra classes to apply to the button (optional)
  #    enabled: Whether or not the button is enabled (optional, boolean)
  #      event: The name of the event to fire when the button is clicked (mandatory)
  #
  class Toolbar.ButtonModel extends Backbone.Model
    defaults:
      enabled: true

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.title then return 'A title attribute is required'
      if not attrs.event then return 'An event attribute is required'

  #
  # A collection of toolbar buttons
  #
  class Toolbar.ButtonCollection extends Backbone.Collection
    model: Toolbar.ButtonModel

  #
  # A model for a strip of buttons
  #
  class Toolbar.ButtonStripModel extends Backbone.Model
    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.buttons then return 'A collection of buttons is required'

  #
  # Model a toolbar filter dropdown menu
  #
  #      title: The text that appears on the dropdown button (mandatory)
  #  className: A list of extra classes to apply to the dropdown button (optional)
  #      event: The name of the event to fire when the filter changes (mandatory)
  #
  class Toolbar.FilterModel extends Backbone.Model
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
  class Toolbar.FilterItemModel extends Backbone.Model
    defaults:
      selected: true

    initialize: (options) ->
      super(options)
      if not @isValid() then throw @validationError

    validate: (attrs, options) ->
      if not attrs.title then return 'A title attribute is required'
      if not attrs.value then return 'A value attribute is required'

  #
  # A collection of filter dropdown menu items
  #
  class Toolbar.FilterItemsCollection extends Backbone.Collection
    model: Toolbar.FilterItemModel

  #
  # A collection of ToolbarButton and ToolbarFilter items
  #
  class Toolbar.ItemsCollection extends Backbone.Collection


