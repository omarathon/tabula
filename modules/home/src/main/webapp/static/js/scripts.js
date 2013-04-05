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
		var $table = $(this);
		if ($table.tablesorter) {
			var headerSettings = {};
			$('th', $table).each(function(index){
				var sortable = $(this).hasClass("sortable");
				if(!sortable){
					headerSettings[index] = {sorter: false};
				}
			});
			$table.tablesorter({headers: headerSettings});
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
	
	// on ready
	$(function() {
		$('input.date-time-picker').tabulaDateTimePicker();	
		$('input.date-picker').tabulaDatePicker();
	
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
		
		/*
		 * .double-submit-protection class on a form will detect submission
		 * and prevent submitting twice. It will also visually disable any
		 * .btn items in a .submit-buttons container. 
		 * 
		 * Obviously this won't make it impossible to submit twice, if JS is
		 * disabled or altered.
		 */
		$('form.double-submit-protection').on('submit', function(event) {
			var $this = $(event.target),
			    submitted = $this.data('already-submitted');
			
			if (!submitted) {
				var $buttons = $this.find('.submit-buttons .btn').not('.disabled');
				$buttons.addClass('disabled');
				$this.data('already-submitted', true);
				// For FF and other browsers with BFCache/History Cache,
				// re-enable the form if you click Back.
				$(window).on('pageshow', function() {
					$buttons.removeClass('disabled');
					$this.removeData('already-submitted');
				});
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
			
			var $icon = $('<i></i>');
			if (open()) $icon.addClass('icon-chevron-down');
			else $icon.addClass('icon-chevron-right');
			
			var $title = $section.find('.section-title'); 
			$title.prepend(' ').prepend($icon);
			
			var buffer = $title.height() / 2 - 10;
			$icon.css('margin-top', buffer + 'px');
			
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
	}); // on ready
})(jQuery);
