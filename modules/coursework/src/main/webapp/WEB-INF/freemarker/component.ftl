<#ftl strip_text=true />

<#assign requestPath = (info.requestedUri.path!"") />

<#if requestPath?starts_with('/exams/')>
	<#assign bodyClass="exams-page" />
	<#assign siteHeader="Exams Management" />
	<#assign subsite=true />
	<#assign title="Exams Management" />
	<#assign name="exams" />
	<#assign context="/exams" />
	<#assign nonav=false />
<#else>
	<#assign bodyClass="coursework-page" />
	<#assign siteHeader="Coursework Management" />
	<#assign subsite=true />
	<#assign title="Coursework Management" />
	<#assign name="courses" />
	<#assign context="/coursework" />
	<#assign nonav=false />
</#if>