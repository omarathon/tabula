<#escape x as x?html>
<h1>All submissions for ${assignment.name}</h1>

<div class="actions">
<a class="long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/download-zip/submissions.zip'/>">
Download all as ZIP file
</a>
</div>

<#if submissions?size gt 0>
<div class="submission-list">
<#list submissions as submission>

	<div class="submission-info">
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${submission.universityId}</h2>
		<div class="date">Submitted <@fmt.date date=submission.submittedDate /></div>
	</div>

</#list>
</div>
<#else><#-- no submissions -->

<p>There are no submissions for this assignment.</p>

</#if>

</#escape>