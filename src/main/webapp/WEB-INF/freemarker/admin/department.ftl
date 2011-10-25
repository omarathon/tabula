<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
 
<#if department??>
<h1>${department.name}</h1>

<#else>
<p>No department.</p>
</#if>

</#escape>