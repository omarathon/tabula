<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/submission_components.ftl" as components />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<#macro listMarkerFeedback items nextRoleName>
	<#list items as item>

		<#assign u = item.student />
		<#local thisFeedback = item.feedbacks?last />
		<#local nextMarkerAction>Send to ${nextRoleName} <#if item.nextMarker?has_content>(${item.nextMarker.fullName})</#if></#local>

		<tr class="item-container <#if thisFeedback.state.toString == "InProgress">in-progress</#if>"
			data-contentid="${markingId(u)}"
			data-markingurl="${onlineMarkingUrls[u.userId]}"
			data-nextmarkeraction="${nextMarkerAction}">
			<td class="check-col">
				<@form.selector_check_row "markerFeedback" thisFeedback.id />
			</td>

			<#if assignment.module.department.showStudentName>
				<td class="student-col toggle-cell"><h6 class="toggle-icon">${item.student.firstName}</h6></td>
				<td class="student-col toggle-cell"><h6>${item.student.lastName} <@pl.profile_link item.student.warwickId! /></h6></td>
				<#assign toggleIcon = "" />
			<#else>
				<#assign toggleIcon = "toggle-icon" />
			</#if>
			<td class="student-col toggle-cell"><h6 class="${toggleIcon}">${item.student.warwickId!}</h6></td>

			<td class="status-col toggle-cell content-cell">
				<dl style="margin: 0; border-bottom: 0;">
					<dt>
						<#if thisFeedback.state.toString == "ReleasedForMarking">
							<span class="label label-warning">Ready for marking</span>
						<#elseif thisFeedback.state.toString == "InProgress">
							<span class="label label-info">In Progress</span>
						<#elseif thisFeedback.state.toString == "MarkingCompleted">
							<span class="label label-success">Marking completed</span>
						<#elseif thisFeedback.state.toString == "Rejected">
							<span class="label label-important">Rejected</span>
						</#if>
					</dt>
					<dd style="display: none;" class="table-content-container" data-contentid="${markingId(u)}">
						<div id="content-${markingId(u)}" class="content-container" data-contentid="${markingId(u)}">
							<p>No data is currently available. Please check that you are signed in.</p>
						</div>
					</dd>
				</dl>
			</td>
			<td class="action-col toggle-cell">
				<#if thisFeedback.state.toString == "ReleasedForMarking">
					Submission needs marking
				<#elseif thisFeedback.state.toString == "InProgress">
					${nextMarkerAction}
				<#elseif thisFeedback.state.toString == "Rejected">
					Review feedback and re-send to ${nextRoleName} <#if item.nextMarker?has_content>(${item.nextMarker.fullName})</#if>
				<#elseif thisFeedback.state.toString == "MarkingCompleted">
					No action required<#if item.nextMarker?has_content> - Sent to ${item.nextMarker.fullName}</#if>
				</#if>
			</td>
		</tr>
	</#list>
</#macro>

<#macro workflowActions nextRoleName previousRoleName currentRoleName>
<div class="btn-toolbar">
	<#if previousRoleName?has_content>
		<a class="use-tooltip form-post btn btn-danger must-be-blank disabled"
		   title="Return marks and feedback to the ${previousRoleName}. You cannot return in-progress or completed feedback."
		   data-container="body"
		   href="<@routes.markingUncompleted assignment marker previousRoleName />"
		   id="marking-uncomplete-button">
			<i class="icon-arrow-left"></i> Reject and return to ${previousRoleName}
		</a>
	</#if>

	<#if currentRoleName != 'Moderator'>
		<a class="use-tooltip form-post btn btn-primary must-be-populated disabled"
		   title="Finalise marks and feedback. Changes cannot be made to marks or feedback files after this point. You cannot finalise blank feedback."
		   data-container="body"
		   href="<@routes.markingCompleted assignment marker nextRoleName/>"
		   id="marking-complete-button">
			Confirm and send to ${nextRoleName} <i class="icon-arrow-right"></i>
		</a>
	</#if>
