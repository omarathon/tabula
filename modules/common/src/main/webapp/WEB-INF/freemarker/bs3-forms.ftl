<#ftl strip_text=true />
<#--

Macros for customised form elements, containers and more complex pickers.

-->

<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#compress>
<#escape x as x?html>

<#macro errors path>
	<div class="has-error">
		<@f.errors path=path cssClass="help-block" />
	</div>
</#macro>

<#macro label path="" for="" cssClass="">
	<#if path?has_content>
		<@f.label path="${path}" for="${path}" cssClass="control-label ${cssClass}" ><#compress><#nested/></#compress></@f.label>
	<#elseif for?has_content>
		<label for="${for}" class="control-label ${cssClass}"><#compress><#nested/></#compress></label>
	<#else>
		<label class="control-label ${cssClass}"><#compress><#nested/></#compress></label>
	</#if>
</#macro>

<#macro form_group path="" checkbox=false radio=false>
	<#local errorClass="" />
	<#if path?has_content>
		<@spring.bind path=path>
			<#if status.error>
				<#local errorClass = "has-error" />
			</#if>
		</@spring.bind>
	</#if>
	<div class="form-group <#if checkbox>checkbox</#if> <#if radio>radio</#if> ${errorClass}">
		<#nested />
	</div>
</#macro>

<#macro labelled_form_group path="" labelText="">
	<@form_group path=path>
		<#if labelText?has_content>
			<@label path=path><#compress><#noescape>${labelText}</#noescape></#compress></@label>
		</#if>
		<#if path?has_content>
			<@spring.bind path=path>
				<#nested />
			</@spring.bind>
			<@errors path=path />
		<#else>
			<#nested />
		</#if>
	</@form_group>
</#macro>

<#macro radio>
	<div class="radio">
		<label><#compress><#nested /></#compress></label>
	</div>
</#macro>

<#macro checkbox path="">
	<#local errorClass="" />
	<#if path?has_content>
		<@spring.bind path=path>
			<#if status.error>
				<#local errorClass = "has-error" />
			</#if>
		</@spring.bind>
	</#if>
	<div class="checkbox ${errorClass}">
		<label><#compress>
			<#nested />
			<#if path?has_content>
			<@errors path=path />
		</#if>
		</#compress></label>
	</div>
</#macro>

<#macro selector_check_all>
	<div class="check-all">
		<input type="checkbox" class="collection-check-all">
	</div>
</#macro>

<#macro selector_check_row name value><#compress>
	<input type="checkbox" class="collection-checkbox" name="${name}" value="${value}">
</#compress></#macro>

</#escape>
</#compress>
