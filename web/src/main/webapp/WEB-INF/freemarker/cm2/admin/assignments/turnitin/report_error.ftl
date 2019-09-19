<#-- There was an error redirecting the user to the Turnitin document viewer -->
<#escape x as x?html>
<h1>Whoops</h1>
<#if problem = "no-object">
  <p>
    This document doesn't appear to exist in Turnitin.
  </p>
<#elseif problem = "no-session">
  <p>
    We couldn't log you in to Turnitin.
  </p>
<#elseif problem = "api-error">
  <p>
    The Turnitin service reported an error: ${message}
  </p>
<#else>
    ${problem}
</#if>

<p>You can close this browser tab and return to the coursework application.</p>
</#escape>