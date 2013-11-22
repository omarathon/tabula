<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#macro longDateRange start end>
	<#local openTZ><@warwick.formatDate value=start pattern="z" /></#local>
	<#local closeTZ><@warwick.formatDate value=end pattern="z" /></#local>
	<@fmt.date start />
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#if department??>


<#assign can_manage_dept=can.do("Department.ManageExtensionSettings", department) />
<#if (features.extensions || features.feedbackTemplates)>
	<h1 class="with-settings">
		${department.name}
	</h1>

	<div class="btn-toolbar dept-toolbar">

		<#if department.parent??>
			<a class="btn btn-medium use-tooltip" href="<@routes.departmenthome department.parent />" data-container="body" title="${department.parent.name}">
				Parent department
			</a>
		</#if>

		<#if department.children?has_content>
		<div class="btn-group">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				Subdepartments
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list department.children as child>
					<li><a href="<@routes.departmenthome child />">${child.name}</a></li>
				</#list>
			</ul>
		</div>
		</#if>

		<#if !modules?has_content && department.children?has_content>
			<a class="btn btn-medium dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
				<i class="icon-wrench"></i>
				Manage
			</a>
		<#else>
			<div class="btn-group dept-settings">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-wrench"></i>
					Manage
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">

				<#if features.extensions>
				<li>
					<#assign extensions_url><@routes.extensionsettings department /></#assign>
					<@fmt.permission_button permission='Department.ManageExtensionSettings' scope=department action_descr='manage extension settings' href=extensions_url>
						<i class="icon-calendar"></i> Extensions
					</@fmt.permission_button>
			   </li>
			   </#if>

				<#if features.feedbackTemplates>
				<li>
					<#assign feedback_url><@routes.feedbacktemplates department /></#assign>
					<@fmt.permission_button permission='FeedbackTemplate.Create' scope=department action_descr='create feedback template' href=feedback_url>
						<i class="icon-comment"></i> Feedback templates
					</@fmt.permission_button>
			   </li>
			   </#if>

				<#if features.markingWorkflows>
				<li>
					<#assign markingflow_url><@routes.markingworkflowlist department /></#assign>
					<@fmt.permission_button permission='MarkingWorkflow.Read' scope=department action_descr='manage marking workflows' href=markingflow_url>
						<i class="icon-check"></i> Marking workflows
					</@fmt.permission_button>
				</li>
				</#if>

				<li id="feedback-report-button">
					<#assign feedbackrep_url><@routes.feedbackreport department /></#assign>
					<@fmt.permission_button permission='Department.DownloadFeedbackReport' scope=department action_descr='generate a feedback report' href=feedbackrep_url
											data_attr='data-container=body data-toggle=modal data-target=#feedback-report-modal'>
						<i class="icon-book"></i> Feedback report
					</@fmt.permission_button>
				</li>

				<li>
					<#assign copy_url><@routes.copyDepartmentsAssignments department /></#assign>
					<@fmt.permission_button
						permission='Assignment.Create'
						scope=department
						action_descr='copy existing assignments'
						href=copy_url>
						<i class="icon-share-alt"></i> Create assignments from previous
					</@fmt.permission_button>
				</li>
				<li>
					<#assign archive_url><@routes.archiveDepartmentsAssignments department /></#assign>
					<@fmt.permission_button
						permission='Assignment.Archive'
						scope=department
						action_descr='archive existing assignments'
						href=archive_url>
						<i class="icon-folder-close"></i> Archive assignments
					</@fmt.permission_button>
				</li>
				<li>
					<#assign settings_url><@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageDisplaySettings'
						scope=department
						action_descr='manage department settings'
						href=settings_url>
						<i class="icon-list-alt"></i> Settings
					</@fmt.permission_button>
				</li>
				</ul>
			</div>
		</#if>

		<#if can_manage_dept && modules?has_content>
		<div class="btn-group dept-show">
			<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no assignments are hidden. Click to show all modules." data-title-show="Modules with no assignments are hidden. Click to show all modules." data-title-hide="Modules with no assignments are shown. Click to hide them">
				<i class="icon-eye-open"></i>
				Show
			</a>
		</div>
		</#if>
	</div>
