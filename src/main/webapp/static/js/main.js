(function () { "use strict";

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

jQuery(function ($) {
	
	var exports = {};
	
	/*function _dbg(msg) {
		if (window.console && console.debug) console.debug(msg);
	}
	var dbg = (window.console && console.debug) ? _dbg : function(){};*/
	
	var trim = jQuery.trim;
	
	WPopupBox.defaultConfig = {imageroot:'/static/libs/popup/'};
	
	// Lazily loaded user picker object.
	var _userPicker = null;
	function getUserPicker() {
		var targetWidth = 500, targetHeight=400;
		if (_userPicker == null) {
			
			// A popup box enhanced with a few methods for user picker
			_userPicker = new WPopupBox();
			
			_userPicker.showPicker = function (element, targetInput) {
				this.setContent("Loading&hellip;");
				this.targetInput = targetInput;
				this.setSize(targetWidth,targetHeight);
				$.get('/api/userpicker/form', function (data) {
					_userPicker.setContent(data);
					_userPicker.setSize(targetWidth,targetHeight);
					_userPicker.show();
					_userPicker.positionRight(element);
					_userPicker.decorateForm();
				});
			};
			
			/* Give behaviour to user lookup form */
			_userPicker.decorateForm = function () {
				
				var $contents = $(this.contentElement),
					$firstname = $contents.find('.userpicker-firstname'),
					$lastname  = $contents.find('.userpicker-lastname'),
					$results = $contents.find('.userpicker-results'),
					$xhr,
					onResultsLoaded;
				
				$firstname.focus();
				$contents.find('input').delayedObserver(function () {
					// WSOS will search with at least 2 chars, but let's
					// enforce at least 3 to avoid overly broad searches.
					if (trim($firstname.val()).length > 2 || 
							trim($lastname.val()).length > 2) {
						$results.html('Loading&hellip;');
						if ($xhr) $xhr.abort();
						$xhr = jQuery.get('/api/userpicker/query',  {
							firstName: $firstname.val(),
							lastName: $lastname.val()
						}, onResultsLoaded);
					}
				}, 0.5);
				
				// wire up each user Id to be clickable
				onResultsLoaded = function(data) {
					$results.html(data);
					$results.find('td.user-id').click(function(){
						var userId = this.innerHTML;
						_userPicker.targetInput.value = userId;
						_userPicker.hide();
					});
				}
				
			};
			
		}
		return _userPicker;
	}
	
	
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
	// TODO make buttons that don't take too long but are less than instantaneous
	// (~5 secs) spawn a spinner or please wait text.
	$('a.long-running').click(function (event) {
		
	});
	
	$('input.user-code-picker').each(function (i, picker) {
		var $picker = $(picker),
			$button = $('<span class="userpicker-button actions"><a href="#">Search for user</a></span>'),
			userPicker = getUserPicker(); // could be even lazier and init on click
		$picker.after("&nbsp;");
		$picker.after($button);
		$button.click(function(event){
			event.preventDefault();
			userPicker.showPicker(picker, picker);
		});
	});
	
	$('input.uni-id-picker').each(function (picker) {
		
	});
	
	var _feedbackPopup;
	var getFeedbackPopup = function() {
		if (!_feedbackPopup) {
			_feedbackPopup = new WPopupBox();
			_feedbackPopup.setSize(500,300);
		}
		return _feedbackPopup;
	}
	
	var fillInAppComments = function($form) {
		BrowserDetect.init();
		$form.find('#app-comment-os').val(BrowserDetect.OS);
		$form.find('#app-comment-browser').val(BrowserDetect.browser + ' ' + BrowserDetect.version);
		$form.find('#app-comment-resolution').val(BrowserDetect.resolution);
		var $currentPage = $form.find('#app-comment-currentPage');
		if ($currentPage.val() == '')
			$currentPage.val(window.location.href);
		BrowserDetect.findIP(function(ip){
			$form.find('#app-comment-ipAddress').val(ip);
		})
	};
	
	var TogglingSection = function ($section, $header, options) {
		var THIS = this;
		var options = options || {};
		var showByDefault = options.showByDefault || false;
		this.$section = $section;
		this.$toggleButton = $('<div class="toggle-button>(Show)</div>');
		$header.append(this.$toggleButton).addClass('clickable-cursor').click(function(){
			THIS.toggle();
			if (options.callback) options.callback();
		});
		
		if (!showByDefault) this.hide();
	};
	TogglingSection.prototype.toggle = function() {
		if (this.$section.is(':visible')) this.hide();
		else this.show();
	};
	TogglingSection.prototype.show = function() {
		this.$toggleButton.html = 'Hide';
		this.$section.show();
	};
	TogglingSection.prototype.hide = function() {
		this.$toggleButton.html = 'Show';
		this.$section.hide();
	}
	
	var decorateAppCommentsForm = function($form) {
		$form.addClass('narrowed-form');
//		var $browserInfo = $form.find('.browser-info');
//		var $heading = $form.find('.browser-info-heading');
//		new TogglingSection($browserInfo, $heading, {callback: function(){
//			getFeedbackPopup().setHeightToFit();
//		}});
	}
	
	// Fills in non-AJAX app comment form 
	$('#app-comment-form').each(function() { 
		var $form = $(this);
		fillInAppComments($form);
		decorateAppCommentsForm($form);
	});
	
	if (window.ZeroClipboard) {
		var $copiedText = $('<div>').html('Copied to clipboard.');
		
		$('a.copyable-url').each(function(){
			var $copyLink = $('<a href="#">')
					.html("Copy URL for Students")
					.attr("title", this.title);
			var $relative = $('<div>').addClass('actions').css('position','relative').append($copyLink);
			$(this).replaceWith($relative);
			
			var clip = new ZeroClipboard.Client();
			clip.setText(this.href);
			clip.setHandCursor( true );
			clip.glue($copyLink[0], $relative[0]);
			
			// Add original link as fallback Flash content, just in case.
			$relative.find('embed').append(this);
			
			clip.addEventListener('onComplete', function(client, text){
				console.log('copied', text);
				$copiedText.stop(true).hide();
				$relative[0].appendChild($copiedText[0]);
				$copiedText.slideDown().delay(2000).slideUp('slow');
			});
		});
	}
	
	$('#app-feedback-link').click(function(event){
		event.preventDefault();
		var popup = getFeedbackPopup();
		var target = event.target;
		var formLoaded = function(contentElement) {
			var $form = jQuery(contentElement).find('form');
			decorateAppCommentsForm($form);
			$form.submit(function(event){
				event.preventDefault();
				jQuery.post('/app/tell-us', $form.serialize(), function(data){
					popup.setContent(data);
					popup.positionRight(target);
					formLoaded(contentElement);
				});
			});
		};
		var formFirstLoaded = function(contentElement) {
			formLoaded(contentElement);
			fillInAppComments($(contentElement).find('form'));
		};
		
		if (popup.isShowing()) {
			popup.hide();
		} else {
			popup.showUrl('/app/tell-us', {
				method:'GET', target: target, position:'right',
				onComplete: formFirstLoaded
			});
		}
		
	});
	
	$('.assignment-info .assignment-buttons').css('opacity',0);
	$('.assignment-info').hover(function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 1);
	}, function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 0); 
	});
	
	
	
	window.Courses = exports;
	
}); // end domready

}());