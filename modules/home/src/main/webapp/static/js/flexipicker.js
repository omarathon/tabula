(function($){ // I'm wrapping all day

/**
 * RichResultField is a text field that can be overlaid with a similar-looking
 * box containing arbitrary text. Useful if you want to save a particular value
 * from a picker but display something more friendly from the user.
 *
 * Requires Bootstrap.
 */
var RichResultField = function (input) {
  var self = this;
  this.$input = $(input);
  this.$uneditable = $('<span><span class=val></span>'+
             '<a href=# class="clear-field" title="Clear">&times;</a></span>');
  this.$uneditable.attr('class', 'uneditable-input rich-result-field ' + this.$input.attr('class'));
  this.$input.after(this.$uneditable);
  this.$uneditable.find('a').click(function () {
    self.edit();
    return false;
  });
  this.$uneditable.hide();
};

/** Clear field, focus for typing */
RichResultField.prototype.edit = function () {
  this.$input.val('').show().focus();
  this.$uneditable.hide().find('.val').text('');
};

/** Hide input field and show the rich `text` instead */
RichResultField.prototype.storeText = function (text) {
  this.$input.hide();
  this.$uneditable.show().find('.val').text(text);
};

/** Set value of input field, hide it and show the rich `text` instead */
RichResultField.prototype.store = function (value, text) {
  this.$input.val(value).trigger('change').hide();
  this.$uneditable.show().find('.val').text(text);
};

/**
 * An AJAX autocomplete-style picker that can return a variety of different
 * result types, such as users, webgroups, and typed-in email addresses.
 *
 * The actual searching logic is handled by the corresponding controller -
 * this plugin just passes it option flags to tell it what to search.
 *
 * Requires Bootstrap with the Typeahead plugin.
 */
var FlexiPicker = function (options) {
  var self = this;
  var $element = $(options.input);

  // Might have manually wired this element up with FlexiPicker,
  // but add the class for CSS style purposes.
  if (!$element.hasClass('flexi-picker')) {
	  $element.addClass('flexi-picker');
  }

  // Disable browser autocomplete dropdowns, it gets in the way.
  $element.attr('autocomplete', 'off');

  this.includeUsers = options.includeUsers !== false;
  this.includeGroups = options.includeGroups || false;
  this.includeEmail = options.includeEmail || false;
  this.prefixGroups = options.prefixGroups || '';

  this.richResultField = new RichResultField($element[0]);

  this.iconMappings = {
    user: 'user',
    group: 'globe',
    email: 'envelope'
  };

  var $typeahead = $element.typeahead({
    source: function (query, process) {
      self.search(query).done(function (results) {
        process(results);
      });
    },
    item: '<li class=flexi-picker-result><a href="#"><i></i><span class=title></span><span class=type></span><div class=description></div></a></li>'
  }).data('typeahead');

  // Overridden lookup() method uses this to delay the AJAX call
  $typeahead.delay = 100;

  // Provides delayed AJAX lookup - called by lookup() method.
  $typeahead._delaySource = function(query, source, process) {
    if(this.calledOnce === undefined)  {
      this.calledOnce = true;
      var items = source(query, process);
      if (items) {
        this.delay = 0;
        return items;
      }
    } else {
      if (this.delay === 0) {
        return source(query, process);
      } else {
        if(this.timeout) {
          clearTimeout(this.timeout);
        }
        this.timeout = setTimeout(function() {
          return source(query, process);
        }, this.delay);
      }
    }
  };

  // Disable clientside sorting - server will sort where appropriate.
  $typeahead.sorter = function(items) { return items; }

  // The server will filter - don't do clientside filtering
  $typeahead.matcher = function(item) { return true; }

  // Mostly the same as the original but delays the lookup if a delay is set.
  $typeahead.lookup = function (event) {
    var items;
    this.query = this.$element.val();
    if (!this.query || this.query.length < this.options.minLength) {
      return this.shown ? this.hide() : this
    }
    items =  ($.isFunction(this.source)) ? this._delaySource(this.query, this.source, $.proxy(this.process, this)) : this.source;
    return items ? this.process(items) : this
  };

  // Renders each result item with icon and description.
  $typeahead.render = function (items) {
    var that = this;

    items = $(items).map(function (i, item) {
      if (item != undefined) {
        i = $(that.options.item);
        i.attr('data-value', item.value);
        i.attr('data-type', item.type);
        i.find('span.title').html(that.highlighter(item.title));
        i.find('span.type').html(item.type);
        i.find('i').addClass('icon-' + self.iconMappings[item.type]);
        var desc = i.find('.description');
        if (desc && desc != '') {
              desc.html(item.description).show();
        } else {
          desc.hide();
        }
        return i[0];
      } else {
        // no idea what's happened here. Return an empty item.
        return $(that.options.item)[0];
      }
    });

    items.first().addClass('active');
    this.$menu.html(items);
    return this;
  };

  // On selecting a value, we can transform it here before it's stored in the field.
  $typeahead.updater = function(value) {
	  var type = this.$menu.find('.active').attr('data-type');
	  return self.getValue(value, type);
  }

  // Override select item to place both the value in the field
  // and a more userfriendly text in the "rich result".
  var oldSelect = $typeahead.select;
  $typeahead.select = function () {
    var text = this.$menu.find('.active .title').text();
    var desc = this.$menu.find('.active .description').text();
    if (desc) { text = text + ' (' + desc + ')'; }
    self.richResultField.storeText(text);
    return oldSelect.call($typeahead);
  }

  // On load, look up the existing value and give it human-friendly text if possible
  // NOTE: this relies on the fact that the saved value is itself a valid search term
  // (excluding the prefix on webgroup, which is handled by getQuery() method).
  var currentValue = $element.val();
  if (currentValue && currentValue.trim().length > 0) {
	var searchQuery = this.getQuery(currentValue);
	this.search(searchQuery, { exact: true }).done(function(results){
		if (results.length > 0) {
			self.richResultField.storeText(results[0].title + ' (' + results[0].description + ')');
		}
	});
  }
};

// Extract the value from a chosen value with type.
FlexiPicker.prototype.getValue = function(value, type) {
  if (type == 'group') {
	  value = this.prefixGroups + value;
  }
  return value;
}

// Turn value into something we can query on.
FlexiPicker.prototype.getQuery = function(value) {
	// If we prefixed something onto a group name, remove it first
	if (this.prefixGroups && value.indexOf(this.prefixGroups) == 0) {
		value = value.substring(this.prefixGroups.length);
	}
	return value;
}

FlexiPicker.prototype.transformItem = function(item) {
  if (item.type == 'user') {
    item.title = item.name;
    item.description = item.value; // usercode
  } else if (item.type == 'group') {
    item.description = item.title;
    item.title = item.value;
  } else if (item.type == 'email') {
    item.title = item.name;
    item.description = item.address;
  }
}

/** Runs the search */
FlexiPicker.prototype.search = function (query, options) {
  // We'll return a Deferred result, that will get resolved by the AJAX call
  options = options || {};
  var d = $.Deferred();
  var self = this;
  // Abort any existing search
  if (this.currentSearch) {
    this.currentSearch.abort();
    this.currentSearch = null;
  }
  this.currentSearch = $.ajax({
    url: '/api/flexipicker/query.json',
    dataType: 'json',
    data: {
      includeUsers: this.includeUsers,
      includeGroups: this.includeGroups,
      includeEmail: this.includeEmail,
      query: query,
      exact: options.exact // if true, only returns 100% matches.
    },
    success: function(json, textStatus, jqxhr) {
      if (json.data && json.data.results) {
        var results = json.data.results;
        // Massage incoming JSON a bit to fill in title and description.
        $.each(results, function(i, item){
          self.transformItem(item);
        });
        // Resolve the deferred result, triggering any handlers
        // that may have been registered against it.
        d.resolve(results);
      }
    }
  });
  // unset the search when it's done
  this.currentSearch.always(function(){
    self.currentSearch = null;
  });
  return d;
};

// The jQuery plugin
$.fn.flexiPicker = function (options) {
  this.each(function (i, field) {
    var $this = $(this);
    if ($this.data('flexi-picker')) {
      throw new Error("FlexiPicker has already been added to this element.");
    }
    var allOptions = {
      input: this,
      includeGroups: $this.data('include-groups'),
      includeEmail: $this.data('include-email'),
      includeUsers: $this.data('include-users') !== false,
      prefixGroups: $this.data('prefix-groups') || ''
    };
    $.extend(allOptions, options || {});
    $this.data('flexi-picker', new FlexiPicker(allOptions));
  });
  return this;
};

/**
 * Any input with the flexi-picker class will have the picker enabled on it,
 * so you can use the picker without writing any code yourself.
 *
 * More likely you'd use the flexi-picker tag.
 */
jQuery(function($){
  $('.flexi-picker').flexiPicker({});
});

// End of wrapping
})(jQuery);