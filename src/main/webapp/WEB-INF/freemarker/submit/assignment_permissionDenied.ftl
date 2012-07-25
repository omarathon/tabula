<#escape x as x?html>
	<#compress>
		<h1>${module.name} (${module.code?upper_case})
			<br><strong>${assignment.name}</strong></h1>

		<#if RequestParameters.requestedAccess??>
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

		<form action="<@routes.assignmentrequestaccess assignment />" method="POST">
			<button class=btn>Request access</button>
		</form>

	</#compress>
</#escape>