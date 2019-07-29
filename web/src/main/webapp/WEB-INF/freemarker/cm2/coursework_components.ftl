<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#import "*/modal_macros.ftlh" as modal />

<#macro student_assignment_list id title assignments empty_message expand_by_default=true show_submission_progress=false hide_late_formative=false>
  <span id="${id}-container">
    <#local has_late_formative = false />
    <#if hide_late_formative><#list assignments as info><#if info.lateFormative><#local has_late_formative = true /><#break></#if></#list></#if>
    <#local has_assignments = (assignments!?size gt 0) />
    <div id="${id}" class="striped-section student-assignment-list collapsible<#if expand_by_default> expanded</#if>" data-name="${id}">
      <div class="clearfix">
        <h4 class="section-title"><a class="collapse-trigger icon-container" href="#">${title}</a></h4>
        <#if hide_late_formative && has_late_formative>
          <div class="checkbox">
            <label>
              <input type="checkbox" name="lateFormative" /> Show late formative assignments
              <@fmt.help_popover id="route" content="Formative assignments do not count towards the final module mark." />
            </label>
          </div>
          <script type="text/javascript" nonce="${nonce()}">
            jQuery(function ($) {
              $('input[name=lateFormative]:checkbox').on('change', function () {
                $('span.late_formative').toggleClass('hidden', !this.checked);
              });
            });
          </script>
        </#if>

        <div class="striped-section-contents">
          <#if has_assignments || has_late_formative>
            <div class="row">
              <div class="col-md-3">Details</div>
              <div class="col-md-4 col-lg-5">Progress</div>
              <div class="col-md-5 col-lg-4">Actions</div>
            </div>
          </#if>
          <#if !has_assignments>
            <div class="item-info empty-message">
              ${empty_message}
            </div>
          </#if>
          <#list assignments as info>
            <span id="assignment-container-${info.assignment.id}" class="<#if hide_late_formative && info.lateFormative>late_formative hidden</#if>">
              <@student_assignment_info info show_submission_progress />
            </span>
          </#list>
        </div>
      </div>
    </div>
  </span>

  <#if !expand_by_default>
  <#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
    <script type="text/javascript" nonce="${nonce()}">
      GlobalScripts.initCollapsible(jQuery('#${id}').filter(':not(.empty)'));
    </script>
  </#if>
</#macro>

<#macro progress_bar tooltip percentage class="default">
  <div class="progress" data-title="${fmt.strip_html(tooltip)}">
    <div class="progress-bar progress-bar-${class}" role="progressbar" aria-valuenow="${percentage?c}" aria-valuemin="0" aria-valuemax="100"
         style="width: ${percentage?c}%;">
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
    <#if info.hasActiveExtension && extension.expiryDate??>
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
  <#elseif !assignment.collectSubmissions>
    <#local extension = info.extension! />

    <#if info.studentDeadline??>
      <#local time_remaining = durationFormatter(info.studentDeadline) />
      <#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
    </#if>

    <#if info.hasActiveExtension>
      <#local extension_time_remaining = durationFormatter(extension.expiryDate) />
    </#if>

    <#if info.extended>
      <#local state = "info" />
      <#local tooltip>
        ${extension_time_remaining} until extended deadline (<@fmt.date date=extension.expiryDate />)
      </#local>
    <#elseif assignment.closed>
      <#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
      <#local state = "info" />
      <#local tooltip>
        Assignment close ${durationFormatter(info.studentDeadline)} (<@fmt.date date=info.studentDeadline />)
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
      <span tabindex="0" class="fa-stack tabula-tooltip" data-title="Assignment ready">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x fa-check-circle-o text-success"></i>
			</span>
    <#else>
      <span tabindex="0" class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(tooltip)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x fa-circle-o text-default"></i>
			</span>
    </#if>

    <div tabindex="0" class="bar tabula-tooltip" data-title="${fmt.strip_html(tooltip)}">
      <div class="progress-bar progress-bar-${state}" role="progressbar" aria-valuenow="${percentage?c}" aria-valuemin="0" aria-valuemax="100"
           style="width: ${percentage?c}%;">
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

    <span tabindex="0" class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(tooltip)}">
			<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
			<i class="fa fa-stack-1x ${icon} text-${state}"></i>
		</span>
  </div>
</#macro>

