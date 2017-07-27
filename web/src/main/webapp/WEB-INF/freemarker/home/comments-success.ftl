<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<h1>Need help?</h1>

<#if command.recipient == Recipients.DeptAdmin>
	<p>Your message has been sent to your department administrator.</p>
<#else>
	<p>Thank you for contacting the Web Team. We've received your message and our support team will contact you shortly.</p>
</#if>

<p><a href="${previousPage}" class="btn btn-default">Return to previous page</a></p>

</#escape>