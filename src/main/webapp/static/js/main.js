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
    var $container = $('<span class=copyable-url-container></span>').attr('title',title);
    var $explanation = $('<span class=press-ctrl-c></span>').html(PressCtrlC).hide();
    var $input = $('<input class="copyable-url" rel="tooltip"></span>')
        .attr('readonly', true)
        .attr('value',url)
        .click(function(){
          this.select();
          $explanation.slideDown('fast');
        }).blur(function(){
          $explanation.fadeOut('fast');
        });
    $container.append($input).append($explanation);
    $this.after($container).remove();
    if (prefixLinkText) {
    	$container.before(text);
    }
    if (preselect) {
    	$input.click();
    }
  });
  
  return this;
}

/*
 * Do the given function only if it matched any elements.
 * "this" refers to the jQuery object inside the callback.
 */
jQuery.fn.use = function(callback) {
  if (this.length > 0) callback.call(this);
  return this;
}

/*
	Provides functionality to lists/tables that have checkboxes,
	so that you can select some/all rows and do stuff with them.

	Options:

		onSomeChecked($container): callback when at least 1 is checked.
		onNoneChecked($container): callback when no rows are checked.
		onAllChecked($container): callback when all rows selected. defaults to behaviour of onSomeChecked.
		onChange($checkbox): callback when a row is selected/unselected.
*/
jQuery.fn.bigList = function(options) {
	var $ = jQuery;
	this.each(function(){
		var $this = $(this);
		
		var checkboxClass = options.checkboxClass || 'collection-checkbox';
		var checkboxAllClass = options.checkboxClass || 'collection-check-all';
		
		var $checkboxes = $this.find('input.'+checkboxClass);
		var $selectAll = $this.find('input.'+checkboxAllClass);
		
		var doNothing = function(){};

		var onChange = options.onChange || doNothing;
		var onSomeChecked = options.onSomeChecked || doNothing;
		var onNoneChecked = options.onNoneChecked || doNothing;
		var onAllChecked = options.onAllChecked || onSomeChecked;

		$checkboxes.change(function(){
			onChange.call(jQuery(this)); // pass the checkbox as the context
			var allChecked = $checkboxes.not(":checked").length == 0;
			$selectAll.attr("checked", allChecked);
			if (allChecked) {
				$this.data('checked','all');
				onAllChecked.call($this);
			} else if ($checkboxes.is(":checked")) {
				$this.data('checked','some');
				onSomeChecked.call($this);
			} else {
				$this.data('checked','none');
				onNoneChecked.call($this);
			}
		});

		$selectAll.change(function(){
			$checkboxes.attr("checked", this.checked);
			$checkboxes.each(function(){
				onChange.call(jQuery(this));
			});
			if (this.checked) {
				$this.data('checked','all');
				onAllChecked.call($this);
			} else {
				$this.data('checked','none');
				onNoneChecked.call($this);
			}
		});
		
		options.setup.call($this);
		
		$(function(){
			$checkboxes.change();
		});
		
		// Returns an array of IDs.
		var getCheckedFeedbacks = function() {
			return $checkboxes.filter(":checked").map(function(i,input){ return input.value; });
		};
	});
	return this;
}