<#macro student_assignment_info info show_submission_progress=false>
  <#local assignment = info.assignment />
  <div class="item-info row assignment-${assignment.id}">
    <div class="col-md-3">
      <div class="module-title">
        <#if can.do_as_real_user('Submission.Read', assignment.module)>
          <#local module_url><@routes.cm2.depthome assignment.module /></#local>
          <@fmt.module_name_with_link assignment.module module_url />
        <#else>
          <@fmt.module_name assignment.module />
        </#if>
      </div>
      <h4 class="name">
        <#if can.do_as_real_user('Submission.Read', assignment.module)>
          <a href="<@routes.cm2.assignmentSubmissionSummary assignment />">
            <span class="ass-name">${assignment.name}</span>
          </a>
        <#else>
          <span class="ass-name">${assignment.name}</span>
        </#if>
      </h4>
    </div>
    <div class="col-md-6">
      <#local submissionStatus = "" />

      <#if info.submission??>
        <#local submission = info.submission />
        <#local submissionStatus>
          <strong>Submitted:</strong> <@fmt.date date=submission.submittedDate />
        </#local>
      <#elseif assignment.collectSubmissions && !assignment.opened>
        <#local submissionStatus>
          <strong>Assignment open:</strong>
          <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />"
                data-html="true"><@fmt.date date=assignment.openDate /> - ${durationFormatter(assignment.openDate)}</span>
        </#local>
      <#elseif assignment.collectSubmissions && assignment.openEnded>
        <#local submissionStatus>
          <strong>Not submitted</strong>
        </#local>
      <#elseif info.submittable || !assignment.collectSubmissions>
        <#local extension = info.extension! />

        <#if info.studentDeadline??>
          <#local time_remaining = durationFormatter(info.studentDeadline) />
          <#local percentage = durationPercentage(assignment.openDate, info.studentDeadline) />
        </#if>

        <#if info.hasActiveExtension && extension.expiryDate??>
          <#local extension_time_remaining = durationFormatter(extension.expiryDate) />
        </#if>

        <#if assignment.collectSubmissions>
          <#local submissionStatus>
            <strong>Not submitted</strong>
          </#local>
        </#if>

        <#if info.extended>
          <#local submissionStatus>
            <strong>Extension granted</strong>
          </#local>
        <#elseif assignment.collectSubmissions && assignment.closed>
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
        <#if info.feedback.releasedDate??>
          <#local feedbackStatus>
            <strong>Feedback received:</strong> <@fmt.date date=info.feedback.releasedDate />
          </#local>
        <#else>
          <#local feedbackStatus>
            <strong>Feedback received</strong>
          </#local>
        </#if>
      <#else>
        <#if (info.submission?? || !assignment.collectSubmissions) && info.feedbackDeadline??>
          <#local feedbackStatus>
            <strong>Feedback <#if info.feedbackLate>over</#if>due:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek info.feedbackDeadline />"
                                                                                  data-html="true"><@fmt.date date=info.feedbackDeadline includeTime=false /></span>
            <#if info.feedbackLate>
              <br />
              Please contact your Departmental Administrator with any queries
            </#if>
          </#local>
        <#elseif info.submission?? && !info.feedbackDeadline?? && info.submission.late>
          <#local feedbackStatus>
            There is no specific feedback due date as the submission was late.
          </#local>
        <#elseif info.submission?? && !assignment.publishFeedback>
          <#local feedbackStatus>
            Feedback for this assignment is provided outside Tabula
          </#local>
        <#elseif !(info.submission??) && info.studentDeadline??>
          <#local feedbackStatus>
            <strong>Assignment due:</strong>
            <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek info.studentDeadline />"
                  data-html="true"><@fmt.date date=info.studentDeadline /> - ${durationFormatter(info.studentDeadline)}</span>
          </#local>
        </#if>
      </#if>

      <div class="submission-status">${submissionStatus}</div>
      <div class="feedback-status">${feedbackStatus}</div>
    </div>
    <div class="col-md-3">
      <#if info.feedback??>
      <#-- View feedback -->
        <a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
          View feedback
        </a>
      <#elseif info.submission?? && info.resubmittable>
      <#-- Resubmission allowed -->
        <a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
          View receipt
        </a>

        <a class="btn btn-block btn-default" href="<@routes.cm2.assignment assignment />#submittop">
          Resubmit assignment
        </a>
      <#elseif info.submission??>
      <#-- View receipt -->
        <a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
          View receipt
        </a>
      <#elseif info.submittable || (!assignment.collectSubmissions && assignment.extensionsPossible)>
        <p>
          <#if info.submittable>
          <#-- First submission allowed -->
            <a class="btn btn-block btn-primary" href="<@routes.cm2.assignment assignment />">
              Submit assignment
            </a>
          <#else>
            <a class="btn btn-block btn-default" href="<@routes.cm2.assignment assignment />">
              View details
            </a>
          </#if>
        </p>

        <#if assignment.extensionsPossible>
          <#if info.extensionRequested>
            <p>
              <a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.home />" class="btn btn-block btn-default">
                Review extension request
              </a>
            </p>
          <#elseif !info.extended && assignment.newExtensionsCanBeRequested>
            <p>
              <a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.home />" class="btn btn-block btn-default">
                Request extension
              </a>
            </p>
          </#if>
        </#if>
      <#else>
      <#-- Assume formative, so just show info -->
        <a class="btn btn-block btn-default" href="<@routes.cm2.assignment assignment />">
          View details
        </a>
      </#if>
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
  <#elseif assignment.newExtensionsCanBeRequested>
    <@extension_button_contents "Request an extension" assignment />
  </#if>
