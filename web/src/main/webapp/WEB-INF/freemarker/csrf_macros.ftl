<#macro csrfHiddenInputField>
  <input type="hidden" name="${currentCsrfToken().tokenName}" value="${currentCsrfToken().tokenValue}" />
</#macro>