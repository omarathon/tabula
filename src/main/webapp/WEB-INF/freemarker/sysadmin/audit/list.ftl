<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign form=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#escape x as x?html>

<h1>All audit events</h1>

<p>Results ${startIndex} - ${endIndex}</p>

<table>
<#list items as item>
<tr class="stage-${item.eventStage}">
<td>
<@warwick.formatDate value=item.eventDate pattern="d MMMM yyyy HH:mm" />
</td>
<td>${item.eventType}</td>
<td>${item.eventStage}</td>
<td>${item.userId!""}</td>
<td>${item.masqueradeUserId!""}</td>
</tr>
</#list>
</table>

<#if page gt 0>
<a href="?page=${page-1}">Newer</a>
</#if>
<a href="?page=${page+1}">Older</a>

</#escape>