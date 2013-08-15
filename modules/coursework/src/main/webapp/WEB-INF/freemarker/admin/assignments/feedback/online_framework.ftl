<#compress>

<#assign module = assignment.module />
<#assign department = module.department />
<#assign feedbackGraphs = studentFeedbackGraphs />

<#macro row graph>
	<#assign u = graph.student />
	<tr class="itemContainer">
		<#if department.showStudentName>
              			<td class="student-col"><h6 data-profile="${u.warwickId}">${u.firstName}</h6></td>
              			<td class="student-col"><h6 data-profile="${u.warwickId}">${u.lastName}</h6></td>
              		<#else>
              			<td class="student-col"><h6 data-profile="${u.warwickId}">${u.warwickId}</h6></td>
              		</#if>
		<td class="status-col">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					<#if !graph.hasSubmission>
						<div class="label">No submission</div>
					</#if>
					<#if graph.hasPublishedFeedback>
						<#-- TODO semantic label classes? -->
						<div class="label label-success">Published</div>
					<#elseif graph.hasFeedback>
						<div class="label label-warning">Marked</div>
					</#if>
				</dt>
				<dd style="display: none;" class="feedback-container-container" data-profile="${u.warwickId}">
					<div id="feedback-${u.warwickId}" class="feedback-container">
						<p>No data is currently available.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
<div>
	<h1>Online marking</h1>
	<h5>for ${assignment.name} (${assignment.module.code?upper_case})</h5>

	<#import "../turnitin/_report_macro.ftl" as tin />
	<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

	<#-- TODO 20 day turnaround deadline status alert thing rendering -->

	<table id="online-marking-table" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
		<thead<#if feedbackGraphs?size == 0> style="display: none;"</#if>>
			<tr>
				<#if department.showStudentName>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<th class="student-col">University ID</th>
				</#if>

				<th class="status-col">Status</th>
			</tr>
		</thead>

		<#if feedbackGraphs?size gt 0>
			<tbody>
				<#list feedbackGraphs as graph>
					<@row graph />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if feedbackGraphs?size gt 0>
		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script type="text/javascript">
		(function($) {
			var repositionAjaxDivs = function() {
				// These have to be positioned in the right order, so we loop through feedback-container-container
				// rather than directly on feedback-container
				$('.feedback-container-container').hide().each(function() {
					var universityId = $(this).attr('data-profile');
					var $feedback = $('#feedback-' + universityId);

					if ($feedback.length) {
						var isOpen = $feedback.data('open');

						if (isOpen) {
							$(this).show();
							var $statusField = $(this).closest('.status-col');

							// Add bottom padding equivalent to the height of the feedback div to the status field
							var fieldPosition = $statusField.position();

							$statusField.css('padding-bottom', '');
							var fieldHeight = $statusField.outerHeight();
							var feedbackHeight = $feedback.outerHeight();
							$statusField.css('padding-bottom', feedbackHeight + 10);

							// Position the workflow div in the correct location
							$feedback.css({
								top: (fieldPosition.top + fieldHeight - 2) + 'px',
								left: ($('.students').position().left + 1) + 'px'
							});
						}
					}
				});
			};

			$.tablesorter.addWidget({
				id: 'repositionAjaxDivs',
				format: repositionAjaxDivs
			});

			// Expanding and contracting
			$('.students tbody tr').each(function() {
				var $nameField = $(this).find('.student-col h6').first();
				var $statusField = $(this).find('.status-col').first();
				var $icon = $('<i class="icon-chevron-right icon-fixed-width"></i>').css('margin-top', '2px');

				$nameField
					.prepend(' ')
					.prepend($icon)
					.closest('tr')
					.find('.student-col,.status-col')
					.css('cursor', 'pointer')
					.on('click', function(evt) {
						var universityId = $nameField.attr('data-profile');
						var $feedback = $('#feedback-' + universityId);
						if ($feedback.length) {
							var isOpen = $feedback.data('open');

							if (isOpen) {
								// Hide the $feedback div and move it back to where it was before
								$feedback.hide();
								$statusField.find('dd').append($feedback);

								// Remove the bottom padding on the status field
								$statusField.css('padding-bottom', '');

								// Remove any position data from the $feedback div
								$feedback.attr('style', '');

								// Change the icon to closed
								$icon.removeClass('icon-chevron-down').addClass('icon-chevron-right');

								// Set the data
								$feedback.data('open', false);

								repositionAjaxDivs();

								// TODO add badge to $statusField.find('dt') if unsaved and no badge already there
								// ...
								// ...
							} else {
								// get the data via AJAX and fill the feedback div
								var sUrl = '<@routes.onlinemarking assignment />' + '/' + universityId;
								$feedback.load(sUrl, function() {
									// Move the feedback div to be at the end of the offset parent and display it
									$('#main-content').append($feedback);
									$feedback.show();

									if ($statusField.closest('tr').is(':nth-child(odd)')) {
										$feedback.css('background-color', '#ebebeb');
									} else {
										$feedback.css('background-color', '#f5f5f5');
									}

									$feedback.css({
										width: ($('.students').width() - 21) + 'px'
									});

									// Change the icon to open
									$icon.removeClass('icon-chevron-right').addClass('icon-chevron-down');

									// Set the data
									$feedback.data('open', true);

									repositionAjaxDivs();
								});
							}

							evt.preventDefault();
							evt.stopPropagation();
						}
					});
			});

			$('.students').tablesorter({
				sortList: [[<#if department.showStudentName>2<#else>1</#if>,0]],
				headers: { 0: { sorter: false } }
			});
		})(jQuery);
		</script>
	<#else>
		<p>There are no students recorded in Tabula for this assignment.</p>
	</#if>
</#escape></#compress>