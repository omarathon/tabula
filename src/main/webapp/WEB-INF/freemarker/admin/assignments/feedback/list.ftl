<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#escape x as x?html>

<h1>All feedback for ${assignment.name}</h1>

<p>
	<@fmt.p whoDownloaded?size "student has" "students have" /> downloaded their feedback to date. <span class="subtle">(recent downloads may take a couple of minutes to show up.)</span>
</p>

<div>
<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download-zip/feedback.zip'/>"><i class="icon-download"></i>
Download all as ZIP file
</a>
&nbsp;
<a class="btn btn-danger" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/delete' />" id="delete-selected-button">Delete selected</a>
</div>
<#if assignment.feedbacks?size gt 0>
<div class="feedback-list">
	<@form.selector_check_all />
	<table id="feedback-table" class="table table-bordered table-striped">
		<tr>
            <th></th>
            <th>University ID</th>
            <th>Uploaded</th>
            <th>Status</th>
            <#if assignment.collectMarks>
                <th>Mark</th>
                <th>Grade</th>
            </#if>
            <th>Attachment(s)</th>
            <th></th>
        </tr>
		<#list assignment.feedbacks as feedback>
			<tr class="itemContainer">
				<td><@form.selector_check_row "feedbacks" feedback.id /></td>
				<td class="id">${feedback.universityId}</td>
				<td class="uploaded"><@fmt.date feedback.uploadedDate /></td>
				<td class="status">
					<#if feedback.checkedReleased>
						<span class="label-green">Published</span>
					<#else>
						<span class="label-orange">Not published</span>
					</#if>
				</td>
                <#if assignment.collectMarks>
                    <td class="mark">
                        <#if feedback.actualMark??>${feedback.actualMark}</#if>
                    </td>
                    <td class="grade">
                        <#if feedback.actualGrade??>${feedback.actualGrade}</#if>
                    </td>
                </#if>
				<td class="attachments">
					<#list feedback.attachments as attachment>${attachment.name}<#if attachment_has_next>, </#if></#list>
				</td>
				<td class="download">
					<a class="btn long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${feedback.id}/feedback-${feedback.universityId}.zip'/>">
						<i class="icon-download"></i>
						Download attachments
					</a>
				</td>
			</tr>
		</#list>
	</table>
</div>
<#else><#-- no feedback -->

<p>There is no feedback for this assignment.</p>

</#if>

</#escape>
