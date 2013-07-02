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
		var $this = $(this)
		// if there is no datepicker bound to this input then add one
		if(!$this.data("datepicker")){
			$this.datetimepicker({
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
			}).next('.add-on').css({'cursor': 'pointer'}).on('click', function() {$(this).prev("input").focus();});
		}
	};

	jQuery.fn.tabulaDatePicker = function() {
		var $this = $(this)
		// if there is no datepicker bound to this input then add one
		if(!$this.data("datepicker")){
			$this.datepicker({
				format: "dd-M-yyyy",
				weekStart: 1,
				minView: 'day',
				autoclose: true
			}).next('.add-on').css({'cursor': 'pointer'}).on('click', function() {$(this).prev("input").focus();});
		}
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

	$(function(){
		$('a.disabled').on('click', function(e) {
			e.preventDefault();
		});
	});

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

		/* SPECIAL: popovers don't inherently know their progenitor, yet popover methods
		 * (eg. hide) are *only* callable on *that original element*. So to close
		 * a specific popover (or introductory) programmatically you need to jump hoops.
		 * Lame.
		 * Workaround is to handle the shown event on the calling element,
		 * call its popover() method again to get an object reference and then go diving
		 * for a reference to the new popover itself in the DOM.
		 */
		$('#container').on('shown', '.use-popover, .use-introductory', function(e) {
			var $po = $(e.target).popover().data('popover').tip();
			$po.data('creator', $(e.target));
		});
		$('#container').on('click', '.popover .close', function(e) {
			var $creator = $(e.target).parents('.popover').data('creator');
			if ($creator) {
				$creator.popover('hide');
			}
		});

		// ensure popovers/introductorys override title with data-title attribute where available
		$('.use-popover, .use-introductory').each(function() {
			if ($(this).attr('data-title')) {
				$(this).attr('data-original-title', $(this).attr('data-title'));
			}
		});

		// add .use-popover and optional data- attributes to enable a cool popover.
		// http://twitter.github.com/bootstrap/javascript.html#popovers
		$('.use-popover').popover({
			trigger: 'click',
			container: '#container',
			template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
		}).click(function(){ return false; });

		// add .use-introductory for custom popover.
		// https://github.com/twitter/bootstrap/issues/2234
		$('.use-introductory').popover({
			trigger: 'click',
			container: '#container',
            template: '<div class="popover introductory"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div><div class="footer"><form class="form-inline"><label class="checkbox"><input type="checkbox"> Don\'t show me this again</label></form></div></div></div>'
		}).click(function(){ return false; });

		$('.use-introductory:not(.auto)').each(function() {
			var template = $(this).data('popover').options.template;
			$(this).data('popover').options.template = template.replace('<input type="checkbox">', '<input type="checkbox" checked="checked">');
		});

		// auto-show introductory popover on load, based on class
		$('.use-introductory.auto').popover('show');

		// persist introductory popover auto-show state
		$('#container').on('change', '.introductory .footer input', function(e) {
			// If intro text is changed to reflect new features, its hash should change to ensure end users see the new version
			var hash = $(e.target).parents('.introductory').data('creator').data('hash');
			// use this hook to persist showOnLoad state with some ajax shizzle
			$.post('/settings/showIntro/' + hash, { dismiss: $(this).is(':checked') });
		});

		/* details/summary polyfill
		 * https://github.com/mathiasbynens/jquery-details
		 * WARNING: apply method ONLY to details tags.
		 * Call it on other elements for UI hilarity.
		 */
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
		//$('table.sticky-table-headers').fixedHeaderTable('show');

		// If we're on OS X, replace all kbd.keyboard-control-key with Cmd instead of Ctrl
		if (navigator.platform.indexOf('Mac') != -1) {
			$('kbd.keyboard-control-key').html('&#8984; Cmd');
		}

		// Fixed to top on scroll
		if ($('.fix-on-scroll').length) {
			var gutter = $('#navigation').height();

			$(window).scroll(function() {
				var scrollTop = $(this).scrollTop() + gutter;

				$('.fix-on-scroll:visible').each(function() {
					var $this = $(this);

					var $scrollContainer = $this.closest('.fix-on-scroll-container');
					if ($scrollContainer.length == 0) $scrollContainer = $('body');

					var height = $this.height();
					var maxHeight = $(window).height() - gutter;
					var tooHigh = (height > maxHeight);

					var floor = $scrollContainer.offset().top + $scrollContainer.height();

					var isFixed = $this.data('is-fixed');
					var pinnedToFloor = $this.data('is-pinned-to-floor');

					var offsetTop = (isFixed) ? $this.data('original-offset') : $this.offset().top;
					var pinToFloor = (scrollTop + height) > floor;

					if (!tooHigh && scrollTop > offsetTop && !isFixed) {
						// Fix it
						$this.data('original-offset', offsetTop);
						$this.data('original-width', $this.css('width'));
						$this.data('original-position', $this.css('position'));
						$this.data('original-top', $this.css('top'));

						$this.css({
							width: $this.width(),
							position: 'fixed',
							top: gutter
						});

						$this.data('is-fixed', true);
					} else if (!tooHigh && isFixed && pinToFloor) {
						// Pin to the floor
						var diff = (scrollTop + height) - floor;

						$this.css('top', gutter - diff);
						$this.data('is-pinned-to-floor', true);
					} else if (!tooHigh && isFixed && !pinToFloor && pinnedToFloor) {
						// Un-pin from the floor
						$this.css('top', gutter);
						$this.data('is-pinned-to-floor', false);
					} else if ((tooHigh || scrollTop <= offsetTop) && isFixed) {
						// Un-fix it
						$this.css('width', $this.data('original-width'));
						$this.css('position', $this.data('original-position'));
						$this.css('top', $this.data('original-top'));

						$this.data('is-fixed', false);
					}
				});
			});
		}

		// tabbable-gadgety-listy things
		var $t = $('.tabbable');
		var $panes = $t.find('.panes');

		if ($t.length && $panes.length) {
			// set up layout control
			var $lt = $('<span class="layout-tools pull-right muted"><i class="icon-folder-close hidden-phone" title="Switch to tabbed layout"></i> <i class="icon-th-large" title="Switch to gadget layout"></i> <i class="icon-reorder" title="Switch to list layout"></i><!-- <i class="icon-ok" title="Save layout settings"></i>--></span>');
			$t.prepend($lt);
			$t.trigger('tabbablechanged');

			var reset = function() { // to list
				$t.hide();
				var $cols = $t.find('.cols');
				$cols.find('.gadget').appendTo($panes);
				$cols.remove();
				$t.find('.gadget-only').children().unwrap();
				$t.find('.tab-container').remove();
				$t.find('.gadget, .tab-content, .tab-pane, .active').removeClass('gadget tab-content tab-pane active');
			}

			$(document).on('tabbablechanged', function(e) {
				$('.tooltip').remove();
				$t.show().find('.tab-container i, .layout-tools i').tooltip({ delay: { show: 750, hide: 100 } });
			});

			// layout options
			$t.on('click', '.layout-tools .icon-folder-close', function() { // tabify
				reset();
				var $tabContainer = $('<div class="row-fluid tab-container"><ul class="nav nav-tabs"></ul></div>');
				var $tabs = $tabContainer.find('ul');
				$panes.children().each(function() {
					var title = $(this).find('h4').html();
					var link = '#' + $(this).attr('id');
					var $tab = $('<li><a href="' + link + '" data-toggle="tab" data-title="' + title + '">' + title + ' <i class="icon-move" title="Click and drag to move"></i> <i class="icon-resize-small" title="Collapse"></i></a></li>');
					$tabs.append($tab);
				});
				$lt.after($tabContainer);
				$panes.addClass('tab-content').children().addClass('tab-pane');
				$t.find('.nav-tabs').sortable({ handle: '.icon-move' }).show().find('li:first a').tab('show');
				$t.trigger('tabbablechanged');
			});

			$t.on('click', '.layout-tools .icon-th-large', function() { // gadgetify
				reset();
				var $cols = $('<div class="cols row-fluid"><ol class="span6" /><ol class="span6" /></div>');
				var paneCount = $panes.children().length;
				$t.append($cols);
				$panes.children().each(function(idx) {
					var $gadget = $(this).addClass('gadget');
					var title = $(this).find('h4').html();
					var link = '#' + $(this).attr('id');
					var $tab = $('<li><a href="' + link + '" data-toggle="tab" data-title="' + title + '" title="Click and drag to move">' + title + ' <i class="icon-minus-sign-alt" title="Hide ' + title + '"></i></a></li>');
					var $gadgetHeaderTab = $('<div class="row-fluid tab-container"><ul class="nav nav-tabs"></ul></div>');
					$gadgetHeaderTab.children().append($tab);
					$gadget.wrapInner('<div class="tab-content gadget-only" />').children().wrapInner('<div class="gadget-only tab-pane active" />');
					$gadget.prepend($gadgetHeaderTab).find('.tab-container li a').tab('show');

					// populate columns (dumbly)
					$(this).appendTo(idx < paneCount/2 ? $cols.children().first() : $cols.children().last());
				});

				// make sortable & finish up rendering
				$t.find('.span6').sortable({
					handle: '.tab-container a',
					placeholder: 'sort-target',
					forcePlaceholderSize: true,
					connectWith: '.span6'
				});

				$t.trigger('tabbablechanged');
			});

			$t.on('click', '.layout-tools .icon-reorder', function() { // listify
				reset();
				$t.trigger('tabbablechanged');
			});

			// tab controls
			$t.on("click", ".tab-container .icon-resize-small", function(e) {
				e.stopPropagation();
				var $a = $(this).parent();
				var title = $a.data("title");
				$(this).prop("title", "Expand " + title);
				$a.data("href", $a.attr("href")).removeAttr("href").removeAttr("data-toggle").html($a.html().replace(title, "").replace("resize-small", "resize-full")).addClass("disabled");
				$t.trigger('tabbablechanged');
			});

			$t.on("click", ".tab-container .icon-resize-full", function(e) {
				e.stopPropagation();
				var $a = $(this).parent();
				var title = $a.data("title");
				$(this).prop("title", "Collapse");
				$a.attr("href", $a.data("href")).removeData("href").attr("data-toggle", "tab").html(title + $a.html().replace("resize-full", "resize-small")).removeClass("disabled");
				$t.trigger('tabbablechanged');
			});

			$t.on("click", ".tab-container .icon-minus-sign-alt", function(e) {
				e.stopPropagation();
				var $a = $(this).parent();
				$a.closest('.gadget').find('.tab-content').slideUp('fast');
				var title = $a.data("title");
				$(this).prop("title", "Show " + title);
				$a.data("href", $a.attr("href")).removeAttr("href").removeAttr("data-toggle").html($a.html().replace("minus-sign", "plus-sign"));
				$t.trigger('tabbablechanged');
			});

			$t.on("click", ".tab-container .icon-plus-sign-alt", function(e) {
				e.stopPropagation();
				var $a = $(this).parent();
				$a.closest('.gadget').find('.tab-content').slideDown('fast');
				var title = $a.data("title");
				$(this).prop("title", "Hide " + title);
				$a.attr("href", $a.data("href")).removeData("href").attr("data-toggle", "tab").html($a.html().replace("plus-sign", "minus-sign"));
				$t.trigger('tabbablechanged');
			});

			// default to gadgets
			$t.find('.layout-tools .icon-th-large').click();
		}
	}); // on ready



})(jQuery);

jQuery(function($){
// If there's an element with an id of 'scroll-container', max-size it to fit to the bottom of
// the page, with scrollbars if needed
    $('#scroll-container').each(function(){
        var scrollable = $(this);
        // window.height includes horizontal scrollbar on mozilla so add 20px of padding.
        var elementHeight = ($(window).height() - scrollable.offset().top) - 20;
        scrollable.css({'max-height':elementHeight,'overflow-y': 'auto'});
    });
});