</#macro>

<#macro lateness assignment user submission="">
  <#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
    <#if submission.late>
      <@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(submission.deadline, submission.submittedDate, true)} after deadline
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
        <div class="time-remaining">${extension_time_remaining} <span tabindex="0" class="label label-warning use-tooltip" title="${latenesstooltip}" data-container="body">Late</span>
        </div>
        Extension deadline was <@fmt.date date=extension.expiryDate />
      <#else>
        <#local latenesstooltip><@lateness assignment user info.submission /></#local>
        <div class="time-remaining">${time_remaining} <span tabindex="0" class="label label-warning use-tooltip" title="${latenesstooltip}" data-container="body">Late</span>
        </div>
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

<#macro marker_assignment_list id title assignments verb="Mark" expand_by_default=true marker=user.apparentUser show_actions=true>
  <span id="${id}-container">
		<#local has_assignments = (assignments!?size gt 0) />
		<div id="${id}" class="striped-section marker-assignment-list<#if has_assignments> collapsible<#if expand_by_default> expanded</#if><#else> empty</#if>"
         data-name="${id}">
			<div class="clearfix">
				<h4 class="section-title"><a class="collapse-trigger icon-container" href="#">${title}</a></h4>

				<#if has_assignments>
          <div class="striped-section-contents">
						<div class="row">
							<div class="col-md-3">Details</div>
							<div class="col-md-3">Status</div>
							<div class="col-md-4">Progress</div>
							<#if (show_actions?is_hash && show_actions?size != 0) || (show_actions?is_boolean && show_actions)>
                <div class="col-md-2">Actions</div>
              </#if>
						</div>

						<#list assignments as info>
              <span id="marker-assignment-container-${info.assignment.id}">
								<#if show_actions?is_hash>
                  <#assign assignment_show_actions=show_actions[info.assignment.id] />
                <#else>
                  <#assign assignment_show_actions=show_actions />
                </#if>
                <@marker_assignment_info info verb marker assignment_show_actions />
							</span>
            </#list>
					</div>
        </#if>
			</div>
		</div>
	</span>

  <#if !expand_by_default>
  <#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
    <script type="text/javascript" nonce="${nonce()}">
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
            <@workflowMessage progress.progress.messageCode /> (${progress.count})<#if progress_has_next>, </#if>
          </#list>
        </#compress></#local>
      </#if>

      <#if stageInfo_index gt 0>
        <div tabindex="0" class="bar bar-${state} tabula-tooltip" data-title="${fmt.strip_html(title)}"></div>
      </#if>
      <span tabindex="0" class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(title)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state}"></i>
			</span>
    </#list>
  </div>
</#macro>

<#-- Progress bar for a single student in a marking workflow  -->
<#macro individual_stage_progress_bar markerStages assignment student>
  <div class="stage-progress-bar">
    <#list markerStages as progress>
      <#local stage = progress.stage />

      <#local state = 'default' />
      <#local icon = 'fa-circle-o' />
      <#if progress.completed>
        <#local state = 'success' />
        <#local icon = 'fa-check-circle-o' />
      <#elseif progress.skipped>
        <#local state = 'primary' />
        <#local icon = 'fa-arrow-circle-o-right' />
      <#elseif progress.started>
        <#local state = 'warning' />
        <#local icon = 'fa-dot-circle-o' />
      </#if>

      <#local title><@workflowMessage progress.stage.actionCode assignment student /></#local>
      <#if progress_index gt 0>
        <div tabindex="0" class="bar bar-${state} tabula-tooltip" data-title="${fmt.strip_html(title)}"></div>
      </#if>
      <#local title><@workflowMessage progress.messageCode assignment student /></#local>
      <span tabindex="0" class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(title)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state}"></i>
			</span>
    </#list>
  </div>
</#macro>

