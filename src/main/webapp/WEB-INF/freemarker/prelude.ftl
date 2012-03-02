<#compress><#-- Included into every Freemarker template. -->
<#if JspTaglibs??>
	<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
	<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
	<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
	<#assign sso=JspTaglibs["/WEB-INF/tld/sso.tld"]>
	<#import "formatters.ftl" as fmt />
	<#import "forms.ftl" as form />
	<#import "routes.ftl" as routes />
	<#import "can_do.ftl" as can />
</#if>
<#macro stylesheet path><link rel="stylesheet" href="<@url resource=path/>" type="text/css"></#macro>
<#macro script path><script src="<@url resource=path/>" type="text/javascript"></script></#macro>
</#compress>