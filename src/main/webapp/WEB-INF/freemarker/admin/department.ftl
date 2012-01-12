<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#if department??>
<h1>${department.name}</h1>

<#list modules as module>
<a id="module-${module.code}"></a>
<div class="module-info">
<h2><@fmt.module_name module /></h2>
	
	<div>
		<#assign  module_managers = ((module.participants.includeUsers)![]) />
		<@fmt.p module_managers?size "module manager"/><#if module_managers?size gt 0>:
			<@fmt.user_list_csv ids=module_managers />
		</#if>
		
		<span class="actions">
		<a title="Edit module permissions" href="<@url page="/admin/module/${module.code}/permissions" />">
		edit
		</a>
		</span>
	</div>
	
	<#if module.assignments!?size = 0>
		<p>This module has no assignments. 
		<span class="actions">
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</span>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<#assign has_feedback = assignment.feedbacks?size gt 0 >
		<div class="assignment-info">
			<h3 class="name">${assignment.name}</h3>
			<div class="stats">
				<div>
			    <@warwick.formatDate value=assignment.openDate pattern="d MMMM yyyy HH:mm" /> -<br>
			    <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm (z)" />
				</div>
				<div>
				${assignment.submissions?size} submissions,
				<#if has_feedback><a class="list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />"></#if>
				${assignment.feedbacks?size} feedback<#if has_feedback></a></#if>.
				
				</div>
			</div>
			<div class="actions">
				<a class="edit-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">edit details</a>
				<a class="feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">add feedback</a>
				<br>
				<#if has_feedback >
				<a class="list-feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/list" />">list feedback</a>
				</#if>
			</div>
			<div class="end-assignment-info"></div>
		</div>
		</#list>
		
		<div class="actions">
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</div>
	</#if>
	
</div>
</#list>

<#else>
<p>No department.</p>
</#if>

</#escape>