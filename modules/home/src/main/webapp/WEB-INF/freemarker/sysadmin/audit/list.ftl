
<#escape x as x?html>

<h1>All audit events</h1>

<div style="float:right" class="very-subtle">
<#if lastIndexTime??>
<div>Last index ran at <@warwick.formatDate value=lastIndexTime pattern="d MMMM yyyy HH:mm" /></div>
</#if>
<#if lastIndexDuration??>
<div>Last index ran for ${lastIndexDuration.getStandardSeconds()} seconds.</div>
</#if>
</div>

<#if fromIndex>
<@f.form commandName="auditLogQuery" action="${url('/sysadmin/audit/search')}" method="POST">
<div class="input-append input-prepend">
<span class="add-on"><i class="icon-search"></i></span><@f.input path="query" placeholder="Query..." /><input class="btn" type="submit" value="Search">
</div>
</@f.form>
</#if>

<#macro paginator>
<#if page gt 0>
<a href="?page=${page-1}">Newer</a>
</#if>
<a href="?page=${page+1}">Older</a>
</#macro>

<p>Results ${startIndex} - ${endIndex} <@paginator /></p>

<table class="audit-events">
<tr>
<th class="date-column">Date</td>
<th>Event</th>
<th>Stage</th>
<th>Real user</th>
<th>Apparent user</th>
<th>Extra data</th>
</tr>
<#list items as item>
<tr class="stage-${item.eventStage}<#if item_index % 2 = 0> even-row</#if>">
<td class="date">
<@warwick.formatDate value=item.eventDate pattern="d MMMM yyyy HH:mm" />
</td>
<td>${item.eventType}</td>
<td>${item.eventStage}</td>
<td>${item.userId!""}</td>
<td>${item.masqueradeUserId!""}</td>
<td>

<#if fromIndex && item.parsedData??>
 <#assign j=item.parsedData/>
 <#list ['department','module','assignment','feedback','submission','studentId'] as key>
  <#if j[key]??><div><a href="?query=${key}:${j[key]}">${key}: ${j[key]}</a></div></#if>
 </#list>
 <div class="extra-data">${item.data}</div>
<#else>
 ${item.data}
</#if>

</td>
</tr>
</#list>
</table>

<@paginator />

</#escape>