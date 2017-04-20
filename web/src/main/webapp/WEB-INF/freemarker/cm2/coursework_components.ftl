<#ftl strip_text=true />

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
</#macro>

<#macro progress_bar tooltip percentage class="default">
	<div class="progress use-tooltip" title="${tooltip}" data-html="true">
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

	<@progress_bar tooltip percentage state />
</#macro>

<#macro feedback_progress info>

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
					<strong>Not open yet</strong>
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
					</#local>
				<#else>
					<#local feedbackStatus>
						<strong>Assignment due:</strong> <span class="use-tooltip" title="<@fmt.dateToWeek info.studentDeadline />" data-html="true"><@fmt.date date=info.studentDeadline /></span>
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