(function ($) { "use strict";

window.Supports = {};
window.Supports.multipleFiles = !!('multiple' in (document.createElement('input')));

/**
 * Replace links with a textbox that selects itself on focus, and
 * gives you a hint to press Ctrl+C (or Apple+C) 
 * 
 * options:
 *   preselect: true to preselect item. Only works when working on a single element.
 */
jQuery.fn.copyable = function(options) {
  options = options || {};
  var Mac = -1!=(window.navigator&&navigator.platform||"").indexOf("Mac");
  var PressCtrlC = 'Now press '+(Mac?'\u2318':'Ctrl')+"-C to copy.";
  
  var preselect = (this.length == 1) && (!!options['preselect'] || false);
  var prefixLinkText = (!!options['prefixLinkText'] || false);
    
  this.each(function(){
    var $this = $(this),
        url = this.href,
        title = this.title,
        text = $this.html();
    var $container = $('<span class=copyable-url-container>').attr('title',title);
    var $explanation = $('<span class=press-ctrl-c>').html(PressCtrlC).hide();
    var $input = $('<input class=copyable-url>')
        .attr('readonly', true)
        .attr('value',url)
        .click(function(){
          this.select();
          $explanation.slideDown('fast');
        }).blur(function(){
          $explanation.fadeOut('fast');
        });
    $container.append($input).append($explanation);
    $this.replaceWith($container);
    if (prefixLinkText) {
    	$container.before(text);
    }
    if (preselect) {
    	$input.click();
    }
  });
}

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
	
	/**
	 * 
	 */
	var $rateFeedback;
	var decorateFeedbackForm = function() {
		$rateFeedback = $('#feedback-rating');
		var $form = $rateFeedback.find('form'),
			action = $form.attr('action');
		
		if ($form.length > 0) {
			$form.find('input[name=rating]').rating({
				callback: function(value, link) {
					if (!value) { // remove rating
						$form.append($('<input type=checkbox>').attr({'name':'unset', checked:true}).hide());
					}
					$form.append($('<span class=subtle> Saving&hellip;</span>'));
					$.post(action, $form.serialize())
						.success(function(data){ 
							$rateFeedback.replaceWith(data);
							decorateFeedbackForm();
						})
						.error(function(){ alert('Sorry, that didn\'t seem to work.'); });
				}
			});
		}
	};
	decorateFeedbackForm();
	$('#feedback-rating-container').each(function(){
		var $this = $(this);
		var action = $this.find('a').attr('href');
		$this.html('').load(action, function(){
			decorateFeedbackForm();
		})
	})
	
//	$('input.date-time-picker').AnyTime_picker({
//		format: "%e-%b-%Y %H:%i:%s",
//		firstDOW: 1
//	});
	
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
		
	$('a.copyable-url').copyable({prefixLinkText:true});
	
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
	
	(function(){
		
		var $collectSubmissions = $('input#collectSubmissions');
		var $options = $('#submission-options');
		$collectSubmissions.change(function(){
			if ($collectSubmissions.is(':checked')) $options.stop().slideDown('fast');
			else $options.stop().slideUp('fast');
		});
		$options.toggle($collectSubmissions.is(':checked'));
		
	})();
	
	
	
	$('.assignment-info .assignment-buttons').css('opacity',0);
	$('.assignment-info').hover(function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 1);
	}, function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 0); 
	});
	
	
	
	window.Courses = exports;
	
}); // end domready

}(jQuery));