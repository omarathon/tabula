<#escape x as x?html>

<h1>Background jobs</h1>

<#if jobs?size gt 0>
<table class="jobs-list">
<tr>
	<td>Created</td>
	<td>Type</td>
	<td>Status</td>
	<td>Progress</td>
	<td>Data</td>
</tr>
<#list jobs as job>
<tr>
	<td><@fmt.date date=job.createdDate seconds=true /></td>
	<td>${job.jobType}</td>
	<td>${job.status!''}</td>
	<td>
		<#if job.started>
			<span class="label-green">Started</span>
		<#else>
			<span class="label-orange">Waiting</span>
		</#if>
		<span class="percent">${job.progress!'?'}%</span>
	</td>
	<td class="data">${job.data}</td>
</tr>
</#list>
</table>
<#else>

<p>Il n'y a pas de jobs.</p>

</#if>

</#escape>