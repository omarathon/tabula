<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
	<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
</#if>

<#macro student_assignment_list id title assignments expand_by_default=true show_submission_progress=false>
	<span id="${id}-container">
		<#local has_assignments = (assignments!?size gt 0) />
		<div id="${id}" class="striped-section student-assignment-list<#if has_assignments> collapsible<#if expand_by_default> expanded</#if><#else> empty</#if>" data-name="${id}">
			<div class="clearfix">
				<h4 class="section-title">${title}</h4>

				<#if has_assignments>
					<div class="striped-section-contents">
						<div class="row">
							<div class="col-md-3">Details</div>
							<div class="col-md-4 col-lg-5">Progress</div>
							<div class="col-md-5 col-lg-4">Actions</div>
						</div>

						<#list assignments as info>
							<span id="assignment-container-${info.assignment.id}">
								<@student_assignment_info info show_submission_progress />
							</span>
						</#list>
					</div>
				</#if>
			</div>
		</div>
	</span>

	<#if !expand_by_default>
		<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
		<script type="text/javascript">
			GlobalScripts.initCollapsible(jQuery('#${id}').filter(':not(.empty)'));
		</script>
	</#if>
</#macro>

<#macro progress_bar tooltip percentage class="default">
	<div class="progress use-tooltip" title="${tooltip}" data-html="true" data-container="body">
		<div class="progress-bar progress-bar-${class}" role="progressbar" aria-valuenow="${percentage?c}" aria-valuemin="0" aria-valuemax="100" style="width: ${percentage?c}%;">
		</div>
	</div>
</#macro>

<#macro submission_progress info>
	<#local assignment = info.assignment />

	<#local state = "default" />
	<#local percentage = 0 />
	<#local tooltip = "" />

	<#if info.submission??>
		<#local submission = info.submission />
		<#local percentage = 100 />

		<#if submission.late>
			<#local state = "danger" />
			<#local tooltip><@lateness assignment user submission /></#local>
		<#elseif submission.authorisedLate>
			<#local state = "success" />
			<#local tooltip>
				Submitted within extension ${durationFormatter(submission.submittedDate)} (<@fmt.date date=submission.submittedDate />)
			</#local>
		<#else>
			<#local state = "success" />
			<#local tooltip>
				Submitted ${durationFormatter(submission.submittedDate)} (<@fmt.date date=submission.submittedDate />)
			</#local>
		</#if>
	<#elseif !assignment.opened>
		<#local percentage = 1 />
		<#local state = "default" />
		<#local tooltip>
			Opens in ${durationFormatter(assignment.openDate)}
		</#local>
	<#elseif assignment.openEnded>
		<#local percentage = 100 />
		<#local state = "info" />
		<#local tooltip>
			Open-ended assignment
		</#local>
	<#elseif info.submittable>
		<#local extension = info.extension! />

		<#local time_remaining = durationFormatter(info.studentDeadline) />
		<#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
		<#if info.hasActiveExtension>
			<#local extension_time_remaining = durationFormatter(extension.expiryDate) />
		</#if>

		<#if info.extended>
			<#local state = "info" />
			<#local tooltip>
				${extension_time_remaining} until extended deadline (<@fmt.date date=extension.expiryDate />)
			</#local>
		<#elseif assignment.closed>
			<#local submissionStatus>
			<strong>Late</strong>
			</#local>

			<#local state = "danger" />
			<#local tooltip>
				<@lateness assignment user info.submission />
			</#local>
		<#else>
			<#local state = "success" />
			<#local tooltip>
				Due in ${time_remaining} (<@fmt.date date=info.studentDeadline />)
			</#local>
		</#if>
	<#else>
		<#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
		<#local state = "info" />
		<#local tooltip>
			Assignment close ${durationFormatter(info.studentDeadline)} (<@fmt.date date=info.studentDeadline />)
		</#local>
	</#if>

	<div class="stage-progress-bar time-progress-bar">
		<#if assignment.opened>
			<span class="fa-stack">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x fa-check-circle-o text-success use-tooltip" title="Assignment ready" data-container="body"></i>
			</span>
		<#else>
			<span class="fa-stack">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x fa-circle-o text-default use-tooltip" title="${tooltip}" data-html="true" data-container="body"></i>
			</span>
		</#if>

		<div class="bar use-tooltip" title="${tooltip}" data-html="true" data-container="body">
			<div class="progress-bar progress-bar-${state}" role="progressbar" aria-valuenow="${percentage?c}" aria-valuemin="0" aria-valuemax="100" style="width: ${percentage?c}%;">
			</div>
		</div>

		<#if state == 'success' && !info.submission??>
			<#local state = 'default' />
		</#if>

		<#local icon = 'fa-circle-o' />
		<#if state == 'success'>
			<#local icon = 'fa-check-circle-o' />
		<#elseif state == 'warning'>
			<#local icon = 'fa-dot-circle-o' />
		<#elseif state == 'danger'>
			<#local icon = 'fa-exclamation-circle' />
		</#if>

		<span class="fa-stack">
			<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
			<i class="fa fa-stack-1x ${icon} text-${state} use-tooltip" title="${tooltip}" data-html="true" data-container="body"></i>
		</span>
	</div>
</#macro>

<#macro student_assignment_info info show_submission_progress=false>
	<#local assignment = info.assignment />
	<div class="item-info row assignment-${assignment.id}">
		<div class="col-md-3">
			<div class="module-title"><@fmt.module_name assignment.module /></div>
			<h4 class="name">
				<a href="<@routes.cm2.assignment assignment />">
					<span class="ass-name">${assignment.name}</span>
				</a>
			</h4>
		</div>
		<div class="col-md-4 col-lg-5">
			<#local submissionStatus = "" />

			<#if !assignment.collectSubmissions>
				<#local submissionStatus = "" />
			<#elseif info.submission??>
				<#local submission = info.submission />
				<#local submissionStatus>
					<strong>Submitted:</strong> <@fmt.date date=submission.submittedDate />
				</#local>
			<#elseif !assignment.opened>
				<#local submissionStatus>
					<strong>Assignment open:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />" data-html="true"><@fmt.date date=assignment.openDate /> - ${durationFormatter(assignment.openDate)}</span>
				</#local>
			<#elseif assignment.openEnded>
				<#local submissionStatus>
					<strong>Not submitted</strong>
				</#local>
			<#elseif info.submittable>
				<#local extension = info.extension! />

				<#local time_remaining = durationFormatter(info.studentDeadline) />
				<#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
				<#if info.hasActiveExtension>
					<#local extension_time_remaining = durationFormatter(extension.expiryDate) />
				</#if>

				<#local submissionStatus>
					<strong>Not submitted</strong>
				</#local>

				<#if info.extended>
					<#local submissionStatus>
						<strong>Extension granted</strong>
					</#local>
				<#elseif assignment.closed>
					<#local submissionStatus>
						<strong>Late</strong>
					</#local>
				</#if>
			</#if>

			<#if show_submission_progress>
				<@submission_progress info />
			</#if>

			<#local feedbackStatus = "" />
			<#if info.feedback?? && info.feedback.released>
				<#local feedbackStatus>
					<strong>Feedback received:</strong> <@fmt.date date=info.feedback.releasedDate />
				</#local>
			<#elseif assignment.collectSubmissions>
				<#if info.submission?? && info.feedbackDeadline??>
					<#local feedbackStatus>
						<strong>Feedback <#if info.feedbackLate>over</#if>due:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek info.feedbackDeadline />" data-html="true"><@fmt.date date=info.feedbackDeadline includeTime=false /></span>
						<#if info.feedbackLate>
							<br />
							Please contact your Departmental Administrator with any queries
						</#if>
					</#local>
				<#elseif info.studentDeadline??>
					<#local feedbackStatus>
						<strong>Assignment due:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek info.studentDeadline />" data-html="true"><@fmt.date date=info.studentDeadline /> - ${durationFormatter(info.studentDeadline)}</span>
					</#local>
				</#if>
			</#if>

			<div class="submission-status">${submissionStatus}</div>
			<div class="feedback-status">${feedbackStatus}</div>
		</div>
		<div class="col-md-5 col-lg-4">
			<div class="row">
				<#if info.feedback??>
					<#-- View feedback -->
					<div class="col-md-6">
						<a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
							View feedback
						</a>
					</div>
				<#elseif info.submission?? && info.resubmittable>
					<#-- Resubmission allowed -->
					<div class="col-md-6">
						<a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
							View receipt
						</a>
					</div>

					<div class="col-md-6">
						<a class="btn btn-block btn-default" href="<@routes.cm2.assignment assignment />#submittop">
							Re-submit assignment
						</a>
					</div>
				<#elseif info.submission??>
					<#-- View receipt -->
					<div class="col-md-6">
						<a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
							View receipt
						</a>
					</div>
				<#elseif info.submittable>
					<#-- First submission allowed -->
					<div class="col-md-6">
						<a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
							Submit assignment
						</a>
					</div>

					<#if assignment.extensionsPossible>
						<#if info.extensionRequested>
							<div class="col-md-6">
								<a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.home academicYear />" class="btn btn-block btn-default">
									Review extension request
								</a>
							</div>
						<#elseif !info.extended && assignment.newExtensionsCanBeRequested>
							<div class="col-md-6">
								<a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.home academicYear />" class="btn btn-block btn-default">
									Request extension
								</a>
							</div>
						</#if>
					</#if>
				<#else>
					<#-- Assume formative, so just show info -->
					<div class="col-md-6">
						<a class="btn btn-block btn-default" href="<@routes.cm2.assignment assignment />">
							View details
						</a>
					</div>
				</#if>
			</div>
		</div>
	</div>
