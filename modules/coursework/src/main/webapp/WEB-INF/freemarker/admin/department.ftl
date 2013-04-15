<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#macro longDateRange start end>
	<#assign openTZ><@warwick.formatDate value=start pattern="z" /></#assign>
	<#assign closeTZ><@warwick.formatDate value=end pattern="z" /></#assign>
	<@fmt.date start /> 
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@fmt.date end /> (${closeTZ})
</#macro>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#if department??>

<script>

	</script>


<#-- TODO change this to a role check -->
<#assign can_manage_dept=can.do("Department.ManageExtensionSettings", department) />
<#if (features.extensions || features.feedbackTemplates) && can_manage_dept>
	<h1 class="with-settings">
		${department.name}
	</h1>
	
	<div class="btn-toolbar dept-toolbar">
	
	<div class="btn-group dept-settings">
		<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
			<i class="icon-wrench"></i>
			Manage
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu pull-right">
			<#if features.extensions>
				<li><a href="settings/extensions"><i class="icon-calendar"></i> Extensions</a></li>
			</#if>
			<#if features.feedbackTemplates>
				<li><a href="settings/feedback-templates"><i class="icon-comment"></i> Feedback templates</a></li>
			</#if>
			<#if features.markingWorkflows>
				<li><a href="markingworkflows"><i class="icon-check"></i> Marking workflows</a></li>
			</#if>
			<li id="feedback-report-button"><a href="<@url page="/admin/department/${department.code}/reports/feedback"/>"  data-toggle="modal"  data-target="#feedback-report-modal"><i class="icon-book"></i> Feedback report</a></li>
			<li><a href="settings/display"><i class="icon-list-alt"></i> Display settings</a></li>
		</ul>
	</div>
	
	<div class="btn-group dept-show">
		<a class="btn btn-medium use-tooltip" href="#" data-container="body" title="Modules with no assignments are hidden. Click to show all modules." data-title-show="Modules with no assignments are hidden. Click to show all modules." data-title-hide="Modules with no assignments are shown. Click to hide them">
			<i class="icon-eye-open"></i>
			Show
		</a>
	</div>

</div>
	
<#else>
	<h1>${department.name}</h1>
</#if>

<#list modules as module>
<#assign can_manage=can.do("Module.Read", module) />
<#assign has_assignments=(module.assignments!?size gt 0) />
<#assign has_archived_assignments=false />
<#list module.assignments as assignment>
		<#if assignment.archived>
			<#assign has_archived_assignments=true />
		</#if>