<#macro marker_assignment_info info verb="Mark" marker=user.apparentUser show_actions=true>
  <#local assignment = info.assignment />
  <div class="item-info row marker-assignment-${assignment.id}">
    <div class="col-md-3">
      <div class="module-title">
        <#if can.do_as_real_user('Submission.Read', assignment.module)>
          <#local module_url><@routes.cm2.depthome assignment.module /></#local>
          <@fmt.module_name_with_link assignment.module module_url />
        <#else>
          <@fmt.module_name assignment.module />
        </#if>
      </div>
      <h4 class="name">
        <#-- If the user can administer the assignment, link them to the admin page here -->
        <#if can.do("Module.ManageAssignments", assignment.module)>
          <a href="<@routes.cm2.assignmentSubmissionSummary assignment />">
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
              <li><@workflowMessage progress.progress.messageCode /> (${progress.count})</li>
            </#list>
          </#if>
        </#list>
      </ul>
    </div>
    <div class="col-md-${show_actions?string('4', '6')}">
      <@stage_progress_bar info.stages />

      <#if !assignment.opened>
        <div>
          <strong>Submissions open:</strong>
          <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />" data-html="true"><@fmt.date date=assignment.openDate /></span>
        </div>
      </#if>

      <#if !assignment.openEnded && !assignment.closed>
        <div>
          <strong>Submissions due:</strong>
          <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.closeDate />" data-html="true"><@fmt.date date=assignment.closeDate /></span>
        </div>
      </#if>

      <#if info.feedbackDeadline??>
        <div>
          <strong>Student feedback due:</strong>
          <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek info.feedbackDeadline />"
                data-html="true"><@fmt.date date=info.feedbackDeadline includeTime=false /></span>
        </div>
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
    <#if show_actions>
      <div class="col-md-2">
        <#if verb?has_content>
          <#if assignment.cm2Assignment>
            <a class="btn btn-block btn-primary" href="<@routes.cm2.listmarkersubmissions assignment marker />">
              ${verb}
            </a>
          <#else>
            <a class="btn btn-block btn-primary" href="<@routes.coursework.listmarkersubmissions assignment marker />">
              ${verb}
            </a>
          </#if>
        </#if>
      </div>
    </#if>
  </div>
</#macro>

<#macro admin_assignment_list module assignments academicYear expand_by_default=true>
  <#local id>module-${module.id}</#local>
  <#local title><@fmt.module_name module /></#local>

  <span id="${id}-container">
		<#local has_assignments = (assignments!?size gt 0) />
		<div id="${id}" class="striped-section admin-assignment-list<#if has_assignments> collapsible<#if expand_by_default> expanded</#if><#else> empty</#if>"
         data-name="${id}"
			<#if has_assignments && !expand_by_default>
        data-populate=".striped-section-contents"
        data-href="<@routes.cm2.modulehome module academicYear />?${info.requestedUri.query!}&skeleton"
        data-name="${id}"
      </#if>
		>
			<div class="clearfix">

				<h4 class="section-title with-button"><#if has_assignments><a class="collapse-trigger icon-container" href="#"></#if>${title}<#if has_assignments></a></#if>
        				<div class="btn-group section-manage-button">
					<a class="btn btn-primary btn-sm dropdown-toggle" data-toggle="dropdown" href="#">Manage this module <span class="caret"></span></a>
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

        </h4>

				<#if has_assignments>
          <div class="striped-section-contents">
						<#if expand_by_default>
              <#list assignments as info>
                <span id="admin-assignment-container-${info.assignment.id}">
									<@admin_assignment_info info skeleton />
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
    <script type="text/javascript" nonce="${nonce()}">
      GlobalScripts.initCollapsible(jQuery('#${id}').filter(':not(.empty)'));
    </script>
  </#if>
</#macro>