</#macro>

<#macro extension_button_contents label assignment>
	<a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.assignment assignment=assignment />" class="btn btn-default btn-xs">
		${label}
	</a>
</#macro>

<#macro extension_button extensionRequested isExtended assignment>
	<#if extensionRequested>
		<@extension_button_contents "Review extension request" assignment />
	<#elseif !isExtended && assignment.newExtensionsCanBeRequested>
		<@extension_button_contents "Request an extension" assignment />
	</#if>
</#macro>

<#macro lateness assignment user submission="">
	<#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
		<#if submission.late>
			<@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(submission.deadline, submission.submittedDate)} after deadline
		<#else>
			${durationFormatter(submission.assignment.closeDate, submission.submittedDate)} after close
		</#if>
		(<@fmt.date date=submission.assignment.submissionDeadline(user.userId) />)
	<#elseif assignment?has_content && user?has_content>
		<#local lateness = assignment.workingDaysLateIfSubmittedNow(user.userId) />
		<@fmt.p lateness "working day" /> overdue, the deadline/extension was ${durationFormatter(assignment.submissionDeadline(user.userId))}
		(<@fmt.date date=assignment.submissionDeadline(user.userId) />)
	</#if>
</#macro>

<#macro student_assignment_deadline info>
	<#local assignment = info.assignment />
	<#local extension = info.extension! />

	<#local time_remaining = durationFormatter(assignment.closeDate) />
	<#if info.hasActiveExtension>
		<#local extension_time_remaining = durationFormatter(extension.expiryDate) />
	</#if>

	<#if info.extended>
		<div class="extended deadline">
			<div class="time-remaining">${extension_time_remaining} <span class="label label-info">Extended</span></div>
			Extension granted until <@fmt.date date=extension.expiryDate />
		</div>
	<#elseif assignment.closed>
		<div class="late deadline">
			<#if info.hasActiveExtension>
				<#local latenesstooltip><@lateness assignment user info.submission /></#local>
				<div class="time-remaining">${extension_time_remaining} <span class="label label-warning use-tooltip" title="${latenesstooltip}" data-container="body">Late</span></div>
				Extension deadline was <@fmt.date date=extension.expiryDate />
			<#else>
				<#local latenesstooltip><@lateness assignment user info.submission /></#local>
				<div class="time-remaining">${time_remaining} <span class="label label-warning use-tooltip" title="${latenesstooltip}" data-container="body">Late</span></div>
				Deadline was <@fmt.date date=assignment.closeDate />
			</#if>
		</div>
	<#else>
		<div class="deadline">
			<div class="time-remaining">${time_remaining}</div>
			Deadline <@fmt.date date=assignment.closeDate />
		</div>
	</#if>
</#macro>

<#macro marker_assignment_list id title assignments verb="Mark" expand_by_default=true>
	<span id="${id}-container">
		<#local has_assignments = (assignments!?size gt 0) />
		<div id="${id}" class="striped-section marker-assignment-list<#if has_assignments> collapsible<#if expand_by_default> expanded</#if><#else> empty</#if>" data-name="${id}">
			<div class="clearfix">
				<h4 class="section-title">${title}</h4>

				<#if has_assignments>
					<div class="striped-section-contents">
						<div class="row">
							<div class="col-md-3">Details</div>
							<div class="col-md-3">Status</div>
							<div class="col-md-4">Progress</div>
							<div class="col-md-2">Actions</div>
						</div>

						<#list assignments as info>
							<span id="marker-assignment-container-${info.assignment.id}">
								<@marker_assignment_info info verb />
							</span>
						</#list>
					</div>
				</#if>
			</div>
		</div>
	</span>

	<#if !expand_by_default>
		<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
		<script type="text/javascript">
			GlobalScripts.initCollapsible(jQuery('#${id}').filter(':not(.empty)'));
		</script>
	</#if>
</#macro>

<#-- Progress bar for all students in a marking workflow  -->
<#macro stage_progress_bar stages>
	<div class="stage-progress-bar">
		<#list stages as stageInfo>
			<#local stage = stageInfo.stage />
			<#local state = 'default' />
			<#local icon = 'fa-circle-o' />
			<#if stageInfo.completed>
				<#local state = 'success' />
				<#local icon = 'fa-check-circle-o' />
			<#elseif stageInfo.started>
				<#local state = 'warning' />
				<#local icon = 'fa-dot-circle-o' />
			</#if>

			<#if stageInfo.progress?size == 1>
				<#local title><@workflowMessage stageInfo.progress[0].progress.messageCode /></#local>
			<#else>
				<#local title><#compress>
					<#list stageInfo.progress as progress>
						<@workflowMessage progress.progress.messageCode /> (<@fmt.p progress.count "student" />)<#if progress_has_next>, </#if>
					</#list>
				</#compress></#local>
			</#if>

			<#if stageInfo_index gt 0>
				<div class="bar bar-${state} use-tooltip" title="${title}" data-html="true" data-container="body"></div>
			</#if>
			<span class="fa-stack">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state} use-tooltip" title="${title}" data-html="true" data-container="body"></i>
			</span>
		</#list>
	</div>
</#macro>

<#-- Progress bar for a single student in a marking workflow  -->
<#macro individual_stage_progress_bar markerStages>
	<div class="stage-progress-bar">
		<#list markerStages as progress>
			<#local stage = progress.stage />

			<#local state = 'default' />
			<#local icon = 'fa-circle-o' />
			<#if progress.completed>
				<#local state = 'success' />
				<#local icon = 'fa-check-circle-o' />
			<#elseif progress.started>
				<#local state = 'warning' />
				<#local icon = 'fa-dot-circle-o' />
			</#if>

			<#local title><@workflowMessage progress.stage.actionCode /></#local>
			<#if progress_index gt 0>
				<div class="bar bar-${state} use-tooltip" title="${title}" data-html="true" data-container="body"></div>
			</#if>
			<#local title><@workflowMessage progress.messageCode /></#local>
			<span class="fa-stack">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state} use-tooltip" title="${title}" data-html="true" data-container="body"></i>
			</span>
		</#list>
	</div>
</#macro>

