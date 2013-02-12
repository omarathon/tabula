/**
 * This files contains small bits of script that haven't been refactored
 * into separate files. Generally new script should go in a different file, but
 * if something is used everywhere and would be overkill in a separate file, then
 * it can go in here.
 */
(function ($) { "use strict";

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

// All WPopupBoxes will inherit this default configuration.
WPopupBox.defaultConfig = {imageroot:'/static/libs/popup/'};

jQuery(function ($) {
	
	$('input.date-time-picker').datetimepicker({
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
	});

    /* When a .long-running link is clicked it will be
     * replaced with "Please wait" text, to tell the user to expect to
     * wait a few seconds.
     */ 
	$('a.long-running').click(function (event) {
		var $this = $(this);
		var originalText = $this.html();
		if (!$this.hasClass('clicked') && !$this.hasClass('disabled')) {
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

}); // end domready

}(jQuery));