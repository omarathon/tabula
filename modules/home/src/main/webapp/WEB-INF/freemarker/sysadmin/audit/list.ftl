
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
	<#assign helpText>
		<p>Use <a href="http://lucene.apache.org/core/4_0_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Terms">Lucene query syntax</a> to search, for example:</p>
		<p><code>department:ps AND eventType:SubmitAssignment</code></p>
		<p><i class="icon-lightbulb icon-large"></i> The fields available in the audit index are <i>(Case-Sensitively)</i>:</p>
		<dl>
			<dt class="muted">assignment</dt><dd class="muted">Database GUID</dd>
			<dt>department</dt><dd>Two letter code</dd>
			<dt>eventDate</dt><dd>Flaky. Should be YYYY-MM-DD. Ish.</dd>
			<dt class="muted">eventId</dt><dd class="muted">Database GUID</dd>
			<dt>eventType</dt><dd>CamelCasedFormat</dd>
			<dt class="muted">feedback</dt><dd class="muted">Database GUID</dd>
			<dt class="muted">id</dt><dd class="muted">Mystery numeric value</dd>
			<dt>masqueradeUserId</dt><dd>ITS usercode</dd>
			<dt class="muted">module</dt><dd class="muted">Database GUID</dd>
			<dt class="muted">studentId</dt><dd class="muted">Uni number. Few useful entries.</dd>
			<dt class="muted">students</dt><dd class="muted">Uni number. Few useful entries.</dd>
			<dt class="muted">submission</dt><dd class="muted">Database GUID</dd>
			<dt>submissionIsNoteworthy</dt><dd>true or null, obviously</dd>
			<dt>userId</dt><dd>ITS usercode</dd>
		</dl>
	</#assign>
	<#assign helpLink>
		<a href="#"
			class="use-introductory<#if showIntro("audit-search-syntax")> auto</#if>"
			data-title="Query terms"
			data-trigger="click"
			data-placement="right"
			data-html="true"
			data-content="${helpText}"><i class="icon-question-sign icon-fixed-width" style="font-size:11pt"></i></a>
	</#assign>

	<@f.form commandName="auditLogQuery" action="${url('/sysadmin/audit/search')}" method="POST" cssClass="form-inline">
		<div class="input-append input-prepend">
			<span class="add-on"><i class="icon-search"></i></span><@f.input path="query" placeholder="Query..." /><input class="btn" type="submit" value="Search">
			<span class="help-inline"><#noescape>${helpLink}</#noescape></span>
		</div>
	</@f.form>
</#if>

<#macro paginator>
<#if page gt 0>
<a href="?page=${page-1}&query=${auditLogQuery.query?url}">Newer</a>
</#if>
<a href="?page=${page+1}&query=${auditLogQuery.query?url}">Older</a>
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