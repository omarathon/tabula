(function ($) { "use strict";

	jQuery.fn.tabulaAjaxForm = function() {
		var $row = $(this);
		var $form = $row.find('form');
		prepareAjaxForm($form, $row);
	};

	$.fn.bindFormHelpers = function() {
		var $this = $(this);

		// bind form helpers
		$this.find('input.date-time-picker').tabulaDateTimePicker();
		$this.find('input.date-picker').tabulaDatePicker();
		$this.find('input.time-picker').tabulaTimePicker();
		$this.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
		$this.find('form.double-submit-protection').tabulaSubmitOnce();
		$this.tabulaAjaxForm();
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
				}));
			}
		});

		// extension managemet - on submit replace html with validation errors or redirect
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
	var prepareAjaxForm = function($form, $row, callbackOption) {
		var $container = $row.find('.detailrow-container');
		var callback = callbackOption || function(resp) {
				var $resp = $(resp);
				// there should be an ajax-response class somewhere in the response text
				var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
				var success = $response.length && $response.data('status') == 'success';

				if (success) {
					return "";
				} else {
					return resp;
				}
			};
		$form.ajaxForm({

			beforeSubmit: function showRequest(formData, jqForm, options) {
				// formData is an array; here we use $.param to convert it to a string to display it
				// but the form plugin does this for you automatically when it submits the data
				var queryString = $.param(formData);

				// jqForm is a jQuery object encapsulating the form element.  To access the
				// DOM element for the form do this:
				// var formElement = jqForm[0];
				alert('About to submit: \n\n' + queryString);

				// here we could return false to prevent the form from being submitted;
				// returning anything other than false will allow the form submit to continue
				return true;
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
					result = callback(response);
				}

				if (!result || /^\s*$/.test(result)) {
					// reset if empty
					$container.html("<p>No data is currently available. Please check that you are signed in.</p>");
					$row.data('loaded', false);
					$row.trigger('show.bs.collapse');
				} else {
					$container.html(result);
				}
			},
			error: function(){alert("There has been an error. Please reload and try again.");}
		});
	};


})(jQuery);