</#list>
<a id="${module_anchor(module)}"></a>
<div class="module-info<#if !has_assignments> empty</#if>">
	<div class="clearfix">
		<h2 class="module-title"><@fmt.module_name module /></h2>
		<div class="btn-group module-manage-button">
		  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
		  <ul class="dropdown-menu pull-right">
		  	<#if can_manage >	
					<#assign  module_managers_count = ((module.managers.includeUsers)![])?size />
					<li><a href="<@url page="/admin/module/${module.code}/permissions" />">
						<i class="icon-user"></i> Edit module permissions <!--<span class="badge">${module_managers_count}</span>-->
					</a></li>
				</#if>
				
				<li><a href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-folder-close"></i> Add assignment</a></li>
				
				<#if has_archived_assignments>
					<li><a class="show-archived-assignments" href="#">
							<i class="icon-eye-open"></i> Show archived assignments
						</a>
					</li>
				</#if>
				
		  </ul>
		</div>
	</div>
	
	
	<#if has_assignments>
	<div class="module-info-contents">
		<#list module.assignments as assignment>
		<#if !assignment.deleted>
		<#assign has_feedback = assignment.hasFullFeedback >
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
						data-original-title="<span class='text-info'><strong>Link for students</strong></span> <button type='button' onclick=&quot;jQuery('#popover-${assignment.id}').popover('hide')&quot; class='close'>&times;</button></span>" 
						data-content="This is the assignment page for students. You can give this web address or URL to students so that they can submit work and receive feedback and/or marks. Copy and paste it into an email or publish it on your module web page."><i class="icon-question-sign"></i></a>
				</p>
			</#if>

			</div>
			<div class="stats">
				<#if assignment.openEnded>
					<div class="dates">
						<@fmt.interval assignment.openDate />, never closes
						(open-ended)
						<#if !assignment.opened>
							<span class="label label-warning">Not yet open</span>
						</#if>
					</div>
				<#else>
					<div class="dates">
						<@fmt.interval assignment.openDate assignment.closeDate />
						<#if assignment.closed>
							<span class="label label-warning">Closed</span>
						</#if>
						<#if !assignment.opened>
							<span class="label label-warning">Not yet open</span>
						</#if>
											
					</div>
				</#if>

				
				<#if features.submissions && assignment.collectSubmissions && !features.combinedForm>
					<div class="submission-count">
						<#if assignment.submissions?size gt 0>
							<a href="<@routes.assignmentsubmissions assignment=assignment />" title="View all submissions">
								<@fmt.p assignment.submissions?size "submission" />
							</a>
						<#else>
							<@fmt.p assignment.submissions?size "submission" />
						</#if>
					</div>
				</#if>
				<#if !features.combinedForm>
					<div class="feedback-count">
						<#if has_feedback><a class="list-feedback-link" href="<@routes.assignmentfeedbacks assignment=assignment  />"></#if>

						${assignment.countFullFeedback} item<#if assignment.countFullFeedback gt 1>s</#if> feedback<#if has_feedback></a></#if>
						<#assign countUnreleasedFeedback = assignment.countUnreleasedFeedback />
												
						<#if countUnreleasedFeedback gt 0>
							<span class="has-unreleased-feedback">
								${countUnreleasedFeedback} 
								<#if countUnreleasedFeedback gt 1>
								items of feedback need publishing
								<#else>
								item of feedback needs publishing
								</#if>
							</span>					
						</#if>
					</div>
					
				</#if>
				<#if (features.combinedForm && ((features.submissions && assignment.collectSubmissions) || has_feedback))>	
					<div class="submission-and-feedback-count">							
						<i class="icon-file"></i> 
						<a href="<@routes.assignmentsubmissionsandfeedback assignment=assignment />" title="View all submissions and feedback">
							<@fmt.p assignment.submissions?size "submission" />
							<#if has_feedback> and ${assignment.countFullFeedback} item<#if assignment.countFullFeedback gt 1>s</#if> of feedback</#if>
						</a>

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
						
					</div>	
				</#if>			
				
				
			</div>
			<div class="assignment-buttons">
				<div class="btn-toolbar">
				<div class="btn-group">
				  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
				  <ul class="dropdown-menu pull-right">
					<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />"><i class="icon-wrench"></i> Edit properties</a></li>
					<li><a class="archive-assignment-link ajax-popup" data-popup-target=".btn-group" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/archive" />">
						<i class="icon-folder-close"></i>
						<#if assignment.archived>
							Unarchive assignment
						<#else> 
							Archive assignment
						</#if>
					</a></li>


					<li class="divider"></li>
					
					<#if assignment.allowExtensions>
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/extensions/" />"><i class="icon-calendar"></i> Grant extensions </a></li>
					<#else>
						<li class="disabled"><a><i class="icon-calendar"></i> Grant extensions </a></li>
					</#if>

					<li class="divider"></li>
					
					
					<#if assignment.markingWorkflow?? && !assignment.markingWorkflow.studentsChooseMarker>
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/assign-markers" />"><i class="icon-user"></i> Assign markers </a></li>
					<#else> 
						<li class="disabled"><a><i class="icon-user"></i> Assign markers </a></li>
					</#if>
					
					<#if assignment.collectMarks >
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/marks" />"><i class="icon-check"></i> Add marks</a></li>
					<#else>
						<li class="disabled"><a><i class="icon-check"></i> Add marks</a></li>
					</#if>
					
					<li><a class="feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />"><i class="icon-upload"></i> Upload feedback</a>
									
										
					<#if assignment.canPublishFeedback>
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />"><i class="icon-envelope"></i> Publish feedback </a></li>
					<#else>			
						<li class="disabled"><a><i class="icon-envelope"></i> Publish feedback </a></li>
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
