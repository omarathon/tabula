(function ($) { "use strict";
	var exports = {};

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
	 * Special case for downloading submissions as PDF.
	 * Does a check for non PDF files in submissions before fetching the download
	 */
	$(function(){
		$('a.download-pdf').on('click', function(e){
			var $this = $(this);
			e.preventDefault();
			if(!$this.hasClass("disabled") && $this.closest('.disabled').length === 0) {
				var $modal = $('#download-pdf-modal'), action = this.href;
				if ($this.data('href')) {
					action = $this.data('href')
				}
				var postData = $this.closest('div.form-post-container').find('.collection-checkbox:checked').map(function(){
					var $input = $(this);
					return $input.prop('name') + '=' + $input.val();
				}).get().join('&');
				$.post(action, postData, function(data){
					if (data.submissionsWithNonPDFs.length === 0) {
						// Use native click instead of trigger because there's no click handler for the marker version
						$modal.find('.btn.btn-primary').get(0).click();
					} else {
						$modal.find('.count').html(data.submissionsWithNonPDFs.length);
						var $submissionsContainer = $modal.find('.submissions').empty();
						$.each(data.submissionsWithNonPDFs, function(i, submission){
							var fileList = $('<ul/>');
							$.each(submission.nonPDFFiles, function(i, file){
								fileList.append($('<li/>').html(file));
							});
							$submissionsContainer.append($('<li/>').append(
								(submission.name.length > 0) ? submission.name + ' (' + submission.universityId + ')' : submission.universityId
							).append(fileList))
						});
						$modal.modal('show');
					}
				});
			}
		});
	});

    exports.initBigList = function ($scope) {
		$scope = $scope || $('body');

		var biglistOptions = {
			setup: function() {
				var $container = this, $outerContainer = $container.closest('div.form-post-container');
				// #delete-selected-button won't work for >1 set of checkboxes on a page.
				$('#download-selected-button, #delete-selected-button', $outerContainer).click(function(event){
					event.preventDefault();

					var $checkedBoxes = $(".collection-checkbox:checked", $container);
					if ($container.data('checked') !== 'none') {
						var $form = $('<form />').attr({method:'POST',action:this.href}).hide();
						$form.append($checkedBoxes.clone());
						$(document.body).append($form);
						$form.submit();
					}
					return false;
				});

				$('#mark-plagiarised-selected-button:not(.disabled)', $outerContainer).click(function(event){
					event.preventDefault();

					var $checkedBoxes = $(".collection-checkbox:checked", $container);

					if ($container.data('checked') !== 'none') {

						var $form = $('<form></form>').attr({method:'POST', action: this.href}).hide();
						$form.append($checkedBoxes.clone());

						if ($container.data("all-plagiarised") === true) {
							$form.append("<input type='hidden' name='markPlagiarised' value='false'>");
						}

						$(document.body).append($form);
						$form.submit();
					}
					return false;
				});

				$('.form-post', $outerContainer).click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						if ($this.data('href')) {
							action = $this.data('href')
						}

						var $form = $('<form />').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = false;

						if ($container.data('checked') !== 'none' || $this.closest('.must-have-selected').length === 0) {
							var $checkedBoxes = $(".collection-checkbox:checked", $container);
							$form.append($checkedBoxes.clone());

							var $extraInputs = $(".post-field", $container);
							$form.append($extraInputs.clone());

							doFormSubmit = true;
						}

						if ($this.hasClass('include-filter') && ($('.filter-form').length > 0)) {
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
					}
				});

				$('.form-post-single', $outerContainer).click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						if ($this.data('href')) {
							action = $this.data('href')
						}

						var $form = $('<form />').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = true;

						var $checkedBoxes = $(".collection-checkbox", $this.closest('itemContainer'));
						$form.append($checkedBoxes.clone().prop('checked', true));

						var $extraInputs = $(".post-field", $this.closest('itemContainer'));
						$form.append($extraInputs.clone());

						if (doFormSubmit) {
							$(document.body).append($form);
							$form.submit();
						} else {
							return false;
						}
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
					$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign icon-fixed-width"></i> Unmark plagiarised');
				} else {
					$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign icon-fixed-width"></i> Mark plagiarised');
				}
			}
		};

		$('.submission-feedback-list, .submission-list, .feedback-list, .marker-feedback-list, .coursework-progress-table, .expanding-table', $scope).not('.marker-feedback-table').bigList(
			$.extend(biglistOptions, {
				onSomeChecked : function() {
					$('.must-have-selected').removeClass('disabled');
				},

				onNoneChecked : function() {
					$('.must-have-selected').addClass('disabled');
				}
			})
		);
	};
	$(function () { exports.initBigList(); });

	exports.equalHeightTabContent = function ($scope) {
		$scope = $scope || $('body');
		$scope.find('.tab-content-equal-height').each(function () {
			var $container = $(this);
			var maxHeight = 0;
			$container.find('.tab-pane').each(function () {
				var $pane = $(this);
				if ($pane.is('.active')) {
					var height = $pane.height();
					maxHeight = (height > maxHeight) ? height : maxHeight;
				} else {
					$pane.addClass('active');
					var height = $pane.height();
					maxHeight = (height > maxHeight) ? height : maxHeight;
					$pane.removeClass('active');
				}
			});
			$container.find('.tab-pane').height(maxHeight);
		});
	};

	// take anything we've attached to "exports" and add it to the global "Courses"
	// we use extend() to add to any existing variable rather than clobber it
	window.Coursework = $.extend(window.Coursework, exports);
})(jQuery);