<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<h1>Problems, questions?</h1>

<#if command.recipient == Recipients.DeptAdmin>
	<p>Your message has been sent to your department administrator.</p>
<#else>
	<p>Thank you for your feedback.</p>
</#if>

<p><a href="${previousPage}" class="btn btn-default">Return to previous page</a></p>

</#escape>