<#macro admin_assignment_info info skeleton=false>
  <#local assignment = info.assignment />
  <#if !skeleton>
    <#local studentCount = (assignment.membershipInfo.totalCount + assignment.submissionsFromUnenrolledStudents?size) />
  </#if>
  <div class="item-info admin-assignment-${assignment.id}" data-detailurl="<@routes.cm2.enhancedAssignmentDetails info.assignment />">

    <div class="clearfix">
      <div class="pull-right">
        <#if assignment.cm2Assignment>
          <#local edit_url><@routes.cm2.editassignmentdetails assignment /></#local>
        <#else>
          <#local edit_url><@routes.coursework.assignmentedit assignment /></#local>
        </#if>
        <@fmt.permission_button
        classes='btn btn-primary btn-xs'
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

    <#if skeleton><p class="hint"><i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em></p>
    <#else>
      <div class="row">
        <div class="col-md-4">
          <h6 class="sr-only">Assignment information</h6>

          <ul class="list-unstyled">
            <#if assignment.archived>
              <li><strong>Archived</strong></li></#if>
            <#if !assignment.opened>
              <li><strong>Assignment opens:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />"
                                                           data-html="true"><@fmt.date date=assignment.openDate /></span></li>
            </#if>

            <#if assignment.openEnded>
              <li><strong>Open-ended</strong></li>
            <#else>
              <li><strong>Assignment <#if assignment.closed>closed<#else>due</#if>:</strong> <span tabindex="0" class="use-tooltip"
                                                                                                   title="<@fmt.dateToWeek assignment.closeDate />"
                                                                                                   data-html="true"><@fmt.date date=assignment.closeDate /></span>
              </li>
            </#if>

            <li>
              <strong>Enrolled students:</strong>
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
              <li><strong>Feedback due:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.feedbackDeadline />"
                                                       data-html="true"><@fmt.date date=assignment.feedbackDeadline includeTime=false /></span></li>
            </#if>
          </ul>

          <#if assignment.collectSubmissions || assignment.extensionsPossible>
            <ul class="list-unstyled">
              <#if assignment.collectSubmissions>
                <#if assignment.submissionsFromUnenrolledStudents?size == 0>
                  <li><strong>Submissions received:</strong> ${assignment.submissions?size}</li>
                <#else>
                  <li><strong>Submissions received:</strong> ${assignment.submissions?size} (${assignment.submissionsFromUnenrolledStudents?size} from students
                    not enrolled on the assignment)
                  </li>
                </#if>
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
                    <strong>Extensions:</strong> ${assignment.countExtensions}
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
              <li><strong>Created:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.createdDate />"
                                                  data-html="true"><@fmt.date date=assignment.createdDate /></span></li>

              <#if assignment.opened>
                <li><strong>Opened:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.openDate />"
                                                   data-html="true"><@fmt.date date=assignment.openDate /></span></li>
              </#if>

              <#if !assignment.openEnded && assignment.closed>
                <li><strong>Closed:</strong> <span tabindex="0" class="use-tooltip" title="<@fmt.dateToWeek assignment.closeDate />"
                                                   data-html="true"><@fmt.date date=assignment.closeDate /></span></li>
              </#if>

              <#list info.stages as category>
                <li class="category">
                  <strong><@workflowMessage category.category.code /></strong>:
                  <ul>
                    <#list category.stages as stage>
                      <#list stage.progress as progress>
                        <li><@workflowMessage progress.progress.messageCode /> (${progress.count} of ${studentCount})</li>
                      </#list>
                    </#list>
                  </ul>
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
                  <#-- As of TAB-5388 show nothing -->
                  <#elseif assignment.openEnded && assignment.collectSubmissions && studentCount gt 0>
                    <li>Assignment needs submitting (${studentCount} of ${studentCount})</li>
                  <#else>
                    <li>Awaiting feedback</li>
                  </#if>
                </#if>
              <#else>
                <#list info.nextStages as nextStage>
                  <li>
                    <#local nextStageDescription><@workflowMessage nextStage.stage.actionCode /> (${nextStage.count} of ${studentCount})</#local>
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
    </#if>
  </div>
</#macro>

