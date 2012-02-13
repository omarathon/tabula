<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#macro longDateRange start end>
	<#assign openTZ><@warwick.formatDate value=start pattern="z" /></#assign>
	<#assign closeTZ><@warwick.formatDate value=end pattern="z" /></#assign>
	<@warwick.formatDate value=start pattern="d MMMM yyyy HH:mm" /> 
	<#if openTZ != closeTZ>(${openTZ})</#if>
	-<br>
	<@warwick.formatDate value=end pattern="d MMMM yyyy HH:mm (z)" />
</#macro>
 
<#if department??>
<h1>${department.name}</h1>

<#list modules as module>
<#assign can_manage=can.manage(module) />
<a id="module-${module.code}"></a>
<div class="module-info">
<h2><@fmt.module_name module /></h2>
	
	
	<div>
		
		<#assign  module_managers = ((module.participants.includeUsers)![]) />
		<@fmt.p module_managers?size "module manager"/><#if module_managers?size gt 0>:
			<@fmt.user_list_csv ids=module_managers />
		</#if>
		<#if can_manage >	
		<span class="actions">
		<a title="Edit module permissions" href="<@url page="/admin/module/${module.code}/permissions" />">
		edit
		</a>
		</span>
		</#if>
	</div>
	
	<#if module.assignments!?size = 0>
		<p>This module has no assignments. 
		<span class="actions">
		<#if can_manage >
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</#if>
		</span>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<#assign has_feedback = assignment.feedbacks?size gt 0 >
		<div class="assignment-info">
			<div class="column1">
			<h3 class="name">${assignment.name}</h3>
			<#if assignment.anyReleasedFeedback>
			<p class="feedback-published">
				Feedback published. URL for students:
				<br>
				<input type="text" class="url-copy-area" value="<@url page="/module/${module.code}/${assignment.id}"/>">
			</p>
			</#if>
			</div>
			<div class="stats">
				<div>
				<@longDateRange assignment.openDate assignment.closeDate />
				</div>
				<div>
				${assignment.submissions?size} submissions,
				<#if has_feedback><a class="list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />"></#if>
				${assignment.feedbacks?size} feedback<#if has_feedback></a></#if>.
				</div>
				<#assign unreleasedFeedback=assignment.unreleasedFeedback />
				<#if unreleasedFeedback?size gt 0>
					<div class="has-unreleased-feedback">
					${unreleasedFeedback?size} feedback to publish.
					</div>
				</#if>
			</div>
			<div class="actions">
				<#if can_manage >
				<a class="edit-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">edit details</a>
				</#if>
				<#if !assignment.resultsPublished>
				<a class="feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">add feedback</a>
				</#if>
				<br>
				<#if has_feedback >
				<a class="list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />">list feedback</a>
				<#if assignment.canPublishFeedback>
				<a class="list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/publish" />">publish feedback</a>
				</#if>
				</#if>
			</div>
			<div class="end-assignment-info"></div>
		</div>
		</#list>
		
		<div class="actions">
		<#if can_manage >
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</#if>
		</div>
	</#if>
	
</div>
</#list>

<#else>
<p>No department.</p>
</#if>

</#escape>