jQuery.fn.tableForm = function(options) {
	var $ = jQuery;
	this.each(function(){
		// this is the form
		var $this = $(this);

        var doNothing = function(){};

		var addButtonClass = options.addButtonClass || 'add-button';
		var headerClass = options.headerClass || 'header-row';
		var rowClass = options.headerRow || 'header-row';
		var listVariable = options.listVariable || 'items';
		// should nearly always provide rowMarkup as the default does nothing.
		var rowMarkup = options.rowMarkup || '<tr class='+rowClass+'><td></td></tr>';
        var onAdd = options.onAdd || doNothing;

        var $table = $this.find('table');
		var $addButton = $this.find('.'+addButtonClass);
		var $header = $this.find('tr.'+headerClass);
		var $rows = $this.find('tr.'+rowClass);

		if($rows.size() === 0){
			$header.hide();
		}

		$addButton.on('click', function(e){
			e.preventDefault();
			$header.show();
			var newIndex = $rows.size;
			var newRow = $(rowMarkup);
			 // add items[index]. to the input names in the new row
			$("input", newRow).each(function(){
				var name = $(this).attr("name");
				$(this).attr("name", listVariable+"["+newIndex+"]."+name)
			});
			$table.append(newRow);
			onAdd.call(newRow);
		});

		options.setup.call($this);

	});
	return this;
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
					$lastname	= $contents.find('.userpicker-lastname'),
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
			$form.find('.rating-question').button().each(function(){
				var $question = $(this);
				var $group = $('<div>').attr({'class':'btn-group',"data-toggle":"buttons-radio"});
				var $radios = $question.find('input[type=radio]');
				var $unsetter = $question.find('input[type=checkbox]');
				$radios.each(function(){
					var radio = this;
					var text = $(radio).parent('label').text();
					var $button = $('<a>').attr({'class':'btn'}).html(text);
					if (radio.checked) $button.addClass('active');
					$button.click(function(ev){
						radio.checked = true;
						$unsetter.attr('checked',false);
					});
					$group.append($button);
				});
				$question.find('label').hide();
				$question.append($group);
			});
			$form.on('submit', function(event){
				$form.find('input[type=submit]').button('loading');
				event.preventDefault();
				$.post(action, $form.serialize())
					.success(function(data){
						if (data.indexOf('id="feedback-rating"') != -1) {
							$rateFeedback.replaceWith(data);
							decorateFeedbackForm();
						} else { // returned some other HTML - error page or login page?
							alert('Sorry, there was a problem saving the rating.');
							$form.find('input[type=submit]').button('reset');
						}
					})
					.error(function(){ alert('Sorry, that didn\'t seem to work.'); });
				return false;
			});
		}
	};
	decorateFeedbackForm();
	$('#feedback-rating-container').each(function(){
		var $this = $(this);
		var action = $this.find('a').attr('href');
		$this.html('').load(action, function(){
			decorateFeedbackForm();
		});
	});
	
	$('input.date-time-picker').AnyTime_picker({
		format: "%e-%b-%Y %H:%i:%s",
		firstDOW: 1
	});
	
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
	
	$('input.user-code-picker').each(function (i, picker) {
		var $picker = $(picker),
			$button = $('<a href="#" class="btn">Search for user</a>'),
			userPicker = getUserPicker(); // could be even lazier and init on click
		$picker.after("&nbsp;");
		$picker.after($button);
		$button.click(function(event){
			event.preventDefault();
			userPicker.showPicker(picker, picker);
		});
	});
	
	var ajaxPopup = new WPopupBox();
	
	/**
	 * Enhances links with "ajax-popup" class by turning them magically
	 * into AJAX-driven popup dialogs, without needing much special magic
	 * on the link itself.
	 * 
	 * "data-popup-target" attribute can be a CSS selector for a parent
	 * element for the popup to point at.
	 */
	$('a.ajax-popup').click(function(e){
		var link = e.target, 
			$link = $(link),
			popup = ajaxPopup,
			width = 300,
			height = 300,
			pointAt = link;
		
		if ($link.data('popup-target') != null) {
			pointAt = $link.closest($link.data('popup-target'));
		}
		
		var decorate = function($root) {
			if ($root.find('html').length > 0) {
				// dragged in a whole document from somewhere, whoops. Replace it
				// rather than break the page layout
				popup.setContent("<p>Sorry, there was a problem loading this popup.</p>");
			}
			$root.find('.cancel-link').click(function(e){
				e.preventDefault();
				popup.hide();
			});
			$root.find('input[type=submit]').click(function(e){
				e.preventDefault();
				var $form = $(this).closest('form');
				jQuery.post($form.attr('action'), $form.serialize(), function(data){
					popup.setContent(data);
					decorate($(popup.contentElement));
				});
			});
			// If page returns hint that we're done, close the popup.
			if ($root.find('.ajax-response').data('status') == 'success') {
				//popup.hide();
				// For now, reload the page. Might extend it to allow selective reloading
				// of just the thing that's changed.
				window.location.reload();
			}
		};
		
		e.preventDefault();
		popup.setContent("Loading&hellip;");
		popup.setSize(width, height);
		$.get(link.href, function (data) {
			popup.setContent(data);
			var $contentElement = $(popup.contentElement);
			decorate($contentElement);
			popup.setSize(width, height);
			popup.show();
			popup.increaseHeightToFit();
			popup.positionLeft(pointAt);
		}, 'html');
	});
	
	$('.show-archived-assignments').click(function(e){
		e.preventDefault();
		$(e.target).hide().closest('.module-info').find('.assignment-info.archived').show();
	})
	
	/*
	var $userCodePickers = $('input.user-code-picker'); 
	$userCodePickers.autocomplete({
		minLength:2,
	    source: function(request, response) {
	    	$.ajax({
	    		url: "/api/userpicker/query.json",
	    		dataType: 'json',
	    		data: { query: request.term },
	    		success: function(data) {
	    			response( data );
	    		}
	    	})
	    },
	    focus: function(event,ui){
	        this.value = ui.item.id;
	        return false;
	    },
	    select: function(event,ui){
	        this.value = ui.item.id;
	        return false;
	    }
	}).data( "autocomplete" )._renderItem = function( ul, item ) {
	    return $( "<li></li>" )
        .data( "item.autocomplete", item )
        .append( "<a>" + item.label + " (" + item.dept + ")</a>" )
        .appendTo( ul );
	};
	*/
	
	$('input.uni-id-picker').each(function (picker) {
		
	});
	
	
	$('.submission-list, .feedback-list').bigList({
		
		setup : function() {
			var $container = this;
			// #delete-selected-button won't work for >1 set of checkboxes on a page.
			$('#download-selected-button, #delete-selected-button').click(function(event){
				event.preventDefault();

				var $checkedBoxes = $(".collection-checkbox:checked", $container);
				if ($container.data('checked') != 'none') {
					var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
					$form.append($checkedBoxes.clone());
					$(document.body).append($form);
					$form.submit();
				}
				return false;
			});
		},

		// rather than just toggling the class check the state of the checkbox to avoid silly errors
		onChange : function() {
			this.closest(".itemContainer").toggleClass("selected", this.is(":checked"));
		},

		onSomeChecked : function() {
			$('#delete-feedback-button, #delete-selected-button, #download-selected-button').removeClass('disabled');
		},
		
		onNoneChecked : function() {
			$('#delete-feedback-button, #delete-selected-button, #download-selected-button').addClass('disabled');
		}
		
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
		
	$('a.copyable-url').copyable({prefixLinkText:true}).tooltip();
	
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
	
	var slideMoreOptions = function($checkbox, $slidingDiv) {
		$checkbox.change(function(){
			if ($checkbox.is(':checked')) $slidingDiv.stop().slideDown('fast');
			else $slidingDiv.stop().slideUp('fast');
		});
		$slidingDiv.toggle($checkbox.is(':checked'));
	};
	
	
	slideMoreOptions($('input#collectSubmissions'), $('#submission-options'));
	
	
	$('.assignment-info .assignment-buttons').css('opacity',0);
	$('.assignment-info').hover(function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 1);
	}, function() { 
		$(this).find('.assignment-buttons').stop().fadeTo('fast', 0); 
	});
	
	$('.module-info.empty').css('opacity',0.66)
		.find('.module-info-contents').hide().end()
		.find('h2').prepend($('<small>Click to expand</small>')).end()
		.click(function(){ 	
			$(this).css('opacity',1)
				.find('h2 small').remove().end()
				.find('.module-info-contents').show().end();	
		})
		.hide()
		.first().before(
			$('<p>').html('Modules with no assignments are hidden. ').append(
				$('<a>').addClass('btn btn-success').attr('href','#').html("Show all modules").click(function(event){
					event.preventDefault();
					$(this.parentNode).remove();
					$('.module-info.empty').show();
				})
			)
		);
	
	window.Courses = exports;
	
	// code for the marks tabs and marks web form
	
	if($(".mark-row").size() === 0){
		$(".mark-header").hide();
	}

	$('#marks-tabs a').click(function (e) {
			e.preventDefault();
			$(this).tab('show');
		});
		
		var rowMarkup = '<tr class="mark-row"><td><input name="universityId" type="text" /></td><td><input name="actualMark" type="text" /></td><td><input name="actualGrade" type="text" /></td></tr>';
		
		$('#add-additional-marks').on('click', function(e){
			e.preventDefault();
			$(".mark-header").show(); //show if this was hidden because the table started out empty
			var newIndex = $(".mark-row").size();
			var newRow = $(rowMarkup);
			// add marks[index]. to the input names in the new row
			$("input", newRow).each(function(){
				var name = $(this).attr("name");
				$(this).attr("name", "marks["+newIndex+"]."+name)
			});
			$('#marks-web-form').append(newRow);
		});
	
}); // end domready

}(jQuery));