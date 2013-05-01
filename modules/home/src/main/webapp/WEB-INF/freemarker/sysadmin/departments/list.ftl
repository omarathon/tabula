<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<h1>All ${departments?size} departments</h1>

<table class="department-list">
<tbody>
	<#list departments as department>
		<#if !department.hasParent>
			<@department_item department />
		</#if>
	</#list> 
</tbody>
</table>

<div id="department-list-edit-dropdown">
	
</div>

<#macro department_item department>
	<tbody>
	<tr>
		<td>
			${(department.code!'?')?upper_case}
		</td>
		<td>
		<a href="<@url page="/sysadmin/departments/${department.code}/" />"><strong>${department.name}</strong></a>
		
		<#if department.children?has_content>
			<table><tbody>
			<#list department.children as child>
				<@department_item child />
			</#list>
			</tbody></table>
		</#if>
		
		</td>
		<td>
		<#--
			<#if department.hasParent>
				<a href="#" class="btn btn-mini">Edit</a>
			<#else>
				<a href="#" class="btn btn-mini">New child</a>
				<a href="#" class="btn btn-mini">Arrange modules</a>
			</#if>
			
			-->
		</td>
	</tr>
	</tbody>
</#macro>


</#escape>