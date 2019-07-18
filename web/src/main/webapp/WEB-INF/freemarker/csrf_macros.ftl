<#escape x as x?html>
<#macro csrfHiddenInputField>
  <input type="hidden" name="${currentCsrfToken().tokenName}" value="${currentCsrfToken().tokenValue}" />
</#macro>
</#escape>