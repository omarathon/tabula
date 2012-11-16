<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<h1>All departments</h1>

<ul class="department-list">
<#list departments as department>
  <li><a href="/sysadmin/departments/${department.code}/">${department.name} (${department.code})</a></li>
</#list>
</ul>

</#escape>