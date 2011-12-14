<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#escape x as x?html>

<h1>All feedback for ${assignment.name}</h1>

<div class="actions">
<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/feedback/download-zip/feedback.zip'/>">
Download all as ZIP file
</a>
</div>

<div class="feedback-list">
<#list assignment.feedbacks as feedback>
	<div class="feedback-info">
		<h2 class="uni-id">${feedback.universityId}</h2>
		<div class="date">Uploaded <@fmt.date date=feedback.uploadedDate /></div>
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
