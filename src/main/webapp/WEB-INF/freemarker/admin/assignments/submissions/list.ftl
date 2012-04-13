<#escape x as x?html>
<h1>All submissions for ${assignment.name}</h1>
<#assign module=assignment.module />

<div class="actions">
<a class="long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/download-zip/submissions.zip'/>">
Download all as ZIP file
</a>
&nbsp;
<a href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/delete' />" id="delete-selected-button">Delete selected</a>
</div>

<#if submissions?size gt 0>
<div class="submission-list">
	<@form.selector_check_all />
<#list submissions as submission>

	<div class="submission-info">
		<@form.selector_check_row "submissions" submission.id />
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${submission.universityId}</h2>
		<div class="date">Submitted <@fmt.date_full date=submission.submittedDate /></div>
	</div>

</#list>
</div>
<#else><#-- no submissions -->

<p>There are no submissions for this assignment.</p>

</#if>

</#escape>