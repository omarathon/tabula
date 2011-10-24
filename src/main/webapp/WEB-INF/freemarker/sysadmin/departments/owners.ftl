<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<h1>${department.name} admins</h1>

<p><a href="add/">Add a new admin</a></p>
<p><a href="..">Back to department</a></p>

<#assign ownerlist = owners.members />


<#if ownerlist?size = 0>
<p>This department has no admins.</p>
<#else>
<p>${ownerlist?size} admins</p>

<#if removeOwner??>
<@spring.bind path="removeOwner">
  <#list status.errorMessages as error>
  	<span class="error">${error}</span>
  </#list>
</@spring.bind>
</#if>

<ul>
<#list ownerlist as owner>
<li>${owner}
 <form method="post" action="/sysadmin/departments/${department.code}/owners/delete">
 	<input type="hidden" name="usercode" value="${owner}" />
 	<input type="submit" value="delete" onclick="return confirm('Are you sure you want to remove this department owner?')" />
 </form>
</li>
</#list>
</ul>
</#if>

</#escape>