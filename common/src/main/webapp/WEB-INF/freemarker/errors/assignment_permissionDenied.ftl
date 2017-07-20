<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath == cm1Context || requestPath?starts_with("${cm1Context}/")>
	<#include "../coursework/submit/assignment_permissionDenied.ftl" />
<#else>
	<#include "../cm2/submit/assignment_permissionDenied.ftl" />
</#if>