<#else>
	<h1>${department.name}</h1>
</#if>

<#if !modules?has_content && department.children?has_content>
<p>This department doesn't directly contain any modules. Check subdepartments.</p>
</#if>

<#list modules as module>
	<#assign can_manage=can.do("Module.ManageAssignments", module) />
	<#assign has_assignments= module.hasLiveAssignments />
	<#assign has_archived_assignments=false />
	<#list module.assignments as assignment>
	<#if assignment.archived>
		<#assign has_archived_assignments=true />
	</#if>
</#list>

<a id="${module_anchor(module)}"></a>
<div class="module-info<#if can_manage_dept && !has_assignments> empty</#if>">
	<div class="clearfix">
		<div class="btn-group module-manage-button">
		  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
		  <ul class="dropdown-menu pull-right">
		  	<#if can_manage >
					<#assign  module_managers_count = ((module.managers.includeUsers)![])?size />
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user"></i> Edit module permissions <!--<span class="badge">${module_managers_count}</span>-->
					</a></li>
				</#if>

				<li>
					<#assign create_url><@routes.createAssignment module /></#assign>
					<@fmt.permission_button
						permission='Assignment.Create'
						scope=module
						action_descr='create a new assignment'
						href=create_url>
						<i class="icon-plus"></i> Create new assignment
					</@fmt.permission_button>
				</li>
				<li>
					<#assign copy_url><@routes.copyModuleAssignments module /></#assign>
					<@fmt.permission_button
						permission='Assignment.Create'
						scope=module
						action_descr='copy existing assignments'
						href=copy_url>
						<i class="icon-share-alt"></i> Create assignments from previous
					</@fmt.permission_button>
				</li>
				<li>
					<#assign archive_url><@routes.archiveModuleAssignments module /></#assign>
					<@fmt.permission_button
						permission='Assignment.Archive'
						scope=module
						action_descr='archive existing assignments'
						href=archive_url>
						<i class="icon-folder-close"></i> Archive assignments
					</@fmt.permission_button>
				</li>

				<#if has_archived_assignments>
					<li><a class="show-archived-assignments" href="#">
						<i class="icon-eye-open"></i> Show archived assignments
						</a>
					</li>
				</#if>
		  </ul>
		</div>

		<h2 class="module-title"><@fmt.module_name module /></h2>
	</div>


	<#if has_assignments || has_archived_assignments>
	<div class="module-info-contents">
		<#list module.assignments as assignment>
		<#if !assignment.deleted>
		<#assign has_feedback = assignment.hasFullFeedback />

		<#-- Build feedback deadline rendering -->
		<#assign feedbackLabel = "" />
		<#assign feedbackDeadline = "" />
		<#if assignment.isClosed() && (features.submissions && assignment.collectSubmissions)>
			<#assign workingDaysAway = assignment.feedbackDeadlineWorkingDaysAway />
			<#if workingDaysAway lt 0>
				<#if assignment.hasUnreleasedFeedback>
					<#assign feedbackLabel><span class="label label-important use-tooltip" title="There is unreleased feedback, and the default deadline has passed. Drill down to see if there is good reason, such as late submissions.">Late feedback</span></#assign>
				<#elseif assignment.submissions?has_content && !assignment.hasReleasedFeedback>
					<#assign feedbackLabel><span class="label label-important use-tooltip" title="There have been submissions, but no feedback, and the default deadline has passed. Drill down to see if there is good reason, such as late submissions.">Feedback overdue</span></#assign>
				</#if>
			<#elseif workingDaysAway lte 5>
				<#assign feedbackLabel><span class="label use-tooltip" title=""The default deadline for feedback is less than five working days away.">Feedback due soon</span></#assign>
			</#if>

			<#assign feedbackDeadline>
				<div class="use-tooltip" title="The deadline for returning feedback is calculated using working days at Warwick.">
					<#if workingDaysAway == 0>
						<b>Feedback due today</b>
					<#elseif workingDaysAway lt 0>
						Feedback deadline was <@fmt.p -workingDaysAway "working day" /> ago,
						<@fmt.date date=assignment.feedbackDeadline includeTime=false />
					<#else>
						Feedback due in <@fmt.p workingDaysAway "working day" />,
						<@fmt.date date=assignment.feedbackDeadline includeTime=false />
					</#if>
				</div>
			</#assign>
		</#if>

		<div class="assignment-info<#if assignment.archived> archived</#if>">
			<div class="column1">
				<h3 class="name">
					<small>
						${assignment.name}
						<#if assignment.archived>
							(Archived)
						</#if>
					</small>
				</h3>

				<#if assignment.hasReleasedFeedback || features.submissions>
					<p class="feedback-published">
						<#assign urlforstudents><@url page="/module/${module.code}/${assignment.id}"/></#assign>
						<a href="${urlforstudents}" class="linkForStudents">Link for students</a>
						<a class="use-popover" id="popover-${assignment.id}" data-html="true"
							data-original-title="<span class='text-info'><strong>Link for students</strong></span>"
							data-content="This is the assignment page for students. You can give this web address or URL to students so that they can submit work and receive feedback and/or marks. Copy and paste it into an email or publish it on your module web page."><i class="icon-question-sign"></i></a>
					</p>
				</#if>
			</div>

			<div class="stats">
				<#assign membershipInfo = assignment.membershipInfo />
				<#assign memberCount>
					<#if membershipInfo.totalCount == 0>
						<span class="badge badge-mini<#if assignment.restrictSubmissions> badge-important</#if>" title="No enrolled students">0</span>
					<#else>
						<span class="badge badge-mini" title="<@fmt.p membershipInfo.totalCount "enrolled student"/> (${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)">${membershipInfo.totalCount}</span>
					</#if>
				</#assign>

				<#if assignment.openEnded>
					<div class="dates">
						<#noescape>${memberCount}</#noescape>
						<@fmt.interval assignment.openDate />, never closes
						(open-ended)
						<#if !assignment.opened>
							<span class="label label-warning">Not yet open</span>
						</#if>
					</div>
				<#else>
					<div class="dates">
						<#noescape>${memberCount}</#noescape>
						<@fmt.interval assignment.openDate assignment.closeDate />
						<#if assignment.closed>
							<span class="label label-warning">Closed</span>
						</#if>
						<#if !assignment.opened>
							<span class="label label-warning">Not yet open</span>
						</#if>

						<#noescape>${feedbackLabel!""}</#noescape>
					</div>
				</#if>

				<#if (features.submissions && assignment.collectSubmissions) || has_feedback>
					<div class="submission-and-feedback-count">
						<i class="icon-file"></i>
						<a href="<@routes.assignmentsubmissionsandfeedback assignment=assignment />" title="View all submissions and feedback">
							<@fmt.p assignment.submissions?size "submission" /><#--
							--><#if has_feedback> and ${assignment.countFullFeedback} item<#if assignment.countFullFeedback gt 1>s</#if> of feedback</#if><#--
						--></a>

						<#assign numUnapprovedExtensions = assignment.countUnapprovedExtensions />
						<#if numUnapprovedExtensions gt 0>
							<span class="has-unapproved-extensions">
								<i class="icon-info-sign"></i>
								<#if numUnapprovedExtensions gt 1>
									${numUnapprovedExtensions} extensions need granting
								<#else>
									1 extension needs granting
								</#if>
							</span>
						</#if>

						<#assign countUnreleasedFeedback = assignment.countUnreleasedFeedback />
						<#if countUnreleasedFeedback gt 0>
							<span class="has-unreleased-feedback">
								<i class="icon-info-sign"></i>
								${countUnreleasedFeedback}
								<#if countUnreleasedFeedback gt 1>
									items of feedback need publishing
								<#else>
									item of feedback needs publishing
								</#if>
							</span>
						</#if>

						<#noescape>${feedbackDeadline!""}</#noescape>
					</div>
				</#if>


			</div>
			<div class="assignment-buttons">
				<div class="btn-toolbar">
				<div class="btn-group">
				  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
				  <ul class="dropdown-menu pull-right">
					<li>
						<#assign edit_url><@routes.assignmentedit assignment /></#assign>
						<@fmt.permission_button
							permission='Assignment.Update'
							scope=assignment
							action_descr='edit assignment properties'
							href=edit_url>
            	<i class="icon-wrench"></i> Edit properties
            </@fmt.permission_button>
					</li>

					<li>
						<#assign archive_url><@url page="/admin/module/${module.code}/assignments/${assignment.id}/archive" /></#assign>
						<#if assignment.archived>
							<#assign archive_caption>Unarchive assignment</#assign>
						<#else>
							<#assign archive_caption>Archive assignment</#assign>
						</#if>
                        <@fmt.permission_button permission='Assignment.Archive' scope=assignment action_descr='${archive_caption}'?lower_case classes='archive-assignment-link ajax-popup' href=archive_url
                                        		data_attr='data-popup-target=.btn-group data-container=body'>
                        	<i class="icon-folder-close"></i> ${archive_caption}
                        </@fmt.permission_button>
					</a></li>


					<li class="divider"></li>

					<#if assignment.allowExtensions>
						<#if assignment.hasExtensions>
							<li>
								<#if can.do('Extension.ReviewRequest', assignment)>
									<#assign ext_caption='Grant extensions' />
								<#else>
									<#assign ext_caption='View extensions' />
								</#if>
								<#assign ext_url><@routes.extensions assignment /></#assign>
								<@fmt.permission_button
								permission='Extension.Read'
								scope=assignment
								action_descr=ext_caption?lower_case
								href=ext_url>
									<i class="icon-calendar"></i> ${ext_caption}
								</@fmt.permission_button>
							</li>
						<#else>
							<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="There have been no extensions requested for this assignment."><i class="icon-calendar"></i> Grant extensions </a></li>
						</#if>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Extensions are not allowed on this assignment."><i class="icon-calendar"></i> Grant extensions </a></li>
					</#if>

					<li class="divider"></li>


					<#if assignment.markingWorkflow?? && !assignment.markingWorkflow.studentsChooseMarker>
						<li>
							<#assign markers_url><@routes.assignMarkers assignment /></#assign>
							<@fmt.permission_button
								permission='Assignment.Update'
								scope=assignment
								action_descr='assign markers'
								href=markers_url>
	            	<i class="icon-user"></i> Assign markers
	            </@fmt.permission_button>
	          </li>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Marking workflow is not enabled for this assignment."><i class="icon-user"></i> Assign markers </a></li>
					</#if>

					<#if assignment.collectMarks>
						<li>
							<#assign marks_url><@routes.addMarks assignment /></#assign>
							<@fmt.permission_button
								permission='Marks.Create'
								scope=assignment
								action_descr='add marks'
								href=marks_url>
	            	<i class="icon-check"></i> Add marks
	            </@fmt.permission_button>
	          </li>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Mark collection is not enabled for this assignment."><i class="icon-check"></i> Add marks</a></li>
					</#if>

					<li>
						<#assign feedback_url><@routes.addFeedback assignment /></#assign>
						<@fmt.permission_button
							permission='Feedback.Create'
							scope=assignment
							action_descr='upload feedback'
							classes='feedback-link'
							href=feedback_url>
            	<i class="icon-upload"></i> Upload feedback
            </@fmt.permission_button>
          </li>

					<#if assignment.canPublishFeedback>
						<li>
							<#assign publishfeedbackurl><@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" /></#assign>
							<@fmt.permission_button permission='Feedback.Publish' scope=module action_descr='release feedback to students' href=publishfeedbackurl data_attr='data-container=body'>
								<i class="icon-envelope-alt"></i> Publish feedback
							</@fmt.permission_button>
						</li>
					<#else>
						<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" data-container="body" title="No current feedback to publish."><i class="icon-envelope-alt"></i> Publish feedback </a></li>
					</#if>


				  </ul>
				</div>
				</div>

			</div>
			<div class="end-assignment-info"></div>
		</div>
		</#if>
		</#list>

	</div>
	</#if>

</div>
</#list>
<div id="feedback-report-modal" class="modal fade"></div>
<#else>
<p>No department.</p>
</#if>
</#escape>
