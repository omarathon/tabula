<#macro listMarkerFeedback items>
	<#list items as item>
		<tr>
			<#if features.markerFeedback><td><@form.selector_check_row "students" item.student.warwickId /></td></#if>
			<td>
				<#if assignment.module.department.showStudentName>
					${item.student.fullName}
				<#else>
					${item.student.warwickId}
				</#if>
			</td>
			<td><@fmt.date date=item.submission.submittedDate seconds=true capitalise=true /></td>
			<#if !isFirstMarker>
				<td>
					${item.firstMarkerFeedback.mark!''}
				</td>
				<td>
					${item.firstMarkerFeedback.grade!''}
				</td>
				<td>
					<#if item.firstMarkerFeedback??>
						<#assign attachments=item.firstMarkerFeedback.attachments />
						<#if attachments?size gt 0>
							<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${item.markerFeedback.id}/feedback-${item.markerFeedback.feedback.universityId}.zip'/>">
								<i class="icon-download"></i>
								${attachments?size}
								<#if attachments?size == 1> file<#else> files</#if>
							</a>
						</#if>
					</#if>
				</td>
			</#if>
			<#if features.markerFeedback>
				<#if item.markerFeedback??>
					<td>${item.markerFeedback.mark!''}</td>
					<td>${item.markerFeedback.grade!''}</td>
					<td>
						<#assign attachments=item.markerFeedback.attachments />
						<#if attachments?size gt 0>
							<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker/feedback/download/${item.markerFeedback.id}/feedback-${item.markerFeedback.feedback.universityId}.zip'/>">
								<i class="icon-download"></i>
								${attachments?size}
								<#if attachments?size == 1> file<#else> files</#if>
							</a>
						</#if>
					</td>
				<#else>
					<td></td><td></td><td></td>
				</#if>
				<td>
					<#if item.markerFeedback.state.toString == "ReleasedForMarking">
						<span class="label label-warning">Ready for marking</span>
					<#elseif item.markerFeedback.state.toString == "DownloadedByMarker">
						<span class="label label-info">Downloaded</span>
					<#elseif item.markerFeedback.state.toString == "MarkingCompleted">
						<span class="label label-success">Marking completed</span>
					<#elseif item.markerFeedback.state.toString == "Rejected">
						<span class="label label-important">Rejected</span>
					</#if>
				</td>
			</#if>
		</tr>
	</#list>
</#macro>

<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<h1>Feedback for ${assignment.name}</h1>
	<p>You are the <#if isFirstMarker>${firstMarkerRoleName}<#else>${secondMarkerRoleName}</#if> for the following submissions</p>
	<div class="btn-toolbar">
		<#assign disabledClass><#if items?size == 0>disabled</#if></#assign>
		<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
			<a class="btn use-tooltip" title="Download feedback templates for all students as a ZIP file." href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker-templates.zip'/>"><i class="icon-download"></i>
				Download feedback templates
			</a>
		</#if>
		<a class="btn use-tooltip ${disabledClass}"
		   title="Download a zip of submissions due to be marked. Note that submissions with a status of 'Marking completed' will not be included in this zip"
		   href="<@routes.downloadmarkersubmissions assignment=assignment />">
			<i class="icon-download"></i> Download submissions (${items?size})
		</a>
		<#if !isFirstMarker>
			<a class="btn" href="<@routes.downloadfirstmarkerfeedback assignment=assignment />">
				<i class="icon-download"></i> Download ${firstMarkerRoleName} feedback
			</a>
		</#if>
		<#if features.markerFeedback>

			<div class="btn-group">
				<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-upload"></i> Upload feedback
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<li>
						<a class="${disabledClass}" href="<@routes.uploadmarkerfeedback assignment=assignment />">
							<i class="icon-upload"></i> Upload attachments
						</a>
						<a class="${disabledClass}" href="<@routes.markeraddmarks assignment=assignment />">
							<i class="icon-plus"></i> Add Marks
						</a>
					</li>
				</ul>
			</div>
			<a class="btn ${disabledClass}" href="<@routes.markerOnlinefeedback assignment />">
				<i class="icon-edit"></i> Online feedback
			</a>
			<div class="btn-group">
				<a id="modify-selected" class="btn dropdown-toggle" data-toggle="dropdown" href="#">
					Update selected
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<li>
						<a class="use-tooltip form-post"
						   title="Finalise marks and feedback. Changes cannot be made to marks or feedback files after this point."
						   href="<@routes.markingCompleted assignment />"
						   id="marking-complete-button">
							<i class="icon-ok"></i> Marking completed
						</a>
					</li>
				</ul>
			</div>
	    </#if>
	</div>
	<div class="submission-feedback-list">
		<#if features.markerFeedback>
			<div class="clearfix">
				<@form.selector_check_all />
			</div>
		</#if>
		<table class="table table-bordered table-striped">
			<thead><tr>
				<#if features.markerFeedback><th></th></#if>
				<th>Student</th>
				<th>Date submitted</th>
				<#if !isFirstMarker>
					<th>First mark</th>
					<th>First grade</th>
					<th>First feedback</th>
				</#if>
				<#if features.markerFeedback>
					<th>Mark</th>
					<th>Grade</th>
					<th>Feedback files</th>
					<th>Status</th>
				</#if>
			</tr></thead>
			<tbody>
				<@listMarkerFeedback completedFeedback />
				<@listMarkerFeedback rejectedFeedback />
				<@listMarkerFeedback items/>
			</tbody>
		</table>
	</div>
</#escape>