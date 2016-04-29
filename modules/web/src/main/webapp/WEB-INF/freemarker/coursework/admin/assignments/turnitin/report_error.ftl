<#escape x as x?html>

<#-- There was an error redirecting the user to the Turnitin document viewer -->
<#if problem = "no-object">
	<h1>Turnitin Document Viewer not available</h1>
	<p>
		At 7pm on Wednesday 27th April Turnitin made a change to their software that
		prevents Tabula from directing you to the document viewer to view the full report.
	</p>
	<p>
		We have been in contact with Turnitin and are currently investigating whether or not this
		functionality will be made available to us again in some way.
	</p>
	<p>
		As such this functionality will be unavailable until further notice.
	</p>
<#elseif problem = "no-session">
	<h1>Whoops</h1>
	<p>
	  We couldn't log you in to Turnitin.
	</p>
<#elseif problem = "api-error">
	<h1>Whoops</h1>
	<p>
	  The Turnitin service reported an error: ${message}
	</p>
</#if>

<p>You can close this browser tab and return to the coursework application.</p>
</#escape>