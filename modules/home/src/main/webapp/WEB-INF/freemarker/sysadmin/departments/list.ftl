<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<h1>All ${departments?size} departments</h1>

<ul class="department-list">
<#list departments as department>
  <li><a href="<@url page="/sysadmin/departments/${department.code}/" />"><strong>${department.name}</strong> (${(department.code!'?')?upper_case})</a></li>
</#list>
</ul>

</#escape>