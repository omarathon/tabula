(function ($) { "use strict";
	jQuery.fn.tabulaAjaxForm = function(beforeSubmit, callback, typeForm) {
		var $row = $(this);
		var $form = $row.find('form.ajax-form');
		prepareAjaxForm($form, $row, beforeSubmit, callback, typeForm);
	};
	$.fn.bindFormHelpers = function() {
		var $this = $(this);
		// bind form helpers
		$this.find('input.date-time-picker').tabulaDateTimePicker();
		$this.find('input.date-picker').tabulaDatePicker();
		$this.find('input.time-picker').tabulaTimePicker();
		$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
		$this.find('form.double-submit-protection').tabulaSubmitOnce();
	};
	$(function(){
		var $body = $('body');
		// fixed header and footer
		$body.find('.fix-area').fixHeaderFooter();
		// sortable tables
		$body.find('.table-sortable').sortableTable();
		// expandable table rows
		$body.on('tabula.expandingTable.contentChanged',function(e){
			var $this = $(e.target);
			$this.bindFormHelpers();
			$this.trigger('tabula.formLoaded');
		});
		$body.on('show.bs.collapse', '.detail-row', function(e){
			var $this = $(e.target);
			var hasLoaded = $this.data('loaded') || false;
			if(!hasLoaded) {
				var detailUrl = $this.data('detailurl');
				$this.data('request', $.get(detailUrl, function(data) {
					$this.find('td').html(data);
					// bind form helpers
					$this.bindFormHelpers();
					$this.data('loaded', true);
					$this.trigger('tabula.formLoaded');
				}));
			}
		});
		$('#feedback-adjustment').on('tabula.formLoaded', '.detail-row', function(e){

			var $content = $(e.target);

			// activate any popovers
			$content.find('.use-popover').popover();

			// bind suggested mark button
			$content.find('.use-suggested-mark').on('click', function(e){
				var $target = $(this);
				var $markInput = $content.find('input[name=adjustedMark]');
				var $commentsTextarea = $content.find('textarea[name=comments]');
				var mark = $content.find('button').attr("data-mark");
				var comment =$content.find('button').attr("data-comment");
				$markInput.val(mark);
				$commentsTextarea.val(comment);
				// simulate a keyup to trigger and grade validation
				$markInput.keyup();
				e.preventDefault();
			});

			var $select = $content.find('select[name=reason]');
			var $otherInput = $content.find('.other-input');
			$otherInput.addClass("hide");
			if($otherInput.val() == "Late submission penalty"){
				$content.find('option[value=Late submission penalty]').prop('selected', true);
			}
			if($otherInput.val() == "Plagarism penalty") {
				$content.find('option[value=Plagarism penalty]').prop('selected', true);
			}
			if (($otherInput.val() != "Plagarism penalty") && ($otherInput.val() != "Late submission penalty") && ($otherInput.val() != "") ){
				$content.find('option[value=Other]').prop('selected', true);
				$otherInput.removeAttr("disabled");
				$otherInput.removeClass("hide");
			}
			// show the suggested mark button if late penalty is selected
			var $suggestedPenalty = $select.closest('form').find('.late-penalty');
			if($select.val() === "Late submission penalty") {
				$suggestedPenalty.removeClass("hide");
			} else {
				$suggestedPenalty.addClass("hide");
			}
		});

		$body.on('change', 'select[name=reason]', function(e){
			var $target = $(e.target);
			var $otherInput = $target.siblings('.other-input');
			if ($target.find('option:selected').text() === "Other") {
				$otherInput.removeAttr("disabled");
				$otherInput.removeClass("hide");
				$otherInput.fadeIn(400);
			} else if ($otherInput.is(':visible')){
				$otherInput.fadeOut(400, function() {
					$otherInput.prop('disabled', true);
					$otherInput.addClass("hide");
				});
			}
			var $suggestedPenalty = $target.closest('form').find('.late-penalty');
			if ($target.val() === "Late submission penalty") {
				$suggestedPenalty.fadeIn(400);
				$suggestedPenalty.removeClass("hide");
			} else if ($suggestedPenalty.is(':visible')) {
				$suggestedPenalty.fadeOut(400);
				$suggestedPenalty.addClass("hide");
			}
		});
		// extension management - on submit replace html with validation errors or redirect
		$body.on('submit', 'form.modify-extension', function(e){
			e.preventDefault();
			var $form = $(e.target);
			var $detailRow = $form.closest('.detailrow-container');
			var formData = $form.serializeArray();
			var $buttonClicked =  $(document.activeElement);
			formData.push({ name: $buttonClicked.attr('name'), value: $buttonClicked.val() });
			$.post($form.attr('action'), formData, function(data) {
				if (data.success) {
					window.location.replace(data.redirect);
				} else {
					$detailRow.html(data);
					var $form = $detailRow.find('form');
					prepareAjaxForm($form, $detailRow);
					$detailRow.bindFormHelpers();
				}
			});
		});
		$('table.expanding-row-pairs').each(function(){
			$(this).find('tbody tr').each(function(i){
				if (i % 2 === 0) {
					var $selectRow = $(this), $expandRow = $selectRow.next('tr');
					$selectRow.data('expandRow', $expandRow.remove()).find('td:first').addClass('can-expand').prepend(
						$('<i/>').addClass('fa fa-fw fa-caret-right')
					);
				}
			}).end().on('click', 'td.can-expand', function(){
				var $row = $(this).closest('tr');
				if ($row.is('.expanded')) {
					$row.removeClass('expanded').next('tr').remove().end()
						.find('td i.fa-caret-down').removeClass('fa-caret-down').addClass('fa-caret-right');
				} else {
					$row.addClass('expanded').after($row.data('expandRow'))
						.find('td i.fa-caret-right').removeClass('fa-caret-right').addClass('fa-caret-down');
					//Tooltip invocation for row expansion to trigger tooltip. Invoking at top level does not trigger tooltips.
					$('.use-tooltip').tooltip();
				}
			}).find('tr.expand td.can-expand').trigger('click');
		});
		// code for tabs
		$('.nav.nav-tabs a').click(function (e) {
			e.preventDefault();
			$(this).tab('show');
		});
		$('input#openEnded').change(function(){
			var $this = $(this);
			if ($this.is(':checked'))  {
				$('#open-reminder-dt').removeAttr("disabled");
				$('#close-dt').attr("disabled", "disabled");
			}  else {
				$('#close-dt').removeAttr("disabled");
				$('#open-reminder-dt').attr("disabled","disabled");
			}
		});
		// check that the extension UI elements are present
		if($('input#allowExtensionRequests').length > 0){
			$('input#allowExtensionRequests').slideMoreOptions($('#request-extension-fields'), true);
		}
		var $assignmentpicker = $('.assignment-picker-input');
		var $assignmentQuery = $assignmentpicker.find('input[name=query]').attr('autocomplete', 'off');
		var target = $assignmentpicker.attr('data-target');
		$assignmentQuery.bootstrap3Typeahead({
			source: function(query, process) {
				if (self.currentSearch) {
					self.currentSearch.abort();
					self.currentSearch = null;
				}
				query = $.trim(query);
				self.currentSearch = $.ajax({
					url: $('.assignment-picker-input').attr('data-target'),
					data: {query: query},
					success: function(data) {
						var assignments = [];
						$.each(data, function(i, assignment) {
							var item = assignment.name + '|' + assignment.moduleCode + '|' + assignment.id;
							assignments.push(item);
						});
						process(assignments);
					}
				})
			},
			matcher: function(item) {
				return true;
			},
			sorter: function(items) {
				return items;
			}, // use 'as-returned' sort
			highlighter: function(item) {
				var assignment = item.split('|');
				return '<div class="description">' + assignment[0] + '-' + assignment[1] + '</div>';
			},
			updater: function(item) {
				var assignment = item.split('|');
				var assignmentId = assignment[2];
				$("#prefillAssignment").val(assignmentId);
				$assignmentpicker.val(assignmentId);
				$('#action-submit').val('');
				$("#command").submit();
				return assignment[0] + '-' + assignment[1];
			}
		});
	});
	var $assessmentComponentTable = $(".assessment-component table");
	$assessmentComponentTable.tablesorter({
		headers: {0:{sorter:false}}
	});
	// modals use ajax to retrieve their contents
	$(function() {
		$('body').on('submit', 'a[data-toggle=modal]', function(e){
			e.preventDefault();
			var $this = $(this);
			var $target = $($this.data('target'));
			var url = $this.attr('href');
			$target.load(url, function(){
				$target.bindFormHelpers()
			});
		});
		$("#modal-container").on("submit","input[type='submit']", function(e){
			e.preventDefault();
			var $this = $(this);
			var $form = $this.closest("form").trigger('tabula.ajaxSubmit');
			$form.removeClass('dirty');
			var updateTargetId = $this.data("update-target");
			var randomNumber = Math.floor(Math.random() * 10000000);
			jQuery.post($form.attr('action') + "?rand=" + randomNumber, $form.serialize(), function(data){
				window.location = data.result;
			});
		});
	});
	// code for bulk copy assignments
	$(function(){
		$('.copy-assignments').bigList({
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
	});
	/**
	 * Helper for ajaxified forms in tabula expanding tables
	 *
	 * This glues together the ajaxForm and bootstrap collapsable plugins. It adds boilerplate for coping with
	 * signed-out users, and provides a callback for giving UI feedback after the form has been processed.
	 *
	 * callback() is passed the raw response text.
	 * It MUST return an empty string if the form has been successfully handled.
	 * Otherwise, whatever is returned will be rendered in the container.
	 */
	var prepareAjaxForm = function($form, $row, beforeSubmitOption, callbackOption, typeOption) {
		var $container = $row.find('.detailrow-container');
		var callback = callbackOption || function(){};
		var beforeSubmit = beforeSubmitOption || function() { return true; };
		var formType = typeOption || "";
		$form.ajaxForm({
			beforeSerialize: beforeSubmit,
			beforeSubmit: function() {
				if(formType === 'feedback-adjustment') {
					if($("#action").val() != "suggestmark") {
						return true;
					}else{
						var $detailrow = $form.closest('.detail-row');
						$detailrow.find('.btn-primary').removeClass('disabled');
						$detailrow.find('.discard-changes').removeClass('disabled');
						$form.unbind('submit');
						$form.removeData('submitOnceSubmitted');
						$detailrow.trigger('tabula.formLoaded');
						return false;
					}
				}else{
					return true;
				}
			},
			iframe: true,
			statusCode: {
				403: function(jqXHR) {
					$container.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
				}
			},
			success: function(response) {
				var result;

				if (response.indexOf('id="dev"') >= 0) {
					// for debugging freemarker...
					result = $(response).find('#column-1-content');
				} else {
					var $resp = $(response);
					// there should be an ajax-response class somewhere in the response text
					var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
					var success = $response.length && $response.data('status') == 'success';
					if (success) {
						result =  "";
					} else {
						result =  response;
					}
				}
				if (!result || /^\s*$/.test(result)) {
					var $detailrow = $container.closest('.detail-row');
					// reset if empty
					$detailrow.collapse("hide");
					$detailrow.data('loaded', false);
					$container.html("<p>No data is currently available. Please check that you are signed in.</p>");
				} else {
					$container.html(result);
					callback($container);
				}
			},
			error: function(){ alert("There has been an error. Please reload and try again."); }
		});
	};


})(jQuery);
