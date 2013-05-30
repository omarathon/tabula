/**
 * Cross-app scripting.
 * Only for code that is common across Tabula UI.
 * There are specific scripts for individual modules; use those for local code.
 */
(function ($) { "use strict";
	window.Supports = {};
	window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

	// All WPopupBoxes will inherit this default configuration.
	WPopupBox.defaultConfig = {imageroot:'/static/libs/popup/'};

	// Tabula-specific rendition of tablesorter plugin for sortable tables
	jQuery.fn.sortableTable = function(settings) {
		settings = settings || {};

		var $table = $(this);
		if ($table.tablesorter) {
			var headerSettings = {};
			$('th', $table).each(function(index){
				var sortable = $(this).hasClass("sortable");
				if(!sortable){
					headerSettings[index] = {sorter: false};
				}
			});
			$table.tablesorter($.extend({headers: headerSettings}, settings));
			return this;
		}
	};

	// Tabula-specific rendition of date and date-time pickers
	jQuery.fn.tabulaDateTimePicker = function() {
		$(this).datetimepicker({
			format: "dd-M-yyyy hh:ii:ss",
			weekStart: 1,
			minView: 'day',
			autoclose: true
		}).on('show', function(ev){
			var d = new Date(ev.date.valueOf()),
				  minutes = d.getUTCMinutes(),
					seconds = d.getUTCSeconds(),
					millis = d.getUTCMilliseconds();

			if (minutes > 0 || seconds > 0 || millis > 0) {
				d.setUTCMinutes(0);
				d.setUTCSeconds(0);
				d.setUTCMilliseconds(0);

				var DPGlobal = $.fn.datetimepicker.DPGlobal;
				$(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat("dd-M-yyyy hh:ii:ss", "standard"), "en", "standard"));

				$(this).datetimepicker('update');
			}
		}).next('.add-on').css({'cursor': 'pointer'}).on('click', function() { $(this).prev("input").focus(); });
	};

	jQuery.fn.tabulaDatePicker = function() {
		$(this).datepicker({
			format: "dd-M-yyyy",
			weekStart: 1,
			minView: 'day',
			autoclose: true
		}).next('.add-on').css({'cursor': 'pointer'}).on('click', function() { $(this).prev("input").focus(); });
	};

	jQuery.fn.tabulaTimePicker = function() {
		$(this).datetimepicker({
			format: "hh:ii:ss",
			weekStart: 1,
			startView: 'day',
			maxView: 'day',
			autoclose: true
		}).on('show', function(ev){
			var d = new Date(ev.date.valueOf()),
				  minutes = d.getUTCMinutes(),
					seconds = d.getUTCSeconds(),
					millis = d.getUTCMilliseconds();

			if (seconds > 0 || millis > 0) {
				d.setUTCSeconds(0);
				d.setUTCMilliseconds(0);

				var DPGlobal = $.fn.datetimepicker.DPGlobal;
				$(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat("hh:ii:ss", "standard"), "en", "standard"));

				$(this).datetimepicker('update');
			}
		}).next('.add-on').css({'cursor': 'pointer'}).on('click', function() { $(this).prev("input").focus(); });
	};

	// apply to a checkbox or radio button
	jQuery.fn.slideMoreOptions = function($slidingDiv, showWhenChecked) {
		var $this = $(this);
		var name = $this.attr("name");
		var $form = $this.closest('form');

		// for checkboxes, there will just be one target - the current element (which will have the same name as itself).
		// for radio buttons, each radio button will be a target.  They are identified as a group because they all have the same name.
		var $changeTargets = $("input[name='" + name + "']", $form);
		if (showWhenChecked) {
			$changeTargets.change(function(){
				if ($this.is(':checked'))
					$slidingDiv.stop().slideDown('fast');
				else
					$slidingDiv.stop().slideUp('fast');
			});
			$this.trigger('change');
		} else {
			$changeTargets.change(function(){
				if ($this.is(':checked'))
					$slidingDiv.stop().slideUp('fast');
				else
					$slidingDiv.stop().slideDown('fast');
			});
			$this.trigger('change');
		}
	};


	// submit bootstrap form using Ajax
	jQuery.fn.ajaxSubmit = function(successCallback) {
		$(this).on('submit', 'form', function(e){
			e.preventDefault();
			var $form = $(this);
			$.post($form.attr('action'), $form.serialize(), function(data){
				var scopeSelector = (data.formId != undefined) ? "#" + data.formId + " " : "";

				if(data.status == "error"){
					// delete any old errors
					$(scopeSelector + "span.error").remove();
					$(scopeSelector + '.error').removeClass('error');
					var error;
					for(error in data.result){
						var message = data.result[error];
						var inputSelector = scopeSelector + "input[name='" + error + "']";
						var textareaSelector = scopeSelector + "textarea[name='" + error + "']";

						var $field = $(inputSelector + ", " + textareaSelector);
						$field.closest(".control-group").addClass("error");

						// insert error message
						$field.last().after('<span class="error help-inline">'+message+'</span>');
					}
				} else {
					successCallback(data)
				}
			});
		});
	}

	/*
	 * Prepare a spinner and store reference in data store.
	 * Add spinner-* classes to control positioning and automatic spinning
	 *
	 * Otherwise methods from spin.js to instantiate, eg:
	 * $(el).data('spinContainer').spin('small');
	 * $(el).data('spinContainer').spin(false);
	 */
	jQuery.fn.tabulaPrepareSpinners = function(selector) {
		selector = selector || '.spinnable';

		// filter selector and descendants
		var $spinnable = $(this).find(selector).add($(this).filter(selector));

		if ($spinnable.length) {
			// stop any delayed spinner
			if (window.pendingSpinner != undefined) {
				clearTimeout(window.pendingSpinner);
				window.pendingSpinner = null;
			}

			$spinnable.each(function() {
				var $this = $(this);

				if ($this.data('spinContainer')) {
					// turn off any existing spinners
					$this.data('spinContainer').spin(false);
				} else {
					// create new spinner element
					var $spinner = $('<div class="spinner-container" />');

					// position new spinner
					if ($this.is('.spinner-prepend')) {
						$this.prepend($spinner);
					} else if ($this.is('.spinner-append')) {
						$this.append($spinner);
					} else if ($this.is('.spinner-before')) {
						$this.before($spinner);
					} else if ($this.is('.spinner-after')) {
						$this.after($spinner);
					} else {
						// default centred on element itself
						$spinner.remove();
						$this.data('spinContainer', $this);
					}

					if (!$this.data('spinContainer')) {
						// if not yet stored...
						$this.data('spinContainer', $spinner);
					}

					if ($this.is('.spinner-auto')) {
						// spin only after 500ms
						$this.click(function(e) {
							var $container = $this.data('spinContainer');
							window.pendingSpinner = setTimeout(function() { $container.spin('small'); }, 500);
						});
					}
				}
			});
		}
	};

	/*
	 * .double-submit-protection class on a form will detect submission
	 * and prevent submitting twice. It will also visually disable any
	 * .btn items in a .submit-buttons container.
	 *
	 * Obviously this won't make it impossible to submit twice, if JS is
	 * disabled or altered.
	 */
	jQuery.fn.tabulaSubmitOnce = function() {
		var $this = $(this);

		if ($this.is('form') && !$this.data('submitOnceHandled')) {
			$this.data('submitOnceHandled', true);
			$this.removeData('submitOnceSubmitted');

			$(this).on('submit', function(event) {
				var $this = $(event.target),
					submitted = $this.data('submitOnceSubmitted');

				if (!submitted) {
					var $buttons = $this.find('.submit-buttons .btn').not('.disabled');
					$buttons.addClass('disabled');
					$this.data('submitOnceSubmitted', true);
					// For FF and other browsers with BFCache/History Cache,
					// re-enable the form if you click Back.
					$(window).on('pageshow', function() {
						$buttons.removeClass('disabled');
						$this.removeData('submitOnceSubmitted');
					});
					return true;
				} else {
					event.preventDefault();
					return false;
				}
			});
		}
	};
	
	
	/* 
		Customised Popover wrapper. Implements click away to dismiss.
	*/
	$.fn.tabulaPopover = function(options) {
		var $items = this;
		
		$items.on('click', function(e) {
			if ($(this).hasClass('disabled')) {
				e.stopImmediatePropagation();
				e.preventDefault();
			}
		});
		
		$items.popover(options);
		
		// Click away to dismiss
		$('html').on('click', function(e) {
			// if clicking anywhere other than the popover itself
			if ($(e.target).closest('.popover').length === 0) {
				$items.popover('hide');
			}
		});
		
		return $items;
	};

	// on ready
	$(function() {
		// form behavioural hooks
		$('input.date-time-picker').tabulaDateTimePicker();
		$('input.date-picker').tabulaDatePicker();
		$('input.time-picker').tabulaTimePicker();
		$('form.double-submit-protection').tabulaSubmitOnce();

		// prepare spinnable elements
		$('body').tabulaPrepareSpinners();

		// repeat these hooks for modals when shown
		$('body').on('shown', '.modal', function() {
			var $m = $(this);
			$m.find('input.date-time-picker').tabulaDateTimePicker();
			$m.find('input.date-picker').tabulaDatePicker();
			$m.find('input.time-picker').tabulaTimePicker();
			$m.find('form.double-submit-protection').tabulaSubmitOnce();
			$m.tabulaPrepareSpinners();
		});

		/* When a .long-running link is clicked it will be
		 * replaced with "Please wait" text, to tell the user to expect to
		 * wait a few seconds.
		 */
		$('a.long-running').click(function (event) {
			var $this = $(this);
			var originalText = $this.html();
			if (!$this.hasClass('clicked') && !$this.hasClass('disabled') && !$this.parent().hasClass('disabled')) {
				$this.addClass('clicked').css({opacity:0.5}).width($this.width()).html('Please wait&hellip;');
				setTimeout(function(){
					$this.removeClass('clicked').css({opacity:1}).html(originalText);
				}, 5000);
				return true;
			} else {
				event.preventDefault();
				return false;
			}
		});

		$('a.copyable-url').copyable({prefixLinkText:true}).tooltip();

		// add .use-tooltip class and title attribute to enable cool looking tooltips.
		// http://twitter.github.com/bootstrap/javascript.html#tooltips
		$('.use-tooltip').tooltip();

		// add .use-popover and optional data- attributes to enable a cool popover.
		// http://twitter.github.com/bootstrap/javascript.html#popovers
		$('.use-popover').popover().click(function(){ return false; });

		// add .use-introductory for custom popover.
		// https://github.com/twitter/bootstrap/issues/2234
		$('.use-introductory').popover({
			template: '<div class="popover introductory"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div><div class="footer"><form class="form-inline"><label class="checkbox"><input type="checkbox"> Don\'t show me this again</label></form></div></div></div>'
		});

		$('.use-introductory:not(.auto)').each(function() {
			var template = $(this).data('popover').options.template;
			$(this).data('popover').options.template = template.replace('<input type="checkbox">', '<input type="checkbox" checked="checked">');
		});

		// auto-show introductory popover on load, based on class
		$('.use-introductory.auto').popover('show');

		// make introductory popovers closable
		$('#container').on('click', '.introductory .close', function(e) {
			$(e.target).parents('.introductory').prev().popover('hide');
		});

		// persist introductory popover auto-show state
		$('#container').on('change', '.introductory .footer input', function(e) {
			// If intro text is changed to reflect new features, change its id to ensure end users see the new version
			var id = $(e.target).parents('.introductory').prev().prop('id');
			var hash = $(e.target).parents('.introductory').prev().data('hash');
			// use this hook to persist showOnLoad state with some ajax shizzle
			$.post('/settings/showIntro/' + hash, { dismiss: $(this).is(':checked') });
		});

		// apply details/summary polyfill
		// https://github.com/mathiasbynens/jquery-details
		$('html').addClass($.fn.details.support ? 'details' : 'no-details');
		$('details').details();

		// togglers
		$(".tabula-page").on("click", ".open-all-details", function() {
			$("html.no-details details:not(.open) summary").click();
			$("html.details details:not([open]) summary").click();
			$(".open-all-details").hide();
			$(".close-all-details").show();
		});
		$(".tabula-page").on("click", ".close-all-details", function() {
			$("html.no-details details.open summary").click();
			$("html.details details[open] summary").click();
			$(".close-all-details").hide();
			$(".open-all-details").show();
		});

		// collapsible striped section
		$('.striped-section.collapsible').each(function() {
			var $section = $(this);
			var open = function() {
				return $section.hasClass('expanded');
			};

			var $icon = $('<i class="icon-fixed-width"></i>');
			if (open()) $icon.addClass('icon-chevron-down');
			else $icon.addClass('icon-chevron-right');

			var $title = $section.find('.section-title');
			$title.prepend(' ').prepend($icon);

			$title.css('cursor', 'pointer').on('click', function() {
				if (open()) {
					$section.removeClass('expanded');
					$icon.removeClass('icon-chevron-down').addClass('icon-chevron-right');
				} else {
					$section.addClass('expanded');
					$icon.removeClass('icon-chevron-right').addClass('icon-chevron-down');

					if ($section.data('name')) {
						window.location.hash = $section.data('name');
					}
				}
			});

			if (!open() && window.location.hash && window.location.hash.substring(1) == $section.data('name')) {
				// simulate a click
				$title.trigger('click');
			}
		});

		// sticky table headers
		//$('table.sticky-table-headers').stickyTableHeaders({
		//	fixedOffset: $('#navigation')
		//});
	}); // on ready
})(jQuery);
