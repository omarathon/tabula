<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#if department??>
<h1>${department.name}</h1>

<#list modules as module>
<div class="module-info"><a id="module-${module.code}"></a>
<h2><@fmt.module_name module /></h2>
	
	<#if module.assignments!?size = 0>
		<p>This module has no assignments. 
		<span class="actions">
		<a href="<@url page="/admin/module/${module.code}/assignments/new" />">New assignment</a>
		</span>
		</p>
	<#else>
		<#list module.assignments as assignment>
		<div class="assignment-info">
			<h3 class="name">${assignment.name}</h3>
			<div class="stats">
			    <@warwick.formatDate value=assignment.openDate pattern="d MMMM yyyy HH:mm" /> -<br>
			    <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm (z)" />
				<br>
				${assignment.submissions?size} submissions,
				${assignment.feedbacks?size} feedback.
			</div>
			<div class="actions">
				<a class="edit-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">edit details</a>
				<a class="feedback-link" href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" />">return feedback</a>
				<br>
				<#if assignment.feedbacks?size gt 0>
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