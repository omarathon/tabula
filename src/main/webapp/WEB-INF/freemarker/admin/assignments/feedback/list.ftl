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

<div class="feedback-list">
		<@form.selector_check_all />
<#list assignment.feedbacks as feedback>
	<div class="feedback-info">
		<@form.selector_check_row "feedbacks" feedback.id />
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${feedback.universityId}</h2>
		<div class="date">Uploaded <@fmt.date feedback.uploadedDate /></div>
		<div>
		<#if feedback.checkedReleased>
		<span class="label-green">Published</span>
		<#else>
		<span class="label-orange">Not published</span>
		</#if>
		</div>
    	<div class="attachments">Attachments:
			<#list feedback.attachments as attachment>
				${attachment.name} 
			</#list>
			<span class="actions">
			<a href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download/${feedback.id}/feedback-${feedback.universityId}.zip'/>">
			Download this feedback as ZIP file
			</a>
			</span>
		</div>
		<#if feedback.actualMark??>
			<div class="mark">
				Mark: ${feedback.actualMark}
			</div>
		</#if>
		<#if feedback.actualGrade??>
			<div class="grade">
				Grade: ${feedback.actualGrade}
			</div>
		</#if>
	</div>
</#list>
</div>

</#escape>
