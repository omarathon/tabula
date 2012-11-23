<#escape x as x?html>
	<#compress>
		<h1>${module.name} (${module.code?upper_case})
			<br><strong>${assignment.name}</strong></h1>

		<#assign has_requested_access=RequestParameters.requestedAccess??/>
		<#if has_requested_access>
			<div class="alert alert-success">
				<a class="close" data-dismiss="alert">&times;</a>
				Thanks, we've sent a message to whoever is in charge of the module with all the necessary
				details.
			</div>
		</#if>

		<h2>You're not enrolled</h2>

		<p>
			This assignment is set up only to allow students who are enrolled on the relevant module.
			If you are reading this and you believe you should have access to this assignment,
			click the button below to send an automated message to the module convenor.
		</p>

		<#if has_requested_access>
			<a href="#" class="btn disabled">Request access</a>
		<#else>
			<form action="<@routes.assignmentrequestaccess assignment />" method="POST">
				<button class=btn>Request access</button>
			</form>
		</#if>

	</#compress>
</#escape>