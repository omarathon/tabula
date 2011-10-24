<#escape x as x?html>

<h1>${department.name}</h1>

<p><a href="owners/">View department admins</a></p>

${department.modules?size} modules

<ul>
<#list department.modules as module>
<li>${module.code} - ${module.name!"Unknown"}</li>
</#list>
</ul>

</#escape>