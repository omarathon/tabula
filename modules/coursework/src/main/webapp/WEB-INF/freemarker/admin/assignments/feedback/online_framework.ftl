<#escape x as x?html>
<div>
	<h1>Online marking</h1>
	<h5>for ${assignment.name} (${assignment.module.code?upper_case})</h5>

	<#assign module=assignment.module />
	<#assign department=module.department />

	<@import "../turnitin/_report_macro.ftl" as tin />

	<#-- TODO 20 day turnaround deadline status alert thing rendering -->

	<table id="online-marking-table" class="students table table-bordered table-striped tabula-greenLight sticky-table-headers">
		<thead<#if students?size == 0> style="display: none;"</#if>>
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

		<#if students?size gt 0>
			<tbody>
				<#macro row student>
					<tr class="itemContainer">
						<#if department.showStudentName>
							<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.firstName}</h6></td>
							<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.lastName}</h6></td>
						<#else>
							<td class="student-col"><h6 data-profile="${student.user.warwickId}">${student.user.warwickId}</h6></td>
						</#if>
						<td class="status-col">
							<dl style="margin: 0; border-bottom: 0;">
								<dt>
									<div class="label">Pointless</div>
								</dt>
								<dd style="display: none;" id="feedback-${student.user.warwickId}" class="feedback-container" data-profile="${student.user.warwickId}">
									<p>Nothing to see here</p>
								</dd>
							</dl>
						</td>
					</tr>
				</#macro>

				<#list students as student>
					<@row student />
				</#list>
			</tbody>
		</#if>
	</table>

	<#if students?size gt 0>
		<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
		<script type="text/javascript">
		(function($) {
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
							} else {
								// Move the workflow div to be at the end of the offset parent and display it
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
		<p>There are no unmarked submissions for this assignment yet.</p>
	</#if>
</#escape>