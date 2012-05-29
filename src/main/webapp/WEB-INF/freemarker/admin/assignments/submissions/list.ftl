<#escape x as x?html>
<h1>All submissions for ${assignment.name}</h1>
<#assign module=assignment.module />

<div>
<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/download-zip/submissions.zip'/>"><i class="icon-download"></i>
Download all as ZIP file
</a>
&nbsp;
<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions.zip'/>" id="download-selected-button"><i class="icon-download"></i>
Download selected as ZIP file
</a>
&nbsp;
<a class="btn btn-danger" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/delete' />" id="delete-selected-button">Delete selected</a>
</div>

<#if submissions?size gt 0>
<div class="submission-list">
	<@form.selector_check_all />
<#list submissions as submission>

	<div class="submission-info">
		<@form.selector_check_row "submissions" submission.id />
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${submission.universityId}</h2>
		<div>
		<span class="date">Submitted <@fmt.date date=submission.submittedDate seconds=true capitalise=false /></span>
		<#if submission.late>
		  <span class="label-red">Late</span>
		</#if>
		</div>
	</div>

</#list>
</div>
<#else><#-- no submissions -->

<p>There are no submissions for this assignment.</p>

</#if>

</#escape>