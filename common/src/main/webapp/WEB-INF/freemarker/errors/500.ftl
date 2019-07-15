<#escape x as x?html>
  <h1>Error</h1>

  <p>Sorry, there's been a problem and we weren't able to complete your request.</p>

  <#if token??>
    <p>The token for this error is <strong>${token}</strong></p>
  </#if>

  <p>If the problem persists, please contact the <a href="mailto:tabula@warwick.ac.uk">Student Information Systems team</a><#if token??>, quoting the token above and any additional details</#if>.</p>

  <#include "_stackTrace.ftl" />
</#escape>
