<#if user.loggedIn>
${user.fullName} (${user.apparentId}) from the ${user.departmentName} department has written some feedback about the Coursework app:
<#if user.masquerading>

(It's actually ${user.realUser.fullName} (${user.realId}) masquerading.)

</#if>
Their email address is ${user.email}
<#else>
An anonymous user has written some feedback about the Coursework app:
</#if>

<#--
<#compress>
<#if pleaseRespond>
They indicated that they would like a response.
<#else>
They did not indicate that they would like a response.
</#if>
</#compress>
-->
---

${message}

---