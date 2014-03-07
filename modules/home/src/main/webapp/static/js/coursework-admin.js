/**
 * Scripts used only by the coursework admin section.
 */
(function ($) { "use strict";

var exports = {};

exports.zebraStripeAssignments = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

// take anything we've attached to "exports" and add it to the global "Courses"
// we use extend() to add to any existing variable rather than clobber it
window.Courses = jQuery.extend(window.Courses, exports);

$(function(){

	// hide stuff that makes no sense when open-ended
	$('input#openEnded').slideMoreOptions($('.has-close-date'), false);
	$('input#modal-open-ended').slideMoreOptions($('.has-close-date'), false);

	// check that the extension UI elements are present
	if($('input#allowExtensionRequests').length > 0){
		$('input#allowExtensionRequests').slideMoreOptions($('#request-extension-fields'), true);
	}

	// Zebra striping on lists of modules/assignments
	$('.module-info').each(function(i, module) {
		exports.zebraStripeAssignments($(module));
	});

	$('.module-info.empty').css('opacity',0.66)
		.find('.module-info-contents').hide().end()
		.click(function(){
			$(this).css('opacity',1)
				.find('.module-info-contents').show().end();
		})
		.hide();

	$('.dept-show').click(function(event){
		event.preventDefault();
		var hideButton = $(this).find("a");

		$('.module-info.empty').toggle('fast', function() {
			if($('.module-info.empty').is(":visible")) {
				hideButton.html('<i class="icon-eye-close"></i> Hide');
				hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));

			} else {
				hideButton.html('<i class="icon-eye-open"></i> Show');
				hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
			}
		});

	});


	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	// code for the marks web forms
	$('#marks-web-form').tableForm({
		addButtonClass: 'add-additional-marks',
		headerClass: 'mark-header',
		rowClass: 'mark-row',
		tableClass: 'marksUploadTable',
		listVariable: 'marks',
		onAdd: function(){
			$('input.universityId', this).each(function(i, picker){
				initUserPicker(picker, true);
			});
		}
	});

	$('.show-archived-assignments').click(function(e){
		e.preventDefault();
		$(e.target).hide().closest('.module-info').find('.assignment-info.archived').show();
	});

	$('.hide-awaiting-submission').on('click', function(){
		$('.awaiting-submission, .hide-label, .show-label').toggle();
	});
	// hide this button if it would do nothing
	if ($('.awaiting-submission').length == 0) {
		$('.hide-awaiting-submission').hide();
	}

	// enable shift-click on multiple checkboxes in tables
	$('table').find('input[type="checkbox"]').shiftSelectable();

	$('.submission-feedback-list, .submission-list, .feedback-list, .marker-feedback-list, .coursework-progress-table, .expanding-table').bigList({
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

			$('#mark-plagiarised-selected-button:not(.disabled)').click(function(event){
				event.preventDefault();

				var $checkedBoxes = $(".collection-checkbox:checked", $container);

				if ($container.data('checked') != 'none') {

					var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
					$form.append($checkedBoxes.clone());

					if ($container.data("all-plagiarised") === true) {
						$form.append("<input type='hidden' name='markPlagiarised' value='false'>");
					}

					$(document.body).append($form);
					$form.submit();
				}
				return false;
			});

			$('.form-post').click(function(event){
				event.preventDefault();

				var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
				var doFormSubmit = false;

				if ($container.data('checked') != 'none') {
					var $checkedBoxes = $(".collection-checkbox:checked", $container);
					$form.append($checkedBoxes.clone());

					var $extraInputs = $(".post-field", $container);
					$form.append($extraInputs.clone());

					doFormSubmit = true;
				}

				if ($(this).hasClass('include-filter') && ($('.filter-form').length > 0)) {
						var $inputs = $(':input', '.filter-form:not("#floatedHeaderContainer *")');
						$form.append($inputs.clone());

						doFormSubmit = true;
				}

				if (doFormSubmit) {
					$(document.body).append($form);
				  $form.submit();
				} else {
					return false;
				}
			});

		},

		// rather than just toggling the class check the state of the checkbox to avoid silly errors
		onChange : function() {
			this.closest(".itemContainer").toggleClass("selected", this.is(":checked"));
			var $checkedBoxes = $(".collection-checkbox:checked");

			var allPlagiarised = false;

			if ($checkedBoxes.length > 0) {
				allPlagiarised = true;
				$checkedBoxes.each(function(index){
					var $checkBox = $(this);
					if ($checkBox.closest('tr').data('plagiarised') != true) {
						allPlagiarised = false;
					}
				});
			}
			$('.submission-feedback-list,.submission-list,.coursework-progress-table').data("all-plagiarised", allPlagiarised);
			if (allPlagiarised) {
				$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign"></i> Unmark plagiarised');
			}
			else {
				$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign"></i> Mark plagiarised');
			}
		},

		onSomeChecked : function() {
			$('.must-have-selected').removeClass('disabled');
		},

		onNoneChecked : function() {
			$('.must-have-selected').addClass('disabled');
		}

	});



});

