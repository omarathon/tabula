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

<a class="btn btn-warn" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised' />" id="mark-plagiarised-selected-button">Mark selected plagiarised</a>




</div>

<#macro originalityReport attachment>
<#local r=attachment.originalityReport />
<div>
${attachment.name}
<img src="<@url resource="/static/images/icons/turnitin-16.png"/>">
<span class="similarity-${r.similarity}">${r.overlap}% similarity</span>
<span class="similarity-subcategories">
(Web: ${r.webOverlap}%,
Student papers: ${r.studentOverlap}%,
Publications: ${r.publicationOverlap}%)
</span>
<div>
</#macro>

<#if submissions?size gt 0>
<div class="submission-list">
	<@form.selector_check_all />
    <table id="submission-table" class="table table-bordered table-striped">
        <tr>
            <th></th>
            <th>University ID</th>
            <th>Submitted</th>
            <th>Status</th>
            <#if hasOriginalityReport><th>Originality Report</th></#if>
        </tr>
        <#list submissions as item>
	        <#assign submission=item.submission>
            <tr class="itemContainer" <#if submission.suspectPlagiarised> data-plagiarised="true" </#if> >
       
                <td><@form.selector_check_row "submissions" submission.id /></td>
                <td class="id">${submission.universityId}</td>
                <#-- TODO show student name if allowed by department -->
		        <td class="submitted">
                    <span class="date">
                        <@fmt.date date=submission.submittedDate seconds=true capitalise=true />
                    </span>
                </td>
                <td class="status">
                    <#if submission.late>
                        <span class="label-red">Late</span>
					<#elseif  submission.authorisedLate>
						<span class="label-blue">Authorised Late</span>
                    </#if>
                    <#if item.downloaded>
                        <span class="label-green">Downloaded</span>
                    </#if>
                    <#if submission.suspectPlagiarised>
                    	<span class="label-orange">Suspect Plagiarised</span>
                    </#if>
                </td>
                <#if hasOriginalityReport>
                    <td class="originality-report">
                    	<#list item.submission.allAttachments as attachment>
                    	<!-- Checking originality report for ${attachment.name} ... -->
                        <#if attachment.originalityReport??>
                            <@originalityReport attachment />
                        </#if>
                        <a target="turnitin-viewer" href="<@url page='/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-report/${attachment.id}'/>">View report</a>
                        </#list>
                    </td>
                </#if>
            </tr>
        </#list>
    </table>
</div>
<#else><#-- no submissions -->

<p>There are no submissions for this assignment.</p>

</#if>

</#escape>