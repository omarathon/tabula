<#escape x as x?html>
	<#compress>
		<h1>${module.name} (${module.code?upper_case})
			<br><strong>${assignment.name}</strong></h1>

		<#assign has_requested_access=RequestParameters.requestedAccess??/>
		<#if has_requested_access>
			<div class="alert alert-success">
				<a class="close" data-dismiss="alert">&times;</a>
				Thanks, we've sent a message to a department administrator with all the necessary
				details.
			</div>
		</#if>

		<h2>You're not enrolled</h2>

		<p>
			This assignment is set up only to allow students who are enrolled on the relevant module.
			If you are reading this and you believe you should have access to this assignment,
			click the button below to send an automated message to an administrator for the department.
		</p>

		<#assign button_text>Request access for <strong>${user.fullName}</strong> (you)</#assign>

		<#if has_requested_access>
			<a href="#" class="btn disabled"><#noescape>${button_text}</#noescape></a>
		<#else>
			<form action="<@routes.assignmentrequestaccess assignment />" method="POST">
				<button class=btn><#noescape>${button_text}</#noescape></button>
			</form>
		</#if>

	</#compress>
</#escape>