<#compress><#-- Included into every Freemarker template. -->
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#macro stylesheet path><link rel="stylesheet" href="<@url resource=path/>" type="text/css"></#macro>
<#macro script path><script src="<@url resource=path/>" type="text/javascript"></script></#macro>
</#compress>