</div>
</#macro>

<#escape x as x?html>
	<h1><@fmt.module_name assignment.module /></h1>
	<h2>Feedback for ${assignment.name}</h2>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<div class="btn-toolbar">
		<#assign disabledClass><#if feedbackToDoCount == 0>disabled</#if></#assign>
		<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
			<a class="btn use-tooltip"
				title="Download feedback templates for all students as a ZIP file."
				href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker-templates.zip'/>"
				data-container="body"
			>
				<i class="icon-download"></i> Download feedback templates
			</a>

		</#if>

		<div class="btn-group">
			<a class="btn dropdown-toggle"
		   		data-toggle="dropdown"
		   		href="#">
				<i class="icon-download"></i> Download
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<a class="use-tooltip"
					   title="Download a zip of submissions due to be marked. Note that submissions with a status of 'Marking completed' will not be included in this zip"
					   href="<@routes.downloadmarkersubmissions assignment=assignment marker=marker />"
					   data-container="body">
						<i class="icon-download"></i> Download submissions
					</a>
				</li>
				<#if hasFirstMarkerFeedback>
					<li>
						<a href="<@routes.downloadfirstmarkerfeedback assignment=assignment marker=marker />">
							<i class="icon-download"></i> Download ${firstMarkerRoleName} feedback
						</a>
					</li>
				</#if>
				<#if hasSecondMarkerFeedback>
					<li>
						<a href="<@routes.downloadsecondmarkerfeedback assignment=assignment  marker=marker />">
							<i class="icon-download"></i> Download ${secondMarkerRoleName} feedback
						</a>
					</li>
				</#if>
			</ul>
		</div>
		<div class="btn-group">
			<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
				<i class="icon-upload"></i> Upload
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<a class="" href="<@routes.uploadmarkerfeedback assignment=assignment marker=marker/>">
						<i class="icon-upload"></i> Upload attachments
					</a>
				</li>
				<li>
					<a class="${disabledClass}" href="<@routes.markeraddmarks assignment=assignment marker=marker/>">
						<i class="icon-plus"></i> Upload Marks
					</a>
				</li>
			</ul>
		</div>
	</div>
	<#if markerFeedback?has_content>
		<#list markerFeedback as stage>
			<div class="well workflow-role">
				<h3>${stage.roleName}</h3>
				<@workflowActions stage.nextRoleName stage.previousRoleName!"" stage.roleName!""/>
				<table class="table
							  table-bordered
							  table-striped
							  tabula-greenLight
							  sticky-table-headers
							  expanding-table
							  tablesorter
							  marker-feedback-table">
					<thead><tr>
						<th class="check-col no-sort">
							<@form.selector_check_all />
						</th>
						<#if assignment.module.department.showStudentName>
							<th class="student-col">First name</th>
							<th class="student-col">Last name</th>
						</#if>
						<th class="student-col">University ID</th>
						<th class="status-col">Status</th>
						<th class="status-col">Next action</th>
					</tr></thead>
					<tbody>
						<@listMarkerFeedback stage.feedbackItems stage.nextRoleName />
					</tbody>
				</table>
				<@workflowActions stage.nextRoleName stage.previousRoleName!"" stage.roleName!""/>
			</div>
		</#list>
		<script type="text/javascript">
			(function($) {
				var tsOptions = {
					sortList: [<#if assignment.module.department.showStudentName>[3, 0], </#if>[2, 0], [1,0]],
					headers: { 0: { sorter: false} }
				};

				$('.expanding-table').expandingTable({
					contentUrlFunction: function($row){ return $row.data('markingurl'); },
					useIframe: true,
					tableSorterOptions: tsOptions
				});

			})(jQuery);
		</script>
	<#else>
		<p>There are no submissions for you to mark</p>
	</#if>
</#escape>