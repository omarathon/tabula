<#escape x as x?html>
  <h1>Error</h1>

  <p>
    Sorry, your file upload failed, because the browser
    stopped sending data before the file was completely received.
  </p>

  <#if token??>
    <p>The token for this error is <strong>${token}</strong></p>
  </#if>

  <p>If the problem persists, please contact the <a href="mailto:tabula@warwick.ac.uk">Student Information Systems team</a><#if token??>, quoting the token above and any additional details</#if>.</p>

  <#include "_stackTrace.ftl" />
</#escape>
