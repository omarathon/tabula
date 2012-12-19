<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#if features.profiles>
<#include "../profile/search/form.ftl" />
<#else>
<p>Here is where you'd see student profiles.</p>
</#if>

</#escape>