<#macro marker_assignment_info info verb="Mark">
	<#local assignment = info.assignment />
	<div class="item-info row marker-assignment-${assignment.id}">
		<div class="col-md-3">
			<div class="module-title"><@fmt.module_name assignment.module /></div>
			<h4 class="name">
				<#-- If the user can administer the assignment, link them to the admin page here -->
				<#if can.do("Module.ManageAssignments", assignment.module)>
					<a href="<@routes.cm2.depthome assignment.module assignment.academicYear/>" class="use-tooltip" title="Return to module management for <@fmt.module_name assignment.module false />">
						<span class="ass-name">${assignment.name}</span>
					</a>
				<#else>
					<span class="ass-name">${assignment.name}</span>
				</#if>
			</h4>
		</div>
		<div class="col-md-3">
			<ul class="list-unstyled">
				<#list info.currentStages as stage>
					<#if stage.progress?size == 1>
						<li><@workflowMessage stage.progress[0].progress.messageCode /></li>
					<#else>
						<#list stage.progress as progress>
							<li><@workflowMessage progress.progress.messageCode /> (<@fmt.p progress.count "student" />)</li>
						</#list>
					</#if>
				</#list>
			</ul>
		</div>
		<div class="col-md-4">
			<@stage_progress_bar info.stages />

			<#if info.feedbackDeadline??>
				<p>
					<strong>Student feedback due:</strong>
					<span class="use-tooltip" title="<@fmt.dateToWeek info.feedbackDeadline />" data-html="true"><@fmt.date date=info.feedbackDeadline includeTime=false /></span>
				</p>
			</#if>

			<#if info.extensionCount gt 0 || info.unsubmittedCount gt 0 || info.lateSubmissionsCount gt 0>
				<div class="row">
					<#if info.extensionCount gt 0>
						<div class="col-sm-6">
							<strong>Extensions:</strong> ${info.extensionCount}
						</div>
					</#if>

					<#if info.unsubmittedCount gt 0>
						<div class="col-sm-6">
							<strong>Not submitted:</strong> ${info.unsubmittedCount}
						</div>
					</#if>

					<#if info.lateSubmissionsCount gt 0>
						<div class="col-sm-6">
							<strong>Submitted late:</strong> ${info.lateSubmissionsCount}
						</div>
					</#if>
				</div>
			</#if>
		</div>
		<div class="col-md-2">
			<#if assignment.closed || assignment.openEnded>
				<a class="btn btn-block btn-primary" href="<@routes.cm2.listmarkersubmissions assignment user.apparentUser />">
					${verb}
				</a>
			</#if>
		</div>
	</div>
</#macro>

<#macro admin_assignment_list module assignments academicYear expand_by_default=true>
	<#local id>module-${module.code}</#local>
	<#local title><@fmt.module_name module /></#local>

	<span id="${id}-container">
		<#local has_assignments = (assignments!?size gt 0) />
		<div id="${id}" class="striped-section admin-assignment-list<#if has_assignments> collapsible<#if expand_by_default> expanded</#if><#else> empty</#if>" data-name="${id}"
			<#if has_assignments && !expand_by_default>
				 data-populate=".striped-section-contents"
				 data-href="<@routes.cm2.modulehome module academicYear />?${info.requestedUri.query!}"
				 data-name="${id}"
			</#if>
		>
			<div class="clearfix">
				<div class="btn-group section-manage-button">
					<a class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">Manage this module <span class="caret"></span></a>
					<ul class="dropdown-menu pull-right">
						<li>
							<#local perms_url><@routes.admin.moduleperms module /></#local>
							<@fmt.permission_button
								permission='RolesAndPermissions.Read'
								scope=module
								action_descr='manage module permissions'
								href=perms_url>
									Module permissions
							</@fmt.permission_button>
						</li>
						<li>
						<#local create_url><@routes.cm2.createassignmentdetails module academicYear /></#local>
							<@fmt.permission_button
								permission='Assignment.Create'
								scope=module
								action_descr='create a new assignment'
								href=create_url>
									Create new assignment
							</@fmt.permission_button>
						</li>
						<li>
							<#local copy_url><@routes.cm2.copy_assignments_previous_module module academicYear /></#local>
							<@fmt.permission_button
								permission='Assignment.Create'
								scope=module
								action_descr='copy existing assignments'
								href=copy_url>
									Create new assignment from previous
							</@fmt.permission_button>
						</li>
					</ul>
				</div>

				<h4 class="section-title with-button">${title}</h4>

				<#if has_assignments>
					<div class="striped-section-contents">
						<#if expand_by_default>
							<#list assignments as info>
								<span id="admin-assignment-container-${info.assignment.id}">
									<@admin_assignment_info info />
								</span>
							</#list>
						</#if>
					</div>
				</#if>
			</div>
		</div>
	</span>

	<#if !expand_by_default>
		<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
		<script type="text/javascript">
			GlobalScripts.initCollapsible(jQuery('#${id}').filter(':not(.empty)'));
		</script>
	</#if>
</#macro>

