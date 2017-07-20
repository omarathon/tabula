<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/_profile_link.ftl" as pl />
<#escape x as x?html>
	<@cm2.assignmentHeader "Online marking" assignment />

	<#if enhancedFeedbacks?has_content>
		<table class="table table-striped expanding-table" data-row-to-open="${studentToOpen!""}">
			<thead>
			<tr>
				<th class="student-col">University ID</th>
				<#if department.showStudentName>
					<#assign colspan = 4>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
				<#else>
					<#assign colspan = 2>
				</#if>
					<th>Status</th>
			</tr>
			</thead>
			<tbody>
				<#list enhancedFeedbacks as ef>
					<#assign student = ef.student />
					<tr
						data-toggle="collapse"
						data-target="#${student.userId}"
						class="clickable collapsed expandable-row"
					>
						<td class="student-col toggle-cell toggle-icon">&nbsp;<#if student.warwickId??>${student.warwickId}<#else>${student.userId}</#if></td>
						<#if department.showStudentName>
							<td class="student-col toggle-cell">${student.firstName}</td>
							<td class="student-col toggle-cell">${student.lastName}&nbsp;<#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></td>
						</#if>
						<td class="status-col toggle-cell content-cell">
							<#if ef.published>
								Feedback published
							<#else>
								<#if ef.hasContent>Marked</#if><#if !ef.hasSubmission><#if ef.hasContent>, </#if>No submission</#if>
							</#if>
						</td>
					</tr>
					<tr id="${student.userId}" data-detailurl="<@routes.cm2.onlinefeedbackform assignment student />" class="collapse detail-row">
						<td colspan="${colspan}" class="detailrow-container">
							No data is currently available. Please check that you are signed in.
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
		<script type="text/javascript">
			(function($) {

				var $body = $('body');

				$body.on('shown.bs.collapse', function(e){
					var $row = $(e.target);
					$row.tabulaAjaxForm({
						successCallback: function($container){
							var $row = $container.closest('tr').prev();
							var $statusCol = $row.find('.status-col');
							var status = $statusCol.text();
							// status is empty or only contains whitespace
							if(status.length === 0 || !status.trim()) {
								$statusCol.text("Marked");
							} else if (!status.trim().startsWith("Marked")) {
								$statusCol.text("Marked, "+status);
							}
						}
					});
				});

			})(jQuery);
		</script>
		<div id="profile-modal" class="modal fade profile-subset"></div>
	<#else>
		No students to mark for this assignment
	</#if>
</#escape>