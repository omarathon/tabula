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

<#macro filewidget types basename multiple=true max=10 labelText="File">
<#-- <#local command=.vars[Request[commandVarName]] /> -->
	<#local elementId="file-upload-${basename?replace('[','')?replace(']','')?replace('.','-')}"/>
	<@labelled_form_group basename labelText>
		<@errors path="${basename}" />
		<@errors path="${basename}.upload" />
		<@errors path="${basename}.attached" />
		<@spring.bind path="${basename}">
			<#local f=status.actualValue />
			<div id="${elementId}">
				<#if f.exists>
					<#list f.attached as attached>
						<#local uploadedId=attached.id />
						<div class="hidden-attachment" id="attachment-${uploadedId}">
							<input type="hidden" name="${basename}.attached" value="${uploadedId}">
						${attached.name} <a id="remove-attachment-${uploadedId}" href="#">Remove attachment</a>
						</div>
						<div id="upload-${uploadedId}" style="display:none">

						</div>
					</#list>
				</#if>

				<#if multiple>
					<input type="file" id="${basename}.upload" name="${basename}.upload" multiple>
					<noscript>
						<#list (2..max) as i>
							<br><input type="file" name="${basename}.upload">
						</#list>
					</noscript>
				<#else>
					<input type="file" id="${basename}.upload" name="${basename}.upload" >
				</#if>
			</div>

			<small class="very-subtle help-block">
				<#if !multiple || max=1>One attachment allowed.<#else>Up to <@fmt.p max "attachment" /> allowed.</#if>
				<#if types?size &gt; 0>
					File types allowed: <#list types as type>${type}<#if type_has_next>, </#if></#list>.
				</#if>
				<#if multiple && max!=1>
					<span id="multifile-column-description" class="muted"><#include "/WEB-INF/freemarker/multiple_upload_help.ftl" /></span>
				</#if>
			</small>

			<script><!--

			jQuery(function($){
				var $container = $('#${elementId}'),
						$file = $container.find('input[type=file]'),
						$addButton;
				if (Supports.multipleFiles) {
					// nothing, already works
				} else {
					// Add button which generates more file inputs
					$addButton = $('<a>').addClass('btn btn-mini').append($('<i class="fa fa-plus"></i>').attr('title','Add another attachment'));
					$addButton.click(function(){
						$addButton
								.before($('<br/>'))
								.before($('<input type="file">').attr('name',"${basename}.upload"));
						if ($container.find('input[type=file]').length >= ${max}) {
							$addButton.hide(); // you've got enough file input thingies now.
						}
					});
					$file.after($addButton);
				}

				$container.find('.hidden-attachment a').click(function(ev){
					ev.preventDefault();
					$(this).parent('.hidden-attachment').remove();
					if ($addButton && $container.find('input[type=file],input[type=hidden]').length < ${max}) {
						$addButton.show();
					}
					return false;
				});
			});

			//--></script>
		</@spring.bind>
	</@labelled_form_group>
</#macro>

</#escape>
</#compress>