<#macro admin_assignment_info info>
	<#local assignment = info.assignment />
	<div class="item-info admin-assignment-${assignment.id}">
		<div class="clearfix">
			<div class="pull-right">
				<#if assignment.cm2Assignment>
					<#local edit_url><@routes.cm2.editassignmentdetails assignment /></#local>
				<#else>
					<#local edit_url><@routes.coursework.assignmentedit assignment /></#local>
				</#if>
				<@fmt.permission_button
					classes='btn btn-default btn-xs'
					permission='Assignment.Update'
					scope=assignment
					action_descr='edit assignment properties'
					href=edit_url>
					Edit assignment
				</@fmt.permission_button>
			</div>

			<h5 class="assignment-name">
				<#if assignment.collectSubmissions>
					<#local sub_caption="Manage assignment's submissions" />
				<#else>
					<#local sub_caption="Manage assignment's feedback" />
				</#if>
				<#local edit_url><@routes.cm2.assignmentsubmissionsandfeedback assignment /></#local>
				<@fmt.permission_button
					permission='AssignmentFeedback.Read'
					scope=assignment
					action_descr=sub_caption?lower_case
					href=edit_url>
						${assignment.name}
				</@fmt.permission_button>
			</h5>
		</div>

		<div class="row">
			<div class="col-md-4">
				<h6 class="sr-only">Assignment information</h6>

				<ul class="list-unstyled">
					<#if !assignment.opened>
						<li><strong>Assignment opens:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />" data-html="true"><@fmt.date date=assignment.openDate /></span></li>
					</#if>

					<#if assignment.openEnded>
						<li><strong>Open-ended</strong></li>
					<#else>
						<li><strong>Assignment <#if assignment.closed>closed<#else>due</#if>:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.closeDate />" data-html="true"><@fmt.date date=assignment.closeDate /></span></li>
					</#if>

					<li>
						<strong>Assigned students:</strong>
						<#if assignment.membershipInfo.totalCount == 0>
							0
						<#elseif assignment.membershipInfo.sitsCount gt 0>
							${assignment.membershipInfo.sitsCount} from SITS<#--
							--><#if assignment.membershipInfo.usedExcludeCount gt 0> after ${assignment.membershipInfo.usedExcludeCount} removed manually</#if><#--
							--><#if assignment.membershipInfo.usedIncludeCount gt 0>, ${assignment.membershipInfo.usedIncludeCount} added manually</#if>
						<#else>
							${assignment.membershipInfo.usedIncludeCount} added manually
						</#if>
					</li>

					<#if (assignment.markingWorkflow.markingMethod)??>
						<li><strong>Workflow type:</strong> ${assignment.markingWorkflow.markingMethod.description}</li>
					</#if>

					<#if assignment.feedbackDeadline??>
						<li><strong>Feedback due:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.feedbackDeadline />" data-html="true"><@fmt.date date=assignment.feedbackDeadline includeTime=false /></span></li>
					</#if>
				</ul>

				<#if assignment.collectSubmissions || assignment.extensionsPossible>
					<ul class="list-unstyled">
						<#if assignment.collectSubmissions>
							<li><strong>Submissions received:</strong> ${assignment.submissions?size}</li>
							<li><strong>Late submissions:</strong> ${assignment.lateSubmissionCount}</li>
						</#if>

						<#if assignment.extensionsPossible>
							<li>
								<#if can.do('Extension.Update', assignment)>
									<#local ext_caption="Manage assignment's extensions" />
								<#else>
									<#local ext_caption="View assignment's extensions" />
								</#if>
								<#local ext_url><@routes.cm2.assignmentextensions assignment /></#local>
								<@fmt.permission_button
									permission='Extension.Read'
									scope=assignment
									action_descr=ext_caption?lower_case
									href=ext_url>
										<strong>Extension requests:</strong> ${assignment.countUnapprovedExtensions}
								</@fmt.permission_button>
							</li>
						</#if>
					</ul>
				</#if>

				<ul class="list-unstyled">
					<li><a href="<@routes.cm2.assignment assignment />">Link for students</a></li>
				</ul>
			</div>
			<#if info.stages??>
				<div class="col-md-4">
					<h6>Progress</h6>

					<ul class="list-unstyled">
						<li><strong>Created:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.createdDate />" data-html="true"><@fmt.date date=assignment.createdDate /></span></li>

						<#if assignment.opened>
							<li><strong>Opened:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />" data-html="true"><@fmt.date date=assignment.openDate /></span></li>
						</#if>

						<#if !assignment.openEnded && assignment.closed>
							<li><strong>Closed:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek assignment.closeDate />" data-html="true"><@fmt.date date=assignment.closeDate /></span></li>
						</#if>

						<#list info.stages as stage>
							<li>
								<strong><@workflowMessage stage.stage.actionCode /></strong>:
								<#if stage.progress?size == 1>
									<@workflowMessage stage.progress[0].progress.messageCode /> (<@fmt.p stage.progress[0].count "student" />)
								<#else>
									<ul>
										<#list stage.progress as progress>
											<li><@workflowMessage progress.progress.messageCode /> (<@fmt.p progress.count "student" />)</li>
										</#list>
									</ul>
								</#if>
							</li>
						</#list>
					</ul>
				</div>
			</#if>
			<#if info.nextStages??>
				<div class="col-md-4">
					<h6>Next steps</h6>

					<ul class="list-unstyled">
						<#if info.nextStages?size == 0>
							<#if !assignment.opened>
								<#-- Not open yet -->
								<li>Not open yet</li>
							<#elseif !assignment.openEnded && !assignment.closed>
								<#-- Not closed yet -->
								<li>Not closed yet</li>
							<#else>
								<#-- Complete? -->
								<#if assignment.hasReleasedFeedback>
									<li><a href="<@routes.cm2.assignmentAudit assignment />">View audit</a></li>
								<#else>
									<li>Awaiting feedback</li>
								</#if>
							</#if>
						<#else>
							<#list info.nextStages as nextStage>
								<li>
									<#local nextStageDescription><@workflowMessage nextStage.stage.actionCode /> (<@fmt.p nextStage.count "student" />)</#local>
									<#if nextStage.url??>
										<a href="${nextStage.url}">${nextStageDescription}</a>
									<#else>
										${nextStageDescription}
									</#if>
								</li>
							</#list>
						</#if>
					</ul>
				</div>
			</#if>
		</div>
	</div>
</#macro>

<#macro workflowMessage code><#compress>
	<#local text><@spring.message code=code /></#local>
	${(text!"")?replace("[STUDENT]", "student")?replace("[FIRST_MARKER]", "first marker")?replace("[SECOND_MARKER]", "second marker")}
</#compress></#macro>

<#-- Common template parts for use in other submission/coursework templates. -->
<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />

<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
<div id="tip-content-${attachment.id}" class="hide">
	<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
	<p class="similarity-subcategories-tooltip">
		Web: ${r.webOverlap}%<br>
		Student papers: ${r.studentOverlap}%<br>
		Publications: ${r.publicationOverlap}%
	</p>
	<p>
		<#if r.turnitinId?has_content>
			<a target="turnitin-viewer" href="<@routes.cm2.turnitinLtiReport assignment attachment />">View full report</a>
		<#else>
			<a target="turnitin-viewer" href="<@routes.cm2.turnitinReport assignment attachment />">View full report - available via Tabula until end of August 2016</a>
		</#if>
	</p>
</div>
<script type="text/javascript">
	jQuery(function($){
		$("#tool-tip-${attachment.id}").popover({
			placement: 'right',
			html: true,
			content: function(){return $('#tip-content-${attachment.id}').html();},
			title: 'Turnitin report summary'
		});
	});
</script>
</#macro>

<#macro workflow_stage stage>
	<#if stage.messageCode!?length gt 0>
		<li class="stage<#if !stage.completed> incomplete<#if !stage.preconditionsMet> preconditions-not-met</#if></#if><#if stage.started && !stage.completed> current</#if>">
			<#local state = 'default' />
			<#local icon = 'fa-circle-o' />
			<#local title = 'Not started yet' />
			<#if stage.completed>
				<#local state = 'success' />
				<#local icon = 'fa-check-circle-o' />
				<#local title = 'Completed' />
			<#elseif stage.started>
				<#local state = 'warning' />
				<#local icon = 'fa-dot-circle-o' />
				<#local title = 'Current stage' />
			<#elseif !stage.preconditionsMet>
				<#local title = 'Preconditions not met' />
			</#if>

			<span class="fa-stack">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state} use-tooltip" title="${title}" data-html="true" data-container="body"></i>
			</span>

			<#local content><#nested/></#local>

			<strong><@spring.message code=stage.messageCode /></strong><#if content?has_content>: ${content}</#if>
		</li>
	</#if>
</#macro>

<#macro uniIdSafeMarkerLink marker role>
	<#if marker.warwickId?has_content>
		- <a href="<@routes.coursework.listmarkersubmissions assignment marker />">Proxy as this ${role}</a>
	<#else>
		- Cannot proxy as this marker as they have no University ID
	</#if>
</#macro>

<#macro uniIdSafeCM2MarkerLink stage marker student>
	<#if marker.warwickId?has_content>
	- <a href="<@routes.cm2.markerOnlineFeedback assignment stage marker student />">Proxy</a>
	<#else>
	- Cannot proxy as this marker as they have no University ID
	</#if>
</#macro>

