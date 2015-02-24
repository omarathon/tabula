<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#escape x as x?html>

<h1>${exam.name} (${module.code?upper_case})</h1>

<div class="btn-toolbar">
	<#assign marks_url><@routes.examAddMarks exam /></#assign>
	<@fmt.permission_button
		permission='Marks.Create'
		scope=exam
		action_descr='add marks'
		href=marks_url
		classes='btn'
	>
		<i class="icon-check"></i> Add marks
	</@fmt.permission_button>
</div>

<table class="table table-bordered table-striped table-condensed feedback-table">
	<thead>
		<tr>
			<th style="width: 20px; padding-right: 5px;" class="for-check-all"></th>
			<th class="sortable">Seat order</th>
			<th class="sortable">Student</th>
			<th colspan="2">Original</th>
			<th colspan="2">Adjusted</th>
			<th colspan="4">SITS upload</th>
		</tr>
		<tr>
			<th colspan="3"></th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
			<th class="sortable">Status</th>
			<th class="sortable">Date</th>
			<th class="sortable">Mark</th>
			<th class="sortable">Grade</th>
		</tr>
	</thead>
	<tbody>
		<#list students as student>
			<#assign hasFeedback = mapGet(feedbackMap, student)?? />
			<#assign hasSitsStatus = hasFeedback && mapGet(sitsStatusMap, mapGet(feedbackMap, student))?? />
			<tr>
				<td>

				</td>
				<td>${student_index + 1}</td>
				<td>
					<#if module.department.showStudentName>
						${student.fullName} <@pl.profile_link student.warwickId />
					<#else>
						${student.warwickId!}
					</#if>
				</td>
				<#if hasFeedback>
					<#assign feedback = mapGet(feedbackMap, student) />
					<td>${feedback.actualMark!""}</td>
					<td>${feedback.actualGrade!""}</td>
					<td>${feedback.adjustedMark!""}</td>
					<td>${feedback.adjustedGrade!""}</td>
					<#if hasSitsStatus>
						<#assign sitsStatus = mapGet(sitsStatusMap, feedback) />
						<#assign sitsClass>
							<#if sitsStatus.status.code == "failed">
								label-important
							<#elseif sitsStatus.status.code == "successful">
								label-success
							<#elseif sitsStatus.status.code == "uploadNotAttempted">
								label-info
							</#if>
						</#assign>
						<td><span class="label ${sitsClass}">${sitsStatus.status.description}</span></td>
						<td data-sortby="${(sitsStatus.dateOfUpload.millis?c)!""}"><#if sitsStatus.dateOfUpload??><@fmt.date sitsStatus.dateOfUpload /></#if></td>
						<td>${sitsStatus.actualMarkLastUploaded!""}</td>
						<td>${sitsStatus.actualGradeLastUploaded!""}</td>
					<#else>
						<td><span class="label">Upload not queued</span></td><td></td><td></td><td></td>
					</#if>
				<#else>
					<td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td>
				</#if>
			</tr>
		</#list>
	</tbody>
</table>

<script type="text/javascript">
	(function($) {
		$('.feedback-table').sortableTable({
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});

	})(jQuery);
</script>

</#escape>