// shift selectable checkboxes
$.fn.shiftSelectable = function() {
	var lastChecked,
		$boxes = this;

	$boxes.click(function(evt) {
		if(!lastChecked) {
			lastChecked = this;
			return;
		}

		if(evt.shiftKey) {
			var start = $boxes.index(this),
				end = $boxes.index(lastChecked);
			$boxes.slice(Math.min(start, end), Math.max(start, end) + 1)
				.attr('checked', lastChecked.checked)
				.trigger('change');
		}

		lastChecked = this;
	});
};

// code for markingWorkflow add/edit
$(function(){
	var markingMethod = $('#markingMethod');
	var secondMarkers = $('.second-markers-container');
	if($('option:selected', markingMethod).hasClass('uses-second-markers'))
		secondMarkers.show();
	$('#markingMethod').on('change', function(){
		var option = $('option:selected', $(this));


		if(option.hasClass('uses-second-markers'))
			secondMarkers.show();
		else
			secondMarkers.hide();
	});
});

// code for bulk archive/copy assignments
$(function(){

	$('.copy-assignments, .archive-assignments').bigList({

		setup: function(e){
			if(!$(".collection-checkbox").is(":checked")){
				$('.btn-primary').prop('disabled', 'disabled');
			}
		},

		onSomeChecked: function() {
			$('.btn-primary').removeProp('disabled');
		},

		onNoneChecked: function() {
			$('.btn-primary').prop('disabled', 'disabled');
		}
	});

	$('form.copy-assignments').confirmModal({
		message: "Are you sure that you want to create these assignments?"
	});

	$('form.archive-assignments').confirmModal({
		message: "Are you sure that you want to archive these assignments?"
	});

});



var frameLoad = function(frame){
	if(jQuery(frame).contents().find("form").length == 0){
		jQuery("#feedback-report-modal").modal('hide');
		document.location.reload(true);
	}
}

