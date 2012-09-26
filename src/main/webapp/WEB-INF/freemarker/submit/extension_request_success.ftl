<#escape x as x?html>
<#compress>
	<h1>You have requested an extension for <strong>${assignment.name}</strong></h1><br/>
	<p>
		You will receive an email when your request has been reviewed. If your application for an extension has not been
		approved before the deadline, hand in any work that you have completed before the deadline passes.
	</p>
	<p>
		If your circumstances change and you wish to provide additional information then you can edit your request by
		revisiting this page.
	</p>
	<a href="<@routes.assignment assignment=assignment />">Back to ${assignment.name}</a>
</#compress>
</#escape>