<#escape x as x?html>

<h1>${department.name}</h1>

<#if department.parent??>
	<h6>
		<span class="muted">Parent department</span>
		<a href='<@url page="/sysadmin/departments/${department.parent.code}/" />'>${department.parent.name}</a>
	</h6>
</#if>

<h4>Actions</h4>

<ul>
	<li><a href="permissions/">View department admins</a></li>
	<li><a href="/profiles/department/${department.code}/tutors/upload">Upload personal tutors</a></li>
</ul>

<#if department.children?size gt 0>
	<h4>Sub-departments</h4>
	
	<ul>
		<#list department.children as child>
			<li><a href='<@url page="/sysadmin/departments/${child.code}/" />'>${child.name}</a></li>
		</#list>
	</ul>
</#if>

<h4>${department.modules?size} modules</h4>

<ul>
	<#list department.modules as module>
		<li>${module.code} - ${module.name!"Unknown"}</li>
	</#list>
</ul>
</#escape>