$(function(){




// Ajax specific modal start

// modals use ajax to retrieve their contents
	$('#feedback-report-button').on('click', 'a[data-toggle=modal]', function(e){
		e.preventDefault();
		var $this = $(this);
		var target = $this.attr('data-target');
		var url = $this.attr('href');
		$(target).load(url);
	});



// any date fields returned by ajax will have datetime pickers bound to them as required
	$('#feedback-report-modal').on('focus', 'input.date-time-picker', function(e){
		e.preventDefault();
		var isPickerHidden = (typeof $('.datetimepicker').filter(':visible')[0] === "undefined") ? true : false;

		if(isPickerHidden) {
			$(this).datetimepicker('remove').datetimepicker({
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
		}
	});

	// feedback report
	$("#feedback-report-modal").tabulaAjaxSubmit(function(data) {
		window.location = data.result;
	});

	// makes dropdown menus dropup rather than down if they're so
	// close to the end of the screen that they will drop off it
	var bodyHeight = $('body').height();
	$('.module-info:not(.empty) .btn-group').each( function(index) {
		if(($(this).find('.dropdown-menu').height() +  $(this).offset().top) > bodyHeight) {
			$(this).addClass("dropup");
		}
	});

});

/**
 * Scripts for expanding tables, including online marking
 */
$(function() {
	// this has to descend from #main-content, as the content container is appended there, outside the table
	$('#main-content').on('tabula.expandingTable.contentChanged', '.content-container', function(e) {
		var $container = $(this);
		var $form = $container.find('form');
		var contentId = $container.attr('data-contentid');
		var $row = $('tr.itemContainer[data-contentid='+contentId+']');

		$form.find('.date-time-picker').tabulaDateTimePicker();
        $form.tabulaSubmitOnce();

		// record the initial values of the fields
		$('input, textarea', $form).each(function() {
			$(this).data('initialvalue', $(this).val());
		});

		// only relevant for marker_moderation.ftl:
		// rejection fields shown when reject is selected
		$('input.reject', $container).each(function() {
			var $this = $(this);
			var $form = $this.closest('form');
			var $table = $('.expanding-table');
			var $rejectionFields = $form.find('.rejection-fields');
			$this.slideMoreOptions($rejectionFields, true);

			$rejectionFields.on('tabula.slideMoreOptions.shown', function() {
				$table.trigger('tabula.expandingTable.repositionContent');
			});

			$rejectionFields.on('tabula.slideMoreOptions.hidden', function() {
				$table.trigger('tabula.expandingTable.repositionContent');
			});
		});

		$('.discard-changes', $form).on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			if(hasChanges($container)) {
				var message = 'Discarding unsaved changes is irreversible. Are you sure?';
				var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
									"<div class='modal-body'>" +
										"<h5>"+message+"</h5>" +
									"</div>" +
									"<div class='modal-footer'>" +
										"<a class='confirm btn'>Yes, discard</a>" +
										"<a data-dismiss='modal' class='btn btn-primary'>No, go back to editing</a>" +
									"</div>" +
								"</div>"
				var $modal = $(modalHtml);
				$modal.modal();
				$('a.confirm', $modal).on('click', function(){
					resetFormValues($form, $row);
					$modal.modal('hide');
				});
			} else {
				resetFormValues($form, $row);
			}
		});

		/**
		 * Helper for ajaxified forms in tabula expanding tables
		 *
		 * This glues together the ajaxForm and expandingTable plugins. It adds boilerplate for coping with
		 * signed-out users, and provides a callback for giving UI feedback after the form has been processed.
		 *
		 * callback() is passed the raw response text.
		 * It MUST return an empty string if the form has been successfully handled.
		 * Otherwise, whatever is returned will be rendered in the container.
 		 */
		var prepareAjaxForm = function($form, callback) {
			$form.ajaxForm({
				iframe: true,
				statusCode: {
					403: function(jqXHR) {
						$container.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
						$container.trigger('tabula.expandingTable.contentChanged');
					}
				},
				success: function(response) {
					var result;

					if (response.indexOf('id="dev"') >= 0) {
						// for debugging freemarker...
						result = $(response).find('#column-1-content');
					} else {
						result = callback(response);
					}

					if (!result || /^\s*$/.test(result)) {
						// reset if empty
						$row.trigger('tabula.expandingTable.toggle');
						$container.html("<p>No data is currently available.</p>");
						$row.find('.status-col dt .unsaved').remove();
						$container.removeData('loaded');
					} else {
						$container.html(result);
						$container.trigger('tabula.expandingTable.contentChanged');
					}
				},
				error: function(){alert("There has been an error. Please reload and try again.");}
			});
		}

		if ($form.parents('.online-feedback').length) prepareAjaxForm($form, function(resp) {
			var $resp = $(resp);

			// there should be an ajax-response class somewhere in the response text
			var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
			var success = $response.length && $response.data('status') == 'success';

			if (success) {
				var rejected = $response.data('data') == 'Rejected';
				var markingCompleted = $response.data('data') == 'MarkingCompleted';
				var $statusContainer = $row.find('.status-col dt');

				if(rejected) {
					$statusContainer.append($('<div class="label label-important">Rejected</div>'));
				}
				else if(markingCompleted) {
					$statusContainer.append($('<div class="label label-success">Marking completed</div>'));
				}
				else if(!$statusContainer.find('.marked').length) {
					$statusContainer.append($('<div class="label label-warning marked">Marked</div>'));
				}

				$row.next('tr').trigger('tabula.expandingTable.toggle');

				return "";
			} else {
				return resp;
			}
		});

		if ($form.parents('.extension-detail').length) prepareAjaxForm($form, function(resp) {
			// FIXME gotta do some handling...
			var $resp = $(resp);

			// there should be an ajax-response class somewhere in the response text
			var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
			var success = $response.length && $response.data('status') == 'success';

			if (success) {
				var rejected = $response.data('data').status == 'Rejected';
				var approved = $response.data('data').status == 'Approved';
				var $statusContainer = $row.find('.status-col dt');

				$statusContainer.find(".label").remove();

				if(rejected) {
					$statusContainer.append($('<div class="label label-important">Rejected</div>'));
				}
				else if(approved) {
					$statusContainer.append($('<div class="label label-info">Approved</div>'));
				}

				return "";
			} else {
				return resp;
			}
		});
	});

	$('#main-content').on('tabula.expandingTable.parentRowCollapsed', '.content-container', function(e) {
		var $this = $(this);
		var contentId = $this.attr('data-contentid');
		var $row = $('tr.itemContainer[data-contentid='+contentId+']');
		var $statusContainer = $row.find('.status-col dt');
		if(hasChanges($this)) {
			if(!$statusContainer.find('.unsaved').length){
				$statusContainer.append($('<div class="label label-important unsaved">Unsaved changes</div>'));
			}
		} else {
			$statusContainer.find('.unsaved').remove();
		}
	});

	$('#main-content').on("click", ".remove-attachment", function(e) {
		var $this = $(this);
		var $form = $this.closest('form');
		var $li = $this.closest("li");
		$li.find('input, a').remove();
		$li.find('span').before('<i class="icon-remove"></i>&nbsp;').wrap('<del />');
		var $ul = $li.closest('ul');

		if (!$ul.next().is('.alert')) {
			var alertMarkup = '<p class="alert pending-removal"><i class="icon-lightbulb"></i> Files marked for removal won\'t be deleted until you <samp>Save</samp>.</p>';
			$ul.after(alertMarkup);
		}

		if($('input[name=attachedFiles]').length == 0){
			var $blankInput = $('<input name="attachedFiles" type="hidden" />');
			$form.append($blankInput);
		}
		return false;
	});

	function resetFormValues($form, $row) {
		// reset all the data for this row
		$('input, textarea', $form).each(function() {
			$(this).val($(this).data('initialvalue'));
		});

		// remove unsaved badges
		$row.find('.unsaved').remove();

		// collapse the row
		$row.trigger('tabula.expandingTable.toggle');
	}

	function hasChanges($container) {
		return $container.data('loaded') &&
			($container.find('.pending-removal').length > 0 || inputsHaveChanges($container));
	}

	function inputsHaveChanges($container) {
		var modifiedField = false;
		var $inputs = $container.find(':input');
		$inputs.each(function() {
			modifiedField = $(this).val() != $(this).data("initialvalue");
			return !modifiedField; // false breaks from loop
		});
		return modifiedField;
	}

	// just for generic feedback
	$(".edit-generic-feedback").on('click', function() {
		var $icon = $(this).find('i');
		$icon.toggleClass('icon-chevron-right')
			 .toggleClass('icon-chevron-down');
		var $container = $('.edit-generic-feedback-container');
		if($container.is(':visible')){
			$icon.addClass('icon-chevron-right')
			$icon.removeClass('icon-chevron-down');
			$container.hide();
		} else {
			if($container.find('form').length) {
				$icon.removeClass('icon-chevron-right')
				$icon.addClass('icon-chevron-down');
				$container.show();
			} else {
				$icon.removeClass('icon-chevron-right');
				$icon.addClass('icon-spinner icon-spin');
				$container.load('generic', function(){
					$icon.removeClass('icon-spinner icon-spin');
					$icon.addClass('icon-chevron-down');
					$(this).show();
                    $(".expanding-table").trigger('tabula.expandingTable.repositionContent');
				});
			}
		}
        $(".expanding-table").trigger('tabula.expandingTable.repositionContent');
	});

	$(".generic-feedback").on('click', 'input[type=submit]', function(e){
		e.preventDefault();
		$('.before-save').hide();
		$('.saving').show();
		var $form = $(e.target).closest('form');
		$.ajax({
			type: "POST",
			url: $form.attr('action'),
			data: $form.serialize(),
			success: function( response ) {
				$('.saving').hide();
				$('.saved').show();
			}
		});
	});
});

}(jQuery));


