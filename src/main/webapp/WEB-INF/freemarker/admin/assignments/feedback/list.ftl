<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#escape x as x?html>

<h1>All feedback for ${assignment.name}</h1>

<p>
	<@fmt.p whoDownloaded?size "student has" "students have" /> downloaded their feedback to date. <span class="subtle">(recent downloads may take a couple of minutes to show up.)</span>
</p>

<div class="actions">
<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download-zip/feedback.zip'/>">
Download all as ZIP file
</a>
&nbsp;
<a href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/delete' />" id="delete-selected-button">Delete selected</a>
</div>

<div class="feedback-list">
		<@form.selector_check_all />
<#list assignment.feedbacks as feedback>
	<div class="feedback-info">
		<@form.selector_check_row "feedbacks" feedback.id />
		<#-- TODO show student name if allowed by department -->
		<h2 class="uni-id">${feedback.universityId}</h2>
		<div class="date">Uploaded <@fmt.date feedback.uploadedDate /></div>
		<#if feedback.checkedReleased>
		<div class="released">Has been published</div>
		</#if>
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
	</div>
</#list>
</div>

</#escape>
