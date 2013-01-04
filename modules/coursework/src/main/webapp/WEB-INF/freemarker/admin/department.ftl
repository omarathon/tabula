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

<#assign can_manage_dept=can.manage(department) />
<#if (features.extensions || features.feedbackTemplates) && can_manage_dept>
	<h1 class="with-settings">
		${department.name}
	</h1>
	<div class="btn-group dept-settings">
		<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown" href="#">
			<i class="icon-wrench"></i>
			Department settings
			<span class="caret"></span>
		</a>
		<ul class="dropdown-menu">
			<#if features.extensions>
				<li><a href="settings/extensions"><i class="icon-calendar"></i> Extensions</a></li>
			</#if>
			<#if features.feedbackTemplates>
				<li><a href="settings/feedback-templates"><i class="icon-file"></i> Feedback templates</a></li>
			</#if>
			<#if features.markSchemes>
				<li><a href="markschemes"><i class="icon-inbox"></i> Mark schemes</a></li>
			</#if>
			<li><a href="settings/display"><i class="icon-list-alt"></i> Display</a></li>
		</ul>
	</div>
<#else>
	<h1>${department.name}</h1>
</#if>

<#if notices.unpublishedAssignments?has_content>
<div class="alert alert-error">
	Some assigments have feedback that hasn't been published to students yet.
	<#list notices.unpublishedAssignments as a>
		<div>
			<a href="#${module_anchor(a.module)}">${a.name}</a>
		</div>
	</#list>
</div>
</#if>

<#list modules as module>
<#assign can_manage=can.manage(module) />
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
		  <a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">Manage <i class="icon-cog"></i><span class="caret"></span></a>
		  <ul class="dropdown-menu">
		  	<#if can_manage >	
					<#assign  module_managers_count = ((module.participants.includeUsers)![])?size />
					<li><a href="<@url page="/admin/module/${module.code}/permissions" />">
						Edit module managers <span class="badge">${module_managers_count}</span>
					</a></li>
				</#if>
				
				<li><a href="<@url page="/admin/module/${module.code}/assignments/new" />"><i class="icon-plus"></i> Add assignment</a></li>
				
				<#if has_archived_assignments>
					<li><a class="show-archived-assignments" href="#">Show archived assignments</a></li>
				</#if>
		  </ul>
		</div>
	</div>
	
	
	<#if has_assignments>
	<div class="module-info-contents">
		<#list module.assignments as assignment>
		<#if !assignment.deleted>
		<#assign has_feedback = assignment.fullFeedback?size gt 0 >
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
					${assignment.fullFeedback?size} feedback<#if has_feedback></a></#if>
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
				</#if>
				<#if (features.combinedForm && ((features.submissions && assignment.collectSubmissions) || has_feedback))>	
					<div class="submission-and-feedback-count">							
						<a href="<@routes.assignmentsubmissionsandfeedback assignment=assignment />" title="View all submissions and feedback">
							<@fmt.p assignment.submissions?size "submission" />
							<#if has_feedback> and ${assignment.fullFeedback?size} feedback</#if>
						</a>
						<#assign unreleasedFeedback=assignment.unreleasedFeedback />
						<#if unreleasedFeedback?size gt 0>
							<span class="has-unreleased-feedback">
							(${unreleasedFeedback?size} feedback to publish)
							</span>
						<#elseif has_feedback>
							<span class="no-unreleased-feedback">
							(all feedback published)
							</span>
						</#if>
					</div>	
				</#if>			
				
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
				  <a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">Manage <i class="icon-cog"></i><span class="caret"></span></a>
				  <ul class="dropdown-menu pull-right">
					<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">Edit properties</a></li>
					<li><a class="archive-assignment-link ajax-popup" data-popup-target=".btn-group" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/archive" />">
						<#if assignment.archived>
							Unarchive
						<#else>
							Archive
						</#if>
					</a></li>
					
					<li><a class="feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">Add feedback <i class="icon-plus"></i></a>
					
					<#if assignment.collectMarks >
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/marks" />">Add marks <i class="icon-plus"></i></a></li>
					</#if>
					
					<#if has_feedback>
						<#-- contained in the combined submissions and feedback list now 
						<a class="btn btn-block list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />">List feedback <i class="icon-list-alt"></i></a>
						-->
						<#if assignment.canPublishFeedback>
							<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />">Publish feedback <i class="icon-envelope"></i></a></li>
						</#if>
					</#if>
					<#if assignment.allowExtensions >
						<li><a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/extensions/" />">List extensions <i class="icon-calendar"></i></a></li>
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

<#else>
<p>No department.</p>
</#if>

</#escape>
