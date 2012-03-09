time=${time.toDate()?string.long}
<#compress>
<#if requestInfo??>
<#if requestInfo.user?? && requestInfo.user.exists>
user=${requestInfo.user.realId}
<#if requestInfo.user.masquerading>
masqueradingAs=${requestInfo.user.apparentId}
</#if>
</#if>
</#if>
<#if request??>
request.requestURI=${request.requestURI}
</#if>
</#compress>

${exceptionStack}

