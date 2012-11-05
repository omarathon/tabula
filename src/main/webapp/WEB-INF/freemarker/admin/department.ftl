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


<#if department??>

<#assign can_manage_dept=can.manage(department) />
<#if (features.extensions || features.feedbackTemplates) && can_manage_dept>
	<h1 class="with-settings">
		${department.name}
	</h1>
	<div class="btn-group dept-settings">
		<a class="btn btn-primary btn-mini" href="#"><i class="icon-white icon-wrench"></i> Department settings</a>
		<a class="btn btn-primary btn-mini dropdown-toggle" data-toggle="dropdown" href="#"><span class="caret"></span></a>
		<ul class="dropdown-menu">
			<#if features.extensions>
				<li><a href="settings/extensions"><i class="icon-calendar"></i> Extensions</a></li>
			</#if>
			<#if features.feedbackTemplates>
				<li><a href="settings/feedback-templates"><i class="icon-file"></i> Feedback templates</a></li>
			</#if>
		</ul>
	</div>
<#else>
	<h1>${department.name}</h1>
</#if>

<#list modules as module>
<#assign can_manage=can.manage(module) />
<#assign has_assignments=(module.assignments!?size gt 0) />
<#assign has_archived_assignments=false />
<a id="module-${module.code}"></a>
<div class="module-info<#if !has_assignments> empty</#if>">
<h2><@fmt.module_name module /></h2>
	<div class="module-info-contents">
	
	<div>
		
		<#assign  module_managers = ((module.participants.includeUsers)![]) />
		<@fmt.p module_managers?size "module manager"/><#if module_managers?size gt 0>:
			<@fmt.user_list_csv ids=module_managers />
		</#if>
		<#if can_manage >	
		
		<a class="btn btn-mini" title="Edit module permissions" href="<@url page="/admin/module/${module.code}/permissions" />">
		Edit
		</a>
		
		</#if>
	</div>
	
	<#if !has_assignments >
		<p>This module has no assignments. 
		<span class="btn-group">
		<a class="btn" href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-plus"></i> New assignment</a>
		</span>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<#if assignment.archived>
			<#assign has_archived_assignments=true />
		</#if>
		<#if !assignment.deleted>
		<#assign has_feedback = assignment.feedbacks?size gt 0 >
		<div class="assignment-info<#if assignment.archived> archived</#if>">
			<div class="column1">
			<h3 class="name">
				${assignment.name}
				<#if assignment.archived>
		            (Archived)
		        </#if>
			</h3>
			<h4>${assignment.academicYear.label}</h4>

			<div>
			<#if assignment.closed>
				<span class="label label-warning">Closed</span>
			</#if>
			<#if assignment.upstreamAssignment??>
			  <#assign _upstream=assignment.upstreamAssignment />
			  <span class="label label-info">SITS : ${_upstream.moduleCode?upper_case}/${_upstream.sequence}</span>
			</#if>
			</div>

			</div>
			<div class="stats">
				<div class="open-date">
					<span class="label-like"><@fmt.tense assignment.openDate "Opens" "Opened" /></span> <@fmt.date assignment.openDate /> 
				</div>
				<div class="close-date">
					<span class="label-like"><@fmt.tense assignment.closeDate "Closes" "Closed" /></span> <@fmt.date assignment.closeDate /> 
				</div>
				<#if features.submissions && assignment.collectSubmissions>
					<div class="submission-count">
						<#if assignment.submissions?size gt 0>
							<a href="<@routes.assignmentsubmissions assignment=assignment />" title="View all submissions">
								${assignment.submissions?size} submissions
							</a>
						<#else>
							${assignment.submissions?size} submissions
						</#if>
					</div>
				</#if>
				<div class="feedback-count">
				<#if has_feedback><a class="list-feedback-link" href="<@routes.assignmentfeedbacks assignment=assignment  />"></#if>
				${assignment.feedbacks?size} feedback<#if has_feedback></a></#if>
				<#assign unreleasedFeedback=assignment.unreleasedFeedback />
				<#if unreleasedFeedback?size gt 0>
					<span class="has-unreleased-feedback">
					(${unreleasedFeedback?size} to publish)
					</span>
				<#elseif has_feedback>
					<span class="no-unreleased-feedback">
					(all published)
					</span>
				</#if>
				</div>
				
				<#if assignment.anyReleasedFeedback || features.submissions>
				<p class="feedback-published">
					<#assign urlforstudents><@url page="/module/${module.code}/${assignment.id}"/></#assign>
					<a class="copyable-url" rel="tooltip" href="${urlforstudents}" title="This is the link you can freely give out to students or publish on your module web page. Click to copy it to the clipboard and then paste it into an email or page.">
						URL for students
					</a>
				</p>
				</#if>
				
			</div>
			<div class="assignment-buttons">
				<div class="btn-toolbar">
				<div class="btn-group">
		          <a class="btn dropdown-toggle" data-toggle="dropdown">Manage <i class="icon-cog"></i><span class="caret"></span></a>
		          <ul class="dropdown-menu pull-right">
		            <li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">Edit properties</a></li>
		            <li><a class="archive-assignment-link ajax-popup" data-popup-target=".btn-group" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/archive" />">
		            	<#if assignment.archived>
		            		Unarchive
		            	<#else>
		            		Archive
		            	</#if>
		            </a></li>
		          </ul>
		        </div>
		        </div>
			
				<a class="btn btn-block feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">Add feedback <i class="icon-plus"></i></a>
				<#if assignment.collectMarks >
					<a class="btn btn-block" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/marks" />">Add marks <i class="icon-plus"></i></a>
				</#if>
				<#if has_feedback>
					<a class="btn btn-block list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />">List feedback <i class="icon-list-alt"></i></a>
					<#if assignment.canPublishFeedback>
						<#if assignment.closed>
							<a class="btn btn-block" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />">Publish feedback <i class="icon-envelope"></i></a>
						<#else>
							<a class="btn btn-block disabled" href="#" title="You can only publish feedback after the close date.">Publish feedback <i class="icon-envelope"></i></a>
						</#if>
					</#if>
				</#if>
				<#if assignment.allowExtensions >
					<a class="btn btn-block" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/extensions/" />">List extensions <i class="icon-calendar"></i></a>
				</#if>
			</div>
			<div class="end-assignment-info"></div>
		</div>
		</#if>
		</#list>
		
		<div>
		<a class="btn" href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-plus"></i> New assignment</a>
		<#if has_archived_assignments>
			<a class="btn show-archived-assignments" href="#">Show archived assignents</a>
		</#if>
		</div>
	</#if>
	
	</div>
	
</div>
</#list>

<#else>
<p>No department.</p>
</#if>

</#escape>
