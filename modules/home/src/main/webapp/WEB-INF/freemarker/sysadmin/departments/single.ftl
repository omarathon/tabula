<#escape x as x?html>

<h1>${department.name}</h1>

<p><a href="permissions/">View department admins</a></p>

<#if department.children?size gt 0>
<p><a href="/coursework/admin/department/${department.code}/sort-modules">Arrange modules among sub-departments</a></p>

<p><strong>Sub-departments:</strong></p>
<ul>
<#list department.children as child>
	<li><a href='<@url page="/sysadmin/departments/${child.code}/" />'>${child.name}</a> (${child.modules?size} modules)</li>
</#list>
</ul>

<#if department.children?size gt 0>
	<h4>Sub-departments</h4>
	
	<ul>
		<#list department.children as child>
			<li><a href='<@url page="/sysadmin/departments/${child.code}/" />'>${child.name}</a></li>
		</#list>
	</ul>
</#if>

<#if department.hasParent>
<p>Parent department: <a href='<@url page="/sysadmin/departments/${department.parent.code}/" />'>${department.parent.name}</a></p>
<#--
<div class="btn-group">
	<a href="edit/" class="btn ">Edit details</a>
</div>
-->
<#else>
<#--
<div class="btn-group">
	<a href="#" class="btn "><i class="icon-plus"></i> New child department</a>
	<#if department.children?has_content>
	<a href="#" class="btn use-tooltip" title="Move modules into child departments"><i class="icon-list"></i> Arrange modules</a>
	</#if>
</div>
-->
</#if>

<p>
${department.modules?size} modules
</p>

<ul>
	<#list department.modules as module>
		<li>${module.code} - ${module.name!"Unknown"}</li>
	</#list>
</ul>
</#escape>
