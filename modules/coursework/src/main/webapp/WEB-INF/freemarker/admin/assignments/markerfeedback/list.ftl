<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl >
<#macro listMarkerFeedback items>
	<#list items as item>
		<tr>
			<td>
				<@form.selector_check_row "students" item.student.warwickId />
			</td>
			<td>
				<#if assignment.module.department.showStudentName>
					${item.student.fullName} <@pl.profile_link item.student.warwickId />
				<#else>
					${item.student.warwickId}
				</#if>
			</td>
			<td>
				<@fmt.date date=item.submission.submittedDate seconds=true capitalise=true />
			</td>

			<#if hasSecondMarkerFeedback || hasFirstMarkerFeedback>
				<#local lastIndex = item.feedbacks?size - 1 />
				<td>
					<#list item.feedbacks as feedback>
						<#if feedback_index < lastIndex>
							${feedback.mark!''}<#if feedback_index < lastIndex - 1><br/></#if>
						</#if>
					</#list>
				</td>

				<td>
					<#list item.feedbacks as feedback>
						<#if feedback_index < lastIndex>
							${feedback.grade!''}<#if feedback_index < lastIndex - 1><br/></#if>
						</#if>
					</#list>
				</td>
				<td>
					<#list item.feedbacks as feedback>
						<#if feedback_index < lastIndex>
							<#local attachments=feedback.attachments />
							<#if attachments?size gt 0>
								<a class="btn long-running" href="<@routes.downloadMarkerFeedback assignment feedback />">
									<i class="icon-download"></i>
									${attachments?size}
									<#if attachments?size == 1> file<#else> files</#if>
								</a>
								<#if feedback_index < lastIndex - 1><br/></#if>
							</#if>
						</#if>
					</#list>
				</td>
			</#if>

			<#if (item.feedbacks?size > 0)>
				<#local thisFeedback = item.feedbacks?last />
				<td>
					${thisFeedback.mark!''}
				</td>
				<td>
					${thisFeedback.grade!''}
				</td>
				<td>
					<#local attachments=thisFeedback.attachments />
					<#if attachments?size gt 0>
						<a class="btn long-running" href="<@routes.downloadMarkerFeedback assignment thisFeedback />">
							<i class="icon-download"></i>
							${attachments?size}
							<#if attachments?size == 1> file<#else> files</#if>
						</a>
					</#if>
				</td>

				<td>
					<#if thisFeedback.state.toString == "ReleasedForMarking">
						<span class="label label-warning">Ready for marking</span>
					<#elseif thisFeedback.state.toString == "InProgress">
						<span class="label label-info">In Progress</span>
					<#elseif thisFeedback.state.toString == "MarkingCompleted">
						<span class="label label-success">Marking completed</span>
					<#elseif thisFeedback.state.toString == "Rejected">
						<span class="label label-important">Rejected</span>
					</#if>
				</td>

			<#else>
				<td></td>
				<td></td>
				<td></td>
				<td></td>
			</#if>
		</tr>
	</#list>
</#macro>

<#escape x as x?html>

	<h1>Feedback for ${assignment.name}</h1>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<p>You are the <#if hasSecondMarkerFeedback>${thirdMarkerRoleName}<#elseif hasFirstMarkerFeedback>${secondMarkerRoleName}<#else>${firstMarkerRoleName}</#if> for the following submissions</p>

	<div class="btn-toolbar">
		<#assign feedbackToDoCount = inProgressFeedback?size + rejectedFeedback?size />
		<#assign disabledClass><#if feedbackToDoCount == 0>disabled</#if></#assign>
		<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
			<a class="btn use-tooltip" title="Download feedback templates for all students as a ZIP file." href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker-templates.zip'/>"><i class="icon-download"></i>
				Download feedback templates
			</a>
		</#if>
		<a class="btn use-tooltip ${disabledClass}"
		   title="Download a zip of submissions due to be marked. Note that submissions with a status of 'Marking completed' will not be included in this zip"
		   href="<@routes.downloadmarkersubmissions assignment=assignment />"
		   data-container="body"
		>
			<i class="icon-download"></i> Download submissions (${feedbackToDoCount})
		</a>
		<#if hasFirstMarkerFeedback && hasSecondMarkerFeedback>
			<div class="btn-group">
				<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-download"></i> Download feedback
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<li>
						<a href="<@routes.downloadfirstmarkerfeedback assignment=assignment />">
							<i class="icon-download"></i> Download ${firstMarkerRoleName} feedback
						</a>
					</li>
					<li>
						<a href="<@routes.downloadsecondmarkerfeedback assignment=assignment />">
							<i class="icon-download"></i> Download ${secondMarkerRoleName} feedback
						</a>
					</li>
				</ul>
			</div>
		<#elseif hasFirstMarkerFeedback>
			<a class="btn" href="<@routes.downloadfirstmarkerfeedback assignment=assignment />">
				<i class="icon-download"></i> Download ${firstMarkerRoleName} feedback
			</a>
		</#if>
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
					   data-container="body"
					   href="<@routes.markingCompleted assignment />"
					   id="marking-complete-button">
						<i class="icon-ok"></i> Marking completed
					</a>
				</li>
			</ul>
		</div>
	</div>
	<div class="submission-feedback-list">
		<table class="table table-bordered table-striped">
			<thead><tr>
				<th>
					<div class="clearfix">
						<@form.selector_check_all />
					</div>
				</th>
				<th>Student</th>
				<th>Date submitted</th>
				<#if hasSecondMarkerFeedback>
					<th>Other marks</th>
					<th>Other grades</th>
					<th>Other feedback</th>
				<#elseif hasFirstMarkerFeedback>
					<th>First mark</th>
					<th>First grade</th>
					<th>First feedback</th>
				</#if>
				<th>Mark</th>
				<th>Grade</th>
				<th>Feedback files</th>
				<th>Status</th>
			</tr></thead>
			<tbody>
				<@listMarkerFeedback inProgressFeedback/>
				<@listMarkerFeedback completedFeedback />
				<@listMarkerFeedback rejectedFeedback />
			</tbody>
		</table>
	</div>
</#escape>