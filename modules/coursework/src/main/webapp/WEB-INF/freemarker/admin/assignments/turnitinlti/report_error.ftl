<#-- There was an error redirecting the user to the Turnitin document viewer -->
<h1>Whoops</h1>
<#if problem = "no-object">
<p>
  This document doesn't appear to exist in Turnitin.
</p>
<#elseif problem = "unexpected-response-code">
<p>
  There was an error contacting the Turnitin service.  Please try again later.
</p>
</#if>

<p>You can close this browser tab and return to the coursework application.</p>