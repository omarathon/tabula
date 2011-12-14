<#compress>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#macro module_name module>
<span class="code">${module.code?upper_case}</span> <span class="name">(${module.name})</span>
</#macro>
<#macro date date>
<@warwick.formatDate value=date pattern="d MMMM yyyy HH:mm:ss" />
</#macro>
</#compress>
