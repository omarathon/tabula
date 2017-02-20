/**
 * Scripts used only by the exams admin section.
 */
(function ($) { "use strict";

var examsExports = {};

examsExports.zebraStripeExams = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

examsExports.prepareAjaxForm = function($form, callback) {
	var $container = $form.closest('.content-container');
	var contentId = $container.attr('data-contentid');
	var $row = $('tr.item-container[data-contentid='+contentId+']');
	$form.ajaxForm({
		iframe: true,
		statusCode: {
			403: function(jqXHR) {
				$container.html("<p>Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p>Refresh the page and try again. If it remains a problem, please let us know using the 'Problems, questions?' button at the top of the page.</p>");
				$container.trigger('tabula.expandingTable.contentChanged');
			}
		},
		success: function(response) {
			var result = callback(response);

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
};

window.Exams = jQuery.extend(window.Exams, examsExports);

/**
* Scripts used only by the exam grids section.
*/
var examGridsExports = {};

examGridsExports.manageNormalLoads = function(){
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		$('input[name=showAdditionalYears]').on('change', function(){
			var $this = $(this);
			if ($this.is(':checked')) {
				$('.normal-load-editor')
					.find('.row > .col-md-5').removeClass('col-md-5').addClass('col-md-2').end()
					.find('.row > .col-md-offset-5').removeClass('col-md-offset-5').addClass('col-md-offset-2').end()
					.find('.row > .col-md-7').removeClass('col-md-7').addClass('col-md-10')
					.find('.col-md-2').removeClass('col-md-2').addClass('col-md-1').end()
					.each(function(){
						$(this).find('.col-md-1').show();
					});
			} else {
				$('.normal-load-editor')
					.find('.row > .col-md-2').removeClass('col-md-2').addClass('col-md-5').end()
					.find('.row > .col-md-offset-2').removeClass('col-md-offset-2').addClass('col-md-offset-5').end()
					.find('.row > .col-md-10').removeClass('col-md-10').addClass('col-md-7')
					.find('.col-md-1').removeClass('col-md-1').addClass('col-md-2').end()
					.each(function(){
						$(this).find('.col-md-2').filter(function(i){ return i > 5; }).hide();
					});

			}
		}).trigger('change');

		var $modal = $('#copy-loads-modal')
			, $loading = $modal.find('loading')
			, $content = $modal.find('.content')
			, $academicYearSelect = $('select[name=academicYear]')
			, $copyButton = $modal.find('.modal-footer .btn-primary').on('click', function(){
				var data = $(this).data('otherAcademicYear');
				if (data) {
					var routeCodes = _.keysIn(data), yearsOfStudy = _.keysIn(data[routeCodes[0]]);
					_.each(routeCodes, function(routeCode){ _.each(yearsOfStudy, function(year){
						$('input[name="normalLoads[' + routeCode + '][' + year + ']"]').val(data[routeCode][year]);
					})});
					$content.empty();
					$academicYearSelect.val('');
					$modal.modal('hide');
				}
			})
			, complete = false;

		$modal.on('click', 'select', function(){
			var academicYear = $academicYearSelect.find(':selected').val();
			if (academicYear.length > 0) {
				$content.empty();
				$copyButton.prop('disabled', true);
				setTimeout(function() {
					if (!complete) {
						$loading.show();
					}
				}, 300);
				$.get($academicYearSelect.data('href') + academicYear, function(data){
					var routeCodes = _.keysIn(data).sort();
					if (routeCodes.length === 0) {
						$content.append("Could not find any routes owned by this department");
					} else {
						var yearsOfStudy = _.sortBy(_.keysIn(data[routeCodes[0]]), function(year){ return parseInt(year); });
						var $table = $('<table/>').addClass('table table-striped table-condensed').append(
							$('<thead/>').append(
								$('<tr/>').append(
									$('<th/>')
								).append(
									$('<th/>').attr('colspan', yearsOfStudy.length).html('Year of study')
								)
							).append(
								$('<tr/>').append(
									$('<th/>').html('Routes')
								)
							)
						).append(
							$('<tbody/>')
						);
						var $yearOfStudyRow = $table.find('thead tr:nth-of-type(2)'), $tbody = $table.find('tbody');
						_.each(yearsOfStudy, function(year){
							$yearOfStudyRow.append($('<th/>').html(year));
						});
						_.each(routeCodes, function(routeCode){
							var $row = $('<tr/>').append($('<th/>').html(routeCode.toUpperCase()));
							_.each(yearsOfStudy, function(year){
								$row.append($('<td/>').html(data[routeCode][year]));
							});
							$tbody.append($row);
						});
						$content.append($table).wideTables();
						$copyButton.data('otherAcademicYear', data).prop('disabled', false);
					}

					$loading.hide();
					complete = true;
				});
			}
		});
	});
};

window.ExamGrids = jQuery.extend(window.ExamGrids, examGridsExports);

$(function(){

	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	/*** Admin home page ***/
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

})

}(jQuery));