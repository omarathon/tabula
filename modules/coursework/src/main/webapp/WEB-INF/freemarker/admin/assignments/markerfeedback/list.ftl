<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<h1>Feedback for ${assignment.name}</h1>
	<p>You are the first marker for the following submissions</p>
	<div class="btn-toolbar">
		<a class="btn" href="<@routes.downloadmarkersubmissions assignment=assignment />">
			<i class="icon-download"></i> Download submissions (${items?size})
		</a>
		<a class="btn" href="<@routes.uploadmarkerfeedback assignment=assignment />">
			<i class="icon-upload"></i> Upload feedback
		</a>
		<a class="btn" href="<@routes.markeraddmarks assignment=assignment />">
			<i class="icon-plus"></i> Add Marks
		</a>
	</div>
	<table class="table table-bordered table-striped">
		<tr>
			<th>Student</th>
			<th>Date submitted</th>
			<th>Mark</th>
			<th>Feedback files</th>
			<th>Status</th>
		</tr>
		<#list items as item>
			<tr>
				<td>
					<#if assignment.module.department.showStudentName>
						${item.student.fullName}
					<#else>
						${item.student.warwickId}
					</#if>
				</td>
				<td><@fmt.date date=item.submission.submittedDate seconds=true capitalise=true /></td>
				<td><#if item.markerFeedback??>
					${item.markerFeedback.mark!''}
				</#if></td>
				<td><#if item.markerFeedback??>
					<#assign attachments=item.markerFeedback.attachments />
					<#if attachments?size gt 0>
						<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${item.markerFeedback.id}/feedback-${item.markerFeedback.feedback.universityId}.zip'/>">
							<i class="icon-download"></i>
							${attachments?size}
							<#if attachments?size == 1> file<#else> files</#if>
						</a>
					</#if>
				</#if></td>
				<td></td>
			</tr>
		</#list>
	</table>
</#escape>




