<#escape x as x?html>
<h1>All submissions for ${assignment.name}</h1>
<#assign module=assignment.module />

<div>
<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions/download-zip/submissions.zip'/>"><i class="icon-download"></i>
Download all as ZIP file
</a>
<a class="btn long-running" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions.zip'/>" id="download-selected-button"><i class="icon-download"></i>
Download selected as ZIP file
</a>
<a class="btn" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/submissions.xml'/>"><i class="icon-download"></i>
XML
</a>
<a class="btn btn-danger" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/delete' />" id="delete-selected-button">Delete selected</a>

<#if features.turnitin>
<a class="btn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/turnitin' />" id="turnitin-submit-button">Submit to Turnitin</a>
</#if>

</div>

<#macro originalityReport r>
<img src="<@url resource="/static/images/icons/turnitin-16.png"/>">
<span class="similarity-${r.similarity}">${r.overlap}% similarity</span>
<span class="similarity-subcategories">
(Web: ${r.webOverlap}%,
Student papers: ${r.studentOverlap}%,
Publications: ${r.publicationOverlap}%)
</span>
</#macro>

<#if submissions?size gt 0>
<div class="submission-list">
	<@form.selector_check_all />
<#list submissions as item>
	<#assign submission=item.submission>

	<div class="submission-info">
		<@form.selector_check_row "submissions" submission.id />
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${submission.universityId}</h2>
		<div>
		<span class="date">Submitted <@fmt.date date=submission.submittedDate seconds=true capitalise=false /></span>
		<#if submission.late>
		  <span class="label-red">Late</span>
		</#if>
		<#if item.downloaded>
		  <span class="label-green">Downloaded</span>
		</#if>
		<#if item.submission.originalityReport??>
			<div class="originality-report">
				<@originalityReport item.submission.originalityReport />
			</div>
		</#if>
		</div>
	</div>

</#list>
</div>
<#else><#-- no submissions -->

<p>There are no submissions for this assignment.</p>

</#if>

</#escape>