<#escape x as x?html>

<#macro jobs jobs>
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
</#macro>

<h1>Background jobs</h1>

<h2>Incomplete jobs</h2>

<#if unfinished?size gt 0>
<@jobs unfinished />
<#else>
<p>There are no incomplete jobs.</p>
</#if>

<h2>Completed jobs</h2>

<#if finished?size gt 0>
<@jobs finished />
<#else>
<p>There are no completed jobs.</p>
</#if>

</#escape>