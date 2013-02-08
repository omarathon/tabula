<#escape x as x?html>

<h1>Background jobs</h1>

<#if jobs?size gt 0>
<table class="jobs-list">
<tr>
	<td>Created</td>
	<td>Type</td>
	<td>User</td>
	<td>Status</td>
	<td>Progress</td>
	<td>Data</td>
</tr>
<#list jobs as job>
<tr>
	<td><@fmt.date date=job.createdDate seconds=true /></td>
	<td>${job.jobType}</td>
	<td>
		<#if job.user.exists>
			${job.user.toString}
		</#if>
	</td>
	<td>${job.status!''}</td>
	<td>
		<#if job.started>
			<span class="label label-success">Started</span>
		<#else>
			<span class="label label-warning">Waiting</span>
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