<#macro student_workflow_details student>
	<#if student.coursework.enhancedSubmission??>
		<#local enhancedSubmission=student.coursework.enhancedSubmission>
		<#local submission=enhancedSubmission.submission>
	</#if>
	<#if student.coursework.enhancedFeedback??>
		<#local enhancedFeedback=student.coursework.enhancedFeedback>
		<#local feedback=enhancedFeedback.feedback>
	</#if>
	<#if student.coursework.enhancedExtension??>
		<#local enhancedExtension=student.coursework.enhancedExtension>
		<#local extension=enhancedExtension.extension>
	</#if>

	<ul class="list-unstyled stage-group">
		<#list student.stages?keys as stage_name>
			<@workflow_stage student.stages[stage_name]><#compress>
				<#if stage_name == 'Submission'>
					<@submission_details submission />
				<#elseif stage_name == 'CheckForPlagiarism'>
					<#if submission??>
						<@fmt.p submission.allAttachments?size "file" />
						<#list submission.allAttachments as attachment>
							<#if attachment.originalityReportReceived>
								<@components.originalityReport attachment />
							</#if>
							<#if can.do("Submission.ViewUrkundPlagiarismStatus", submission) && attachment.urkundResponseReceived>
								<@components.urkundOriginalityReport attachment />
							</#if>
						</#list>
					</#if>
				<#elseif stage_name == 'CM1FirstMarking'>
					<#local fm = assignment.getStudentsFirstMarker(student.user.userId)!"" />
					<#if fm?has_content>
						<#local firstMarker><span data-profile="${fm.warwickId!}">${fm.fullName}</span></#local>
					</#if>

					<#if firstMarker!?length gt 0>
						(${firstMarker})
						<#if can.do("Assignment.MarkOnBehalf", assignment)>
							<@uniIdSafeMarkerLink fm "marker" />
						</#if>
					</#if>
				<#elseif stage_name == 'CM1SecondMarking'>
					<#local sm = assignment.getStudentsSecondMarker(student.user.userId)!"" />
					<#if sm?has_content>
						<#local secondMarker><span data-profile="${sm.warwickId!}">${sm.fullName}</span></#local>
					</#if>

					<#if secondMarker!?length gt 0>
						(${secondMarker})
						<#if can.do("Assignment.MarkOnBehalf", assignment)>
							<@uniIdSafeMarkerLink sm "marker" />
						</#if>
					</#if>
				<#elseif stage_name == 'CM1Moderation'>
					<#local sm = assignment.getStudentsSecondMarker(student.user.userId)!"" />
					<#if sm?has_content>
						<#local secondMarker><span data-profile="${sm.warwickId!}">${sm.fullName}</span></#local>
					</#if>

					<#if secondMarker!?length gt 0>
						(${secondMarker})
						<#if can.do("Assignment.MarkOnBehalf", assignment)>
							<@uniIdSafeMarkerLink sm "moderator" />
						</#if>
					</#if>
				<#elseif stage_name == 'CM1FinaliseSeenSecondMarking'>
					<#local fm = assignment.getStudentsFirstMarker(student.user.userId)!"" />
					<#if fm?has_content>
						<#local firstMarker><span data-profile="${fm.warwickId!}">${fm.fullName}</span></#local>
					</#if>

					<#if firstMarker!?length gt 0>
						(${firstMarker})
						<#if can.do("Assignment.MarkOnBehalf", assignment)>
							<@uniIdSafeMarkerLink fm "marker" />
						</#if>
					</#if>
				<#elseif stage_name == 'CM2ReleaseForMarking'>

				<#elseif assignment.cm2Assignment && student.stages[stage_name].stage.markingRelated>
					<#if feedback??>
						<#local markingStage = student.stages[stage_name].stage.markingStage />
						<#local marker = mapGet(feedback.feedbackMarkers, markingStage)! />

						<#if marker?has_content>
							${marker.fullName}
							<#if can.do("Assignment.MarkOnBehalf", assignment)>
								<@uniIdSafeCM2MarkerLink markingStage marker student.user />
							</#if>
						<#else>
							Not assigned
						</#if>
					</#if>
				<#elseif stage_name == 'AddMarks'>
					<#if feedback?? && feedback.hasMarkOrGrade>
						<#if feedback.hasMark>
							${feedback.actualMark!''}%<#if feedback.hasGrade>,</#if>
						</#if>
						<#if feedback.hasGrade>
							grade ${feedback.actualGrade!''}
						</#if>

						<#if feedback.hasPrivateOrNonPrivateAdjustments>
							Marks adjusted:
							<#if feedback.latestMark??>${feedback.latestMark}%</#if><#if feedback.latestGrade??>,</#if>
							<#if feedback.latestGrade??> grade ${feedback.latestGrade}</#if>
							<#if feedback.latestPrivateOrNonPrivateAdjustment?? && feedback.latestPrivateOrNonPrivateAdjustment.reason??>
								- Reason for adjustment: ${feedback.latestPrivateOrNonPrivateAdjustment.reason!''}
							</#if>
						</#if>
					</#if>
				<#elseif stage_name == 'AddFeedback'>
					<#if feedback?? && (feedback.hasAttachments || feedback.hasOnlineFeedback)>
						<#local attachments=feedback.attachments />
						<#if attachments?size gt 0>
							<a class="long-running" href="<@routes.cm2.assignmentFeedbackZip assignment />">
								<@fmt.p attachments?size "file" />
							</a>
							uploaded
						<#-- If the feedback was entered online there may not be attachments  -->
						<#elseif feedback?? && feedback.hasOnlineFeedback>
							Comments entered online
						</#if>
						<#if feedback.updatedDate??>
							<@fmt.date date=feedback.updatedDate seconds=true capitalise=true shortMonth=true />
						</#if>
					</#if>
				<#elseif stage_name == 'ReleaseFeedback'>
					<#if feedback?? && feedback.releasedDate??>
						<@fmt.date date=feedback.releasedDate seconds=true capitalise=true shortMonth=true />
					</#if>

					<#if !student.stages?keys?seq_contains('AddFeedback') && feedback?? && feedback.hasContent>
						<#local attachments=feedback.attachments />
						<#if attachments?size gt 0>
							<a class="long-running" href="<@routes.cm2.assignmentFeedbackZip assignment />">
								<@fmt.p attachments?size "file" />
							</a>
							uploaded
						<#-- If the feedback was entered online there may not be attachments  -->
						<#elseif feedback?? && feedback.hasOnlineFeedback>
							Comments entered online
						<#elseif feedback?? && feedback.hasMark>
							Marks added
						<#elseif feedback?? && feedback.hasGrade>
							Grade added
						</#if>
						<#if feedback.updatedDate??>
							<@fmt.date date=feedback.updatedDate seconds=true capitalise=true shortMonth=true />
						</#if>
					</#if>

					<#if feedback?? && feedback.hasContent>
						<ul class="list-unstyled">
							<li>
								<span class="fa-stack"></span>
								<a href="<@routes.cm2.feedbackSummary assignment student.user.userId!''/>"
									 class="ajax-modal"
									 data-target="#feedback-modal">
									View feedback
								</a>
							</li>
							<li>
								<span class="fa-stack"></span>
								<a href="<@routes.cm2.feedbackAudit assignment student.user.userId!''/>">
									View audit
								</a>
							</li>
							<#local queueSitsUploadEnabled=(features.queueFeedbackForSits && department.uploadCourseworkMarksToSits) />
							<#if queueSitsUploadEnabled>
								<li>
									<span class="fa-stack"></span>
									<#if enhancedFeedback.feedbackForSits??>
										<#local feedbackSitsStatus=enhancedFeedback.feedbackForSits.status />
										<#local sitsWarning = feedbackSitsStatus.dateOfUpload?has_content && feedbackSitsStatus.status.code != "uploadNotAttempted" && (
											(feedbackSitsStatus.actualMarkLastUploaded!0) != (student.enhancedFeedback.feedback.latestMark!0) || (feedbackSitsStatus.actualGradeLastUploaded!"") != (student.enhancedFeedback.feedback.latestGrade!"")
										) />
										<#if feedbackSitsStatus.code == "failed">
											<a href="<@routes.cm2.checkSitsUpload enhancedFeedback.feedback />" target="_blank">
												<span style="cursor: pointer;" class="label label-danger use-tooltip" title="There was a problem uploading to SITS. Click to try and diagnose the problem.">
													${feedbackSitsStatus.description}
												</span><#--
											--></a>
										<#elseif sitsWarning>
											<span class="label label-danger use-tooltip" title="The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.">
												${feedbackSitsStatus.description}
											</span>
										<#elseif feedbackSitsStatus.code == "successful">
											<span class="label label-success">${feedbackSitsStatus.description}</span>
										<#else>
											<span class="label label-info">${feedbackSitsStatus.description}</span>
										</#if>
									<#else>
										<span class="label label-info">Not queued for SITS upload</span>
									</#if>
								</#if>
							</li>
						</ul>
					</#if>
				</#if>
			</#compress></@workflow_stage>
		</#list>
	</ul>
</#macro>

<#macro marker_feedback_summary feedback stage currentStage=[] currentFeedback=[]>
	<h4>${stage.description} <#if feedback.marker??>- ${feedback.marker.fullName}</#if></h4>

	<#list feedback.customFormValues as formValue>
		<#if formValue.value?has_content>
			<@bs3form.form_group><textarea class="form-control feedback-comments" readonly="readonly">${formValue.value!""}</textarea></@bs3form.form_group>
		<#else>
		<p>No feedback comments added.</p>
		</#if>
	</#list>

	<div class="row form-inline">
		<#if feedback.mark?has_content || feedback.grade?has_content>
			<div class="col-xs-3">
				<label>Mark</label>
				<div class="input-group">
					<input type="text" class="form-control" readonly="readonly" value="${feedback.mark!""}">
					<div class="input-group-addon">%</div>
				</div>
			</div>

			<div class="col-xs-3">
				<label>Grade</label>
				<input type="text" class="form-control" readonly="readonly" value="${feedback.grade!""}">
			</div>
		<#else>
			<div class="col-xs-6"><span>No mark or grade added.</span></div>
		</#if>

		<div class="col-xs-3">
		<#-- Download a zip of all feedback or just a single file if there is only one -->
			<#if feedback.attachments?has_content >
				<#local attachment = "" />
				<#if !feedback.attachments?is_enumerable>
				<#-- assume it's a FileAttachment -->
					<#local attachment = feedback.attachments />
				<#elseif feedback.attachments?size == 1>
				<#-- take the first and continue as above -->
					<#local attachment = feedback.attachments?first />
				</#if>
				<#if attachment?has_content>
					<#local downloadUrl><@routes.cm2.downloadMarkerFeedbackOne assignment feedback.marker feedback attachment /></#local>
				<#elseif feedback.attachments?size gt 1>
					<#local downloadUrl><@routes.cm2.downloadMarkerFeedbackAll assignment feedback.marker feedback stage.description+" feedback" /></#local>
				</#if>
				<a class="btn btn-default long-running use-tooltip" href="${downloadUrl}">Download feedback</a>
				<ul class="feedback-attachments hide">
					<#list feedback.attachments as attachment>
						<li id="attachment-${attachment.id}" class="attachment">
							<span>${attachment.name}</span>&nbsp;<a href="#" class="remove-attachment">Remove</a>
							<input type="hidden" name="attachedFiles" value="${attachment.id}" />
						</li>
					</#list>
				</ul>
			</#if>
		</div>
		<div class="col-xs-3">
			<#if currentFeedback?? && currentFeedback?has_content>
				<#if currentStage?? && currentStage.populateWithPreviousFeedback>
					<div class="form-group">
						<label class="radio-inline"><input type="radio" name="changesState" <#if !currentFeedback.hasBeenModified>checked</#if> value="approve" />Approve</label>
						<label class="radio-inline"><input type="radio" name="changesState" <#if currentFeedback.hasBeenModified>checked</#if> value="make-changes" >Make changes</label>
					</div>
				<#else>
					<a class="copy-feedback btn btn-default long-running use-tooltip" href="#">Copy comments and files</a>
				</#if>
			</#if>
		</div>
	</div>
</#macro>

<#macro lateness submission="" assignment="" user=""><#compress>
	<#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
		<#if submission.late>
			<@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(submission.deadline, submission.submittedDate)} after deadline
		<#else>
			${durationFormatter(submission.assignment.closeDate, submission.submittedDate)} after close
		</#if>
	<#elseif assignment?has_content && user?has_content>
		<#local lateness = assignment.workingDaysLateIfSubmittedNow(user.userId) />
		<@fmt.p lateness "working day" /> overdue, the deadline/extension was ${durationFormatter(assignment.submissionDeadline(user.userId))}
	</#if>
</#compress></#macro>

<#macro extensionLateness extension submission><#compress>
	<#if extension?has_content && extension.expiryDate?? && submission.late>
		<@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(extension.expiryDate, submission.submittedDate)} after extended deadline (<@fmt.date date=extension.expiryDate capitalise=false shortMonth=true stripHtml=true />)
	</#if>
</#compress></#macro>

<#macro submission_details submission=[]><@compress single_line=true>
	<#if submission?has_content>
		<#local attachments = submission.allAttachments />
		<#local assignment = submission.assignment />
		<#local module = assignment.module />

		<#if submission.submittedDate??>
			<span class="date use-tooltip" title="<@lateness submission />" data-container="body">
				<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true />
			</span>
		</#if>

		<#if attachments?size gt 0>
			<#if attachments?size == 1>
				<#local filename = "${attachments[0].name}">
				<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/>?single=true</#local>
			<#else>
				<#local filename = "submission-${submission.studentIdentifier}.zip">
				<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/></#local>
			</#if>
			&emsp;<a class="long-running" href="${downloadUrl}">Download submission</a>
		</#if>
	</#if>
</@compress></#macro>

<#macro submission_status submission="" enhancedExtension="" enhancedFeedback="" student="">
	<#if submission?has_content>
		<#if submission.late>
			<#if enhancedExtension?has_content && enhancedExtension.extension.approved>
				<span class="label label-danger use-tooltip" title="<@extensionLateness enhancedExtension.extension submission/>" data-container="body">Late</span>
			<#else>
				<span class="label label-danger use-tooltip" title="<@lateness submission />" data-container="body">Late</span>
			</#if>
		<#elseif submission.authorisedLate>
			<span class="label label-info use-tooltip" data-html="true" title="Extended until <@fmt.date date=enhancedExtension.extension.expiryDate capitalise=false shortMonth=true />" data-container="body">Within Extension</span>
		</#if>
		<#if features.disabilityOnSubmission && student.disability??>
			<a class="use-popover cue-popover" id="popover-disability" data-html="true"
			   data-original-title="Disability disclosed"
			   data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${student.disability.code}</h6><small>${(student.disability.sitsDefinition)!}</small></div>"
			>
				<span class="label label-info">Disability disclosed</span>
			</a>
		</#if>
	<#elseif !enhancedFeedback?has_content>
		<span class="label label-info">Unsubmitted</span>
		<#if enhancedExtension?has_content>
			<#local extension=enhancedExtension.extension>
			<#if extension.approved && !extension.rejected>
				<#local date>
					<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true stripHtml=true />
				</#local>
			</#if>
			<#if enhancedExtension.within>
				<span class="label label-info use-tooltip" data-html="true" title="${date}" data-container="body">Within Extension</span>
			<#elseif extension.rejected>
				<span class="label label-info">Extension Rejected</span>
			<#elseif !extension.approved>
				<span class="label label-info">Extension Requested</span>
			<#else>
				<span class="label label-info use-tooltip" title="${date}" data-container="body">Extension Expired</span>
			</#if>
		</#if>
	</#if>
</#macro>

<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />

	<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
	<div id="tip-content-${attachment.id}" class="hide">
		<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
		<p class="similarity-subcategories-tooltip">
			Web: ${r.webOverlap}%<br>
			Student papers: ${r.studentOverlap}%<br>
			Publications: ${r.publicationOverlap}%
		</p>
		<p>
			<a target="turnitin-viewer" href="<@routes.cm2.turnitinLtiReport assignment attachment />">View full report</a>
		</p>
	</div>
	<script type="text/javascript">
		jQuery(function($){
			$("#tool-tip-${attachment.id}").popover({
				placement: 'right',
				html: true,
				content: function(){return $('#tip-content-${attachment.id}').html();},
				title: 'Turnitin report summary'
			});
		});
	</script>
</#macro>

<#macro autoGradeOnline gradePath gradeLabel markPath markingId generateUrl>
	<@bs3form.labelled_form_group path=gradePath labelText=gradeLabel>
		<@f.input path="${gradePath}" cssClass="form-control input-sm auto-grade" id="auto-grade-${markingId}" />
		<select name="${gradePath}" class="form-control input-sm" disabled style="display:none;"></select>
		<@fmt.help_popover id="auto-grade-${markingId}-help" content="The grades available depends on the mark entered and the SITS mark scheme in use" />
	</@bs3form.labelled_form_group>

