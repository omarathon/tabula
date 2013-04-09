<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<h1>All ${departments?size} departments</h1>

<#macro department_item department>
	<li>
		<a href="<@url page="/sysadmin/departments/${department.code}/" />"><strong>${department.name}</strong> (${(department.code!'?')?upper_case})</a>
		<#if department.children?has_content>
			<ul>
			<#list department.children as child>
				<@department_item child />
			</#list>
			</ul>
		</#if>
	</li>
</#macro>

<ul class="department-list">
	<#list departments as department>
		<#if !department.parent??>
			<@department_item department />
		</#if>
	</#list> 
</ul>

</#escape>