<#macro workflowMessage code assignment="" student=""><#compress>
  <#local studentName = "student" />
  <#local firstMarkerName = "first marker" />
  <#local secondMarkerName = "second marker" />
  <#if assignment?has_content && student?has_content>
    <#local studentName><span
      data-profile="${student.warwickId!(student.userId)}"><#if assignment.module.adminDepartment.showStudentName>${student.fullName}<#else>${student.warwickId!(student.userId)}</#if></span></#local>

    <#local firstMarker = assignment.getStudentsFirstMarker(student.userId)!"" />
    <#if firstMarker?has_content>
      <#local firstMarkerName><span data-profile="${firstMarker.warwickId!(firstMarker.userId)}">${firstMarker.fullName}</span></#local>
    </#if>

    <#local secondMarker = assignment.getStudentsSecondMarker(student.userId)!"" />
    <#if secondMarker?has_content>
      <#local secondMarkerName><span data-profile="${secondMarker.warwickId!(secondMarker.userId)}">${secondMarker.fullName}</span></#local>
    </#if>
  </#if>

  <#local text><@spring.message code=code /></#local>
  ${(text!"")?replace("[STUDENT]", studentName)?replace("[FIRST_MARKER]", firstMarkerName)?replace("[SECOND_MARKER]", secondMarkerName)}
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
        <a target="turnitin-viewer" href="<@routes.cm2.turnitinReport assignment attachment />">View full report - available via Tabula until end of August
          2016</a>
      </#if>
    </p>
  </div>
  <script type="text/javascript" nonce="${nonce()}">
    jQuery(function ($) {
      $("#tool-tip-${attachment.id}").popover({
        placement: 'right',
        html: true,
        content: function () {
          return $('#tip-content-${attachment.id}').html();
        },
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
      <#elseif stage.skipped>
        <#local state = 'primary' />
        <#local icon = 'fa-arrow-circle-o-down' />
        <#local title = 'Skipped' />
      <#elseif stage.started>
        <#local state = 'warning' />
        <#local icon = 'fa-dot-circle-o' />
        <#local title = 'Current stage' />
      <#elseif !stage.preconditionsMet>
        <#local title = 'Preconditions not met' />
      </#if>

      <span tabindex="0" class="fa-stack tabula-tooltip" data-title="${fmt.strip_html(title)}">
				<i class="fa fa-stack-1x fa-circle fa-inverse"></i>
				<i class="fa fa-stack-1x ${icon} text-${state}"></i>
			</span>

      <#local content><#nested/></#local>

      <strong><@spring.message code=stage.messageCode /></strong><#if content?has_content>: ${content}</#if>
    </li>
  </#if>
</#macro>

<#macro uniIdSafeMarkerLink marker role>
  - <a href="<@routes.coursework.listmarkersubmissions assignment marker />">Proxy as this ${role}</a>
</#macro>

<#macro uniIdSafeCM2MarkerLink stage marker student>
  - <a href="<@routes.cm2.listmarkersubmissions assignment marker />#${stage.name}-${student.userId}">Proxy</a>
</#macro>

<#macro student_workflow_details assignment student canProxy>
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
          <@submission_details submission true student />
        <#elseif stage_name == 'CheckForPlagiarism'>
          <#if submission??>
            <@fmt.p submission.allAttachments?size "file" />
            <#list submission.allAttachments as attachment>
              <#if attachment.originalityReportReceived>
                <@components.originalityReport attachment />
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
            <#if canProxy>
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
            <#if canProxy>
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
            <#if canProxy>
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
            <#if canProxy>
              <@uniIdSafeMarkerLink fm "marker" />
            </#if>
          </#if>
        <#elseif stage_name == 'CM2ReleaseForMarking'>

        <#elseif assignment.cm2Assignment && student.stages[stage_name].stage.markingRelated>
          <#if feedback??>
            <#local markingStage = student.stages[stage_name].stage.markingStage />
            <#local marker = mapGet(feedback.feedbackMarkers, markingStage)! />

            <#if (marker.userId)??>
              ${marker.fullName}
              <#if canProxy>
                <@uniIdSafeCM2MarkerLink markingStage marker student.user />
              </#if>
            <#elseif stage_name != "CM2MarkingWorkflowStage(admin-moderation-admin)">
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
          </#if>
        <#elseif stage_name == 'AddFeedback'>
          <#if feedback?? && (feedback.hasAttachments || feedback.hasOnlineFeedback)>
            <#local attachments=feedback.attachments />
            <#if attachments?size gt 0>
              <@studentFeedbackDownload feedback />
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
              <@studentFeedbackDownload feedback />
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
              <#if feedback.hasPrivateOrNonPrivateAdjustments>
                <li>
                  <span class="fa-stack"></span>
                  <@studentFeedbackAdjustment feedback />
                </li>
              </#if>
            </ul>
          </#if>
        <#elseif stage_name == 'UploadMarksToSits'>
          <#if feedback?? && feedback.hasContent>
            <#if enhancedFeedback.feedbackForSits??>
              <#local feedbackForSits = enhancedFeedback.feedbackForSits />
              <#local feedbackSitsStatus = feedbackForSits.status />
              <#local sitsWarning = feedbackForSits.dateOfUpload?? && feedbackSitsStatus.code != "uploadNotAttempted" && (
              (feedbackForSits.actualMarkLastUploaded!0) != (feedback.latestMark!0) || (feedbackForSits.actualGradeLastUploaded!"") != (feedback.latestGrade!"")
              ) />

              <#if feedbackSitsStatus.code == "failed">
                <a href="<@routes.cm2.checkSitsUpload feedback />" target="_blank">
                  Diagnose problems
                </a>
              <#elseif sitsWarning>
                The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.
              </#if>
            <#else>
              <a href="<@routes.cm2.checkSitsUpload feedback />" target="_blank">
                Check settings
              </a>
            </#if>
          </#if>
        </#if>
      </#compress></@workflow_stage>
    </#list>
  </ul>
</#macro>

<#macro marker_feedback_summary feedback stage currentStage=[] currentFeedback=[] currentStageIncomplete=false>
  <h4>${stage.description} <#if feedback.marker??>- ${feedback.marker.fullName}</#if></h4>
  <#if feedback.hasContent && !feedback.hasBeenModified && !currentStageIncomplete && feedback.wasModerated!false>
    Approved by the moderator
  <#elseif !feedback.hasContent>
    Not moderated
  <#else>
    <#if feedback.customFormValues?has_content>
      <#list assignment.feedbackFields as field>
        <#list feedback.customFormValues as fv>
          <#if fv.name == field.name>
            <#assign formValue = fv>
            <#break>
          </#if>
        </#list>

        <@bs3form.form_group>
          <label for="form-value-${formValue.id}">${field.label}</label>
          <#if formValue?? && formValue.value?has_content>
            <textarea id="form-value-${formValue.id}" class="form-control feedback-comments" readonly="readonly"
                      data-form-value-name="${formValue.name}">${formValue.value!""}</textarea>
          <#else>
            <p>No comments added.</p>
          </#if>
        </@bs3form.form_group>
      </#list>
    <#else>
      <p>No feedback comments added.</p>
    </#if>

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
          <#if feedback.marker.userId?has_content >
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
            <a class="btn btn-default long-running" href="${downloadUrl}">Download feedback</a>
            <ul class="feedback-attachments hide">
              <#list feedback.attachments as attachment>
                <li id="attachment-${attachment.id}" class="attachment">
                  <span>${attachment.name}</span>&nbsp;<a href="#" class="remove-attachment">Remove</a>
                  <input type="hidden" name="attachedFiles" value="${attachment.id}" />
                </li>
              </#list>
            </ul>
          <#else>
            The <@fmt.p number=feedback.attachments?size singular="feedback attachment" shownumber=false/> cannot be downloaded as the marker is unknown.
          </#if>
        </#if>
      </div>
      <div class="col-xs-3">
        <#if currentFeedback?? && currentFeedback?has_content>
          <#if currentStage?? && currentStage.populateWithPreviousFeedback>
            <div class="form-group">
              <form>
                <label class="radio-inline"><input type="radio" name="changesState" <#if !currentFeedback.hasBeenModified>checked</#if> value="approve" />Approve</label>
                <label class="radio-inline"><input type="radio" name="changesState" <#if currentFeedback.hasBeenModified>checked</#if> value="make-changes">Make
                  changes</label>
              </form>
            </div>
          <#else>
            <a class="copy-feedback btn btn-default long-running" href="#">Copy comments and files</a>
          </#if>
        </#if>
      </div>
    </div>
  </#if>



</#macro>

<#macro lateness submission="" assignment="" user=""><#compress>
  <#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
    <#if submission.late>
      <@fmt.p submission.workingDaysLate "working day" /> late: ${durationFormatter(submission.deadline, submission.submittedDate, true)} after the deadline
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
    <@fmt.p submission.workingDaysLate "working day" /> late: ${durationFormatter(extension.expiryDate, submission.submittedDate, true)} after the extended deadline (<@fmt.date date=extension.expiryDate capitalise=false shortMonth=true stripHtml=true />)
  </#if>
</#compress></#macro>

<#macro submission_attachments_link submission link_single="Download submission" link_multiple="Download submission">
  <#local attachments = submission.allAttachments />

  <#if attachments?size gt 0>
    <#if attachments?size == 1>
      <#local filename = "${attachments[0].name}">
      <#local downloadUrl><@routes.cm2.downloadSubmission submission filename/>?single=true</#local>
      <a class="long-running" href="${downloadUrl}">${link_single}</a>
    <#else>
      <#local filename = "submission-${submission.studentIdentifier}.zip">
      <#local downloadUrl><@routes.cm2.downloadSubmission submission filename/></#local>

      <#local popoverContent>
        <ul class="list-unstyled">
          <#list attachments as attachment>
            <li><a href="<@routes.cm2.downloadSubmission submission attachment.name />">${attachment.name}</a></li>
          </#list>
        </ul>

        <p style="margin-top: 1.5rem; margin-bottom: 0; font-weight: bold;">
          <a class="long-running" href="${downloadUrl}"><i class="fa fa-arrow-circle-o-down fa-fw"></i>Download all as zip</a>
        </p>
      </#local>

      <a href="#" class="use-popover" data-html="true" data-content="${popoverContent?html}">${link_multiple}</a>
    </#if>
  </#if>
</#macro>

<#macro submission_details submission=[] showDisabilityDisclosure=false student=""><@compress single_line=true>
  <#if submission?has_content>
    <#if submission.submittedDate??>
      <span tabindex="0" class="date tabula-tooltip" data-title="<@lateness submission />">
				<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true />
			</span>
    </#if>

    <#if features.disabilityOnSubmission && showDisabilityDisclosure && student.disability??>
      <@disability_disclosure student_disability=student.disability/>
    </#if>

    <#if submission.allAttachments?size gt 0>
      &emsp;<@submission_attachments_link submission />
    </#if>
  </#if>
</@compress></#macro>

<#macro submission_status submission="" enhancedExtension="" enhancedFeedback="" student="">
  <#if submission?has_content>
    <#if submission.late>
      <#if enhancedExtension?has_content && enhancedExtension.extension.approved>
        <span tabindex="0" class="label label-danger use-tooltip" title="<@extensionLateness enhancedExtension.extension submission/>" data-container="body">Late</span>
      <#else>
        <span tabindex="0" class="label label-danger use-tooltip" title="<@lateness submission />" data-container="body">Late</span>
      </#if>
    <#elseif submission.authorisedLate>
      <span tabindex="0" class="label label-info use-tooltip" data-html="true"
            title="Extended until <@fmt.date date=enhancedExtension.extension.expiryDate capitalise=false shortMonth=true />" data-container="body">Within Extension</span>
    </#if>
    <#if features.disabilityOnSubmission && student.disability??>
      <@disability_disclosure student_disability=student.disability/>
    </#if>
  <#else>
    <span class="label label-info">Unsubmitted</span>
    <#if enhancedExtension?has_content>
      <#local extension=enhancedExtension.extension>
      <#if extension.approved && !extension.rejected>
        <#local date>
          <#if extension.expiryDate??>
            <@fmt.date date=extension.expiryDate capitalise=true shortMonth=true stripHtml=true />
          </#if>
        </#local>
      </#if>
      <#if enhancedExtension.within>
        <span tabindex="0" class="label label-info use-tooltip" data-html="true" title="${date}" data-container="body">Within Extension</span>
      <#elseif extension.rejected>
        <span class="label label-info">Extension Rejected</span>
      <#elseif !extension.approved>
        <span class="label label-info">Extension Requested</span>
      <#else>
        <span tabindex="0" class="label label-info use-tooltip" title="${date}" data-container="body">Extension Expired</span>
      </#if>
    </#if>
  </#if>
</#macro>

<#macro disability_disclosure student_disability>
  <a href="#" class="use-popover cue-popover white-text-decoration" id="popover-disability" data-html="true"
     data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${student_disability.code}</h6><small>${(student_disability.sitsDefinition)!}</small></div>"
  >
    <span class="label label-info">Disability disclosed</span>
  </a>
</#macro>

<#macro originalityReport attachment>
  <#local r=attachment.originalityReport />
  <#local assignment=attachment.submissionValue.submission.assignment />

  <span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
  <div id="tip-content-${attachment.id}" class="hide">
    <p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
    <p class="similarity-subcategories-tooltip">
      Web: ${r.webOverlap!"-"}%<br>
      Student papers: ${r.studentOverlap!"-"}%<br>
      Publications: ${r.publicationOverlap!"-"}%
    </p>
    <p>
      <a target="turnitin-viewer" href="<@routes.cm2.turnitinLtiReport assignment attachment />">View full report</a>
    </p>
  </div>
  <script type="text/javascript" nonce="${nonce()}">
    jQuery(function ($) {
      $("#tool-tip-${attachment.id}").popover({
        placement: 'right',
        container: 'body',
        html: true,
        content: function () {
          return $('#tip-content-${attachment.id}').html();
        },
        title: 'Turnitin report summary'
      });
    });
  </script>
</#macro>

<#macro studentFeedbackDownload feedback>
  <#local attachments=feedback.attachments />
  <#if attachments?size == 1>
    <#local attachmentExtension = feedback.attachments[0].fileExt>
  <#else>
    <#local attachmentExtension = "zip">
  </#if>

  <a class="long-running" href="<@routes.cm2.feedbackDownload feedback attachmentExtension/>">
    <@fmt.p attachments?size "file" />
  </a>
</#macro>

<#macro studentFeedbackAdjustment feedback>
  <#local adjustmentReasonAdded=(feedback.latestPrivateOrNonPrivateAdjustment?? && feedback.latestPrivateOrNonPrivateAdjustment.reason??) />
  <span class="very-subtle">Marks adjusted:</span>
  <#if feedback.latestMark??>${feedback.latestMark}%</#if><#if feedback.latestGrade??>, </#if>
  <#if feedback.latestGrade??><span class="very-subtle">Grade:</span> ${feedback.latestGrade}</#if><#if adjustmentReasonAdded>, </#if>
  <#if adjustmentReasonAdded>
    <span class="very-subtle">Reason for adjustment:</span> ${feedback.latestPrivateOrNonPrivateAdjustment.reason!''}
  </#if>
</#macro>

<#macro downloadPdfModal url id="download-pdf-modal">
  <@modal.modal id="${id}">
    <@modal.wrapper>
      <@modal.header>
        <h3 class="modal-title">Download submissions as PDF</h3>
      </@modal.header>
      <@modal.body>
        <p>There are <span class="count"></span> submissions that have files that are not valid PDFs (shown below). The download will not include these files.
        </p>
        <p><a class="form-post btn btn-primary"
              data-href="${url}?download" href="">Download submissions as PDF</a>
        </p>
        <ul class="submissions"></ul>
      </@modal.body>
    </@modal.wrapper>
  </@modal.modal>
</#macro>