<script>
	jQuery(function($){
		var $gradeInput = $('#auto-grade-${markingId}').hide()
			, $markInput = $gradeInput.closest('form').find('input[name=${markPath}]')
			, $select = $gradeInput.closest('div').find('select').on('click', function(){
			$(this).closest('.control-group').removeClass('info');
		})
			, currentRequest = null
			, data = {'studentMarks': {}}
			, doRequest = function(){
			if (currentRequest != null) {
				currentRequest.abort();
			}
			data['studentMarks']['${markingId}'] = $markInput.val();
			if ($select.is(':visible') || $gradeInput.val().length > 0) {
				data['selected'] = {};
				data['selected']['${markingId}'] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
			}
			currentRequest = $.ajax('${generateUrl}',{
				'type': 'POST',
				'data': data,
				success: function(data) {
					$select.html(data);
					if ($select.find('option').length > 1) {
						$gradeInput.hide().prop('disabled', true);
						$select.prop('disabled', false).show()
							.closest('.control-group').addClass('info');
						$('#auto-grade-${markingId}-help').show();
					} else {
						$gradeInput.show().prop('disabled', false);
						$select.prop('disabled', true).hide();
						$('#auto-grade-${markingId}-help').hide();
					}
				}, error: function(xhr, errorText){
					if (errorText != "abort") {
						$gradeInput.show().prop('disabled', false);
						$('#auto-grade-${markingId}-help').hide();
					}
				}
			});
		};
		$markInput.on('keyup', doRequest);
		doRequest();
	});
</script>
</#macro>

<#macro marksForm assignment templateUrl formUrl commandName cancelUrl generateUrl seatNumberMap="" showAddButton=true>
<div id="batch-feedback-form">
	<h1>Submit marks for ${assignment.name}</h1>
	<ul id="marks-tabs" class="nav nav-tabs">
		<li class="active"><a href="#upload">Upload</a></li>
		<li class="webform-tab"><a href="#webform">Web Form</a></li>
	</ul>
	<div class="tab-content">
		<div class="tab-pane active" id="upload">
			<p>
				You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
				The spreadsheet should have at least two column headings: <b>University ID</b> and <b>Mark</b>.
				You can use this <a href="${templateUrl}" >generated spreadsheet</a> as a template.
				Note that you can upload just marks, or marks and grades.
			</p>
			<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
				<input name="isfile" value="true" type="hidden"/>
				<table role="presentation" class="narrowed-form">
					<tr>
						<td id="multifile-column">
							<h3>Select file</h3>
							<@bs3form.filewidget
								basename="file"
								labelText="Files"
								types=[]
								multiple=true
							/>
						</td>
					</tr>
				</table>
				<div class="submit-buttons">
					<button class="btn btn-primary btn-large">Upload</button>
				</div>
			</@f.form>
		</div>
		<div class="tab-pane " id="webform">
			<#if showAddButton>
				<p>
					Click the add button below to enter marks for a student.
				</p>

				<table class="hide">
					<tbody class="row-markup">
					<tr class="mark-row">
						<#if seatNumberMap?has_content>
							<td></td>
						</#if>
						<td>
							<div class="input-prepend input-append">
								<span class="add-on"><i class="fa fa-user"></i></span>
								<input class="universityId form-control" name="universityId" type="text" />
							</div>
						</td>
						<td>
							<div class="input-append">
								<input name="actualMark" type="text" class="form-control" />
								<span class="add-on">%</span>
							</div>
						<td>
							<input class="grade form-control input-sm" name="actualGrade" type="text"/>
							<#if isGradeValidation>
								<select name="actualGrade" class="form-control input-sm" disabled style="display:none;"></select>
							</#if>
						</td>
					</tr>
					</tbody>
				</table>
			</#if>
			<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
				<div class="fix-area">
					<input name="isfile" value="false" type="hidden"/>
					<table class="marksUploadTable">
						<tr class="mark-header">
							<#if seatNumberMap?has_content>
								<th>Seat order</th>
							</#if>
							<th>University ID</th>
							<#if studentMarkerMap?has_content>
								<th>Marker</th>
							</#if>
							<th>Marks</th>
							<th>Grade <#if isGradeValidation><@fmt.help_popover id="auto-grade-help" content="The grade is automatically calculated from the SITS mark scheme" /></#if></th>
						</tr>
						<#if marksToDisplay??>
							<#list marksToDisplay as markItem>
								<#if markItem.universityId??>
									<tr class="mark-row">
										<#if seatNumberMap?has_content>
											<#if mapGet(seatNumberMap, markItem.usercode)??>
												<td>${mapGet(seatNumberMap, markItem.usercode)}</td>
											<#else>
												<td></td>
											</#if>
										</#if>
										<td>
											<div class="input-prepend input-append">
												<span class="add-on"><i class="fa fa-user"></i></span>
												<input class="universityId form-control" value="${markItem.universityId}" name="marks[${markItem_index}].universityId" type="text" readonly="readonly" />
											</div>
										</td>
										<#if studentMarkerMap?has_content>
											<#if mapGet(studentMarkerMap, markItem.usercode)??>
												<td>${mapGet(studentMarkerMap, markItem.usercode)}</td>
											<#else>
												<td></td>
											</#if>
										</#if>
										<td>
											<div class="input-append">
												<input name="marks[${markItem_index}].actualMark" class="mark form-control" value="<#if markItem.actualMark??>${markItem.actualMark}</#if>" type="text" />
												<span class="add-on">%</span>
											</div>
										</td>
										<td>
											<input name="marks[${markItem_index}].actualGrade" class="grade form-control input-sm" value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" />
											<#if isGradeValidation>
												<select name="marks[${markItem_index}].actualGrade" class="form-control input-sm" disabled style="display:none;"></select>
											</#if>
										</td>
									</tr>
								</#if>
							</#list>
						</#if>
					</table>
					<#if showAddButton>
						<br /><button class="add-additional-marks btn btn-default">Add</button>
					</#if>
					<div class="submit-buttons fix-footer">
						<input type="submit" class="btn btn-primary" value="Save">
						or <a href="${cancelUrl}" class="btn btn-default">Cancel</a>
					</div>
				</div>
			</@f.form>
		</div>
	</div>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		// Fire a resize to get the fixed button in the right place
		$('.webform-tab').on('shown', function(){
			$(window).trigger('resize');
		});

		if (${isGradeValidation?string('true','false')}) {
			var currentRequest = null, doIndividualRequest = function() {
				if (currentRequest != null) {
					currentRequest.abort();
				}
				var data = {'studentMarks': {}, 'selected': {}}
					, $this = $(this)
					, $markRow = $this.closest('tr')
					, $gradeInput = $markRow.find('input.grade')
					, $select = $markRow.find('select')
					, universityId = $markRow.find('input.universityId').val();
				data['studentMarks'][universityId] = $this.val();
				if ($select.is(':visible') || $gradeInput.val().length > 0) {
					data['selected'] = {};
					data['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
				}
				currentRequest = $.ajax('${generateUrl}',{
					'type': 'POST',
					'data': data,
					success: function(data) {
						$select.html(data);
						if ($select.find('option').length > 1 || $this.val() == "") {
							$gradeInput.hide().prop('disabled', true);
							$select.prop('disabled', false).show()
						} else {
							$gradeInput.show().prop('disabled', false);
							$select.prop('disabled', true).hide();
						}
					}, error: function(xhr, errorText){
						if (errorText != "abort") {
							$gradeInput.show().prop('disabled', false);
						}
					}
				});
			};

			$('.marksUploadTable').on('keyup', 'input[name*="actualMark"]', doIndividualRequest).on('tableFormNewRow', function(){
				// Make sure all the selects have the correct name
				$('.marksUploadTable .mark-row select').each(function(){
					$(this).prop('name', $(this).closest('td').find('input').prop('name'));
				});
			});

			var currentData = {'studentMarks': {}, 'selected': {}};
			var $markRows = $('.marksUploadTable .mark-row').each(function(){
				var $markRow = $(this)
					, universityId = $markRow.find('input.universityId').val()
					, $select = $markRow.find('select')
					, $gradeInput = $markRow.find('input.grade');
				currentData['studentMarks'][universityId] = $markRow.find('input[name*="actualMark"]').val();
				currentData['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
				$gradeInput.hide();
			});
			$.ajax('${generateUrl}/multiple',{
				'type': 'POST',
				'data': currentData,
				success: function(data) {
					var $selects = $(data);
					$markRows.each(function(){
						var $markRow = $(this)
							, universityId = $markRow.find('input.universityId').val()
							, $thisSelect = $selects.find('select').filter(function(){
							return $(this).data('universityid') == universityId;
						});
						if ($thisSelect.length > 0 && ($thisSelect.find('option').length > 1 || $markRow.find('input.mark').val() == "")) {
							$markRow.find('input.grade').hide().prop('disabled', true);
							$markRow.find('select').html($thisSelect.html()).prop('disabled', false).show();
						} else {
							$markRow.find('input.grade').show().prop('disabled', false);
							$markRow.find('select').prop('disabled', true).hide();
						}

					});
				}
			});
		}
	});
</script>
</#macro>

<#macro feedbackGradeValidation isGradeValidation gradeValidation>
	<#local gradeValidationClass><#compress>
		<#if isGradeValidation>
			<#if gradeValidation.invalid?has_content || gradeValidation.zero?has_content>error<#elseif gradeValidation.populated?has_content>info</#if>
		<#else>
			<#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content>error</#if>
		</#if>
	</#compress></#local>

	<#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content || gradeValidation.zero?has_content>
		<#if isGradeValidation>
		<div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
			<#if gradeValidation.invalid?has_content>
				<#local total = gradeValidation.invalid?keys?size />
				<p>
					<a href="#grade-validation-invalid-modal" data-toggle="modal"><@fmt.p total "student" /></a>
					<#if total==1>
						has feedback with a grade that is invalid. It will not be uploaded.
					<#else>
						have feedback with grades that are invalid. They will not be uploaded.
					</#if>
				</p>
			</#if>
			<#if gradeValidation.zero?has_content>
				<#local total = gradeValidation.zero?keys?size />
				<p>
					<a href="#grade-validation-zero-modal" data-toggle="modal"><@fmt.p total "student" /></a>
					<#if total==1>
						has feedback with a mark of zero and no grade. Zero marks are not populated with a default grade and it will not be uploaded.
					<#else>
						have feedback with marks of zero and no grades. Zero marks are not populated with a default grade and they will not be uploaded.
					</#if>
				</p>
			</#if>
			<#if gradeValidation.populated?has_content>
				<#local total = gradeValidation.populated?keys?size />
				<p>
					<a href="#grade-validation-populated-modal" data-toggle="modal"><@fmt.p total "student" /></a>
					<#if total==1>
						has feedback with a grade that is empty. It will be populated with a default grade.
					<#else>
						have feedback with grades that are empty. They will be populated with a default grade.
					</#if>
				</p>
			</#if>
		</div>
		<div id="grade-validation-invalid-modal" class="modal fade">
			<@modal.header>
				<h3 class="modal-title">Students with invalid grades</h3>
			</@modal.header>
			<@modal.body>
				<table class="table table-condensed table-bordered table-striped table-hover">
					<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th><th>Valid grades</th></tr></thead>
					<tbody>
						<#list gradeValidation.invalid?keys as feedback>
						<tr>
							<td>${feedback.studentIdentifier}</td>
							<td>${(feedback.latestMark)!}</td>
							<td>${(feedback.latestGrade)!}</td>
							<td>${mapGet(gradeValidation.invalid, feedback)}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</@modal.body>
		</div>
		<div id="grade-validation-zero-modal" class="modal fade">
			<@modal.header>
				<h3 class="modal-title">Students with zero marks and empty grades</h3>
			</@modal.header>
			<@modal.body>
				<table class="table table-condensed table-bordered table-striped table-hover">
					<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th></tr></thead>
					<tbody>
						<#list gradeValidation.zero?keys as feedback>
						<tr>
							<td>${feedback.studentIdentifier}</td>
							<td>${(feedback.latestMark)!}</td>
							<td>${(feedback.latestGrade)!}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</@modal.body>
		</div>
		<div id="grade-validation-populated-modal" class="modal fade">
			<@modal.header>
				<h3 class="modal-title">Students with empty grades</h3>
			</@modal.header>
			<@modal.body>
				<table class="table table-condensed table-bordered table-striped table-hover">
					<thead><tr><th>University ID</th><th>Mark</th><th>Populated grade</th></tr></thead>
					<tbody>
						<#list gradeValidation.populated?keys as feedback>
						<tr>
							<td>${feedback.studentIdentifier}</td>
							<td>${(feedback.latestMark)!}</td>
							<td>${mapGet(gradeValidation.populated, feedback)}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</@modal.body>
		</div>
		<#else>
		<div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
			<#local total = gradeValidation.populated?keys?size + gradeValidation.invalid?keys?size />
			<a href="#grade-validation-modal" data-toggle="modal"><@fmt.p total "student" /></a>
			<#if total==1>
				has feedback with a grade that is empty or invalid. It will not be uploaded.
			<#else>
				have feedback with grades that are empty or invalid. They will not be uploaded.
			</#if>
		</div>
		<div id="grade-validation-modal" class="modal fade">
			<@modal.header>
				<h3 class="modal-title">Students with empty or invalid grades</h3>
			</@modal.header>
			<@modal.body>
				<table class="table table-condensed table-bordered table-striped table-hover">
					<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th><th>Valid grades</th></tr></thead>
					<tbody>
						<#list gradeValidation.populated?keys as feedback>
						<tr>
							<td>${feedback.studentIdentifier}</td>
							<td>${(feedback.latestMark)!}</td>
							<td></td>
							<td></td>
						</tr>
						</#list>
						<#list gradeValidation.invalid?keys as feedback>
						<tr>
							<td>${feedback.studentIdentifier}</td>
							<td>${(feedback.latestMark)!}</td>
							<td>${(feedback.latestGrade)!}</td>
							<td>${mapGet(gradeValidation.invalid, feedback)}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</@modal.body>
		</div>
		</#if>
	</#if>
<script>
	jQuery(function($){
		$('#sendToSits').on('change', function(){
			var $validationDiv = $('.grade-validation');
			if ($(this).is(':checked') && ($validationDiv.hasClass('alert-info') || $validationDiv.hasClass('alert-error'))) {
				$validationDiv.show();
			} else {
				$validationDiv.hide();
			}
		});
	});
</script>
</#macro>

<#macro uploadToSits withValidation assignment verb isGradeValidation=false gradeValidation="">
<div class="alert alert-info">
	<label class="checkbox">
		<@f.checkbox path="sendToSits" id="sendToSits" />
		Queue these marks for upload to SITS
	</label>
	<#if assignment.module.adminDepartment.canUploadMarksToSitsForYear(assignment.academicYear, assignment.module)>
		<div>
			<p>${verb} this feedback will cause marks to be queued for upload to SITS.</p>
			<p>Marks and grades will automatically be uploaded and displayed in the SITS SAT screen as actual marks and grades.</p>
		</div>
	<#else>
		<div class="alert alert-warning">
			<p>${verb} this feedback will cause marks to be queued for upload to SITS.</p>
			<p>
				However mark upload is closed for ${assignment.module.adminDepartment.name} <#if assignment.module.degreeType??> (${assignment.module.degreeType.toString})</#if>
				for the academic year ${assignment.academicYear.toString}.
			</p>
			<p>
				If you still have marks to upload, please contact the Exams Office <a id="email-support-link" href="mailto:aoexams@warwick.ac.uk">aoexams@warwick.ac.uk</a>.
			</p>
			<p>
				As soon as mark upload is re-opened for this department,
				the marks and grades will automatically be uploaded and displayed in the SITS SAT screen as actual marks and grades
			</p>
		</div>
	</#if>
</div>

	<#if withValidation>
		<@feedbackGradeValidation isGradeValidation gradeValidation />
	</#if>
</#macro>