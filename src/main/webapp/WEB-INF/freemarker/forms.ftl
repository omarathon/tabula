<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>

<#macro row>
<div class="form-row"><#nested/></div>
</#macro>

<#macro field>
<div class="form-field"><#nested/></div>
</#macro>

<#macro labelled_row path label>
<@f.errors path=path cssClass="error" />
<@row>
	<@f.label path=path>
	${label}
	</@f.label>
	<@field>
	<#nested />
	</@field>
</@row>
</#macro>

<#--

Render a text field with user picker.

To bind with Spring:
<@userpicker path="yourBindPath" />

To not bind:
<@userpicker name="paramName" />

-->
<#macro userpicker path="" name="" list=false multiple=false>
<#if name="">
	<@spring.bind path=path>
	<#-- This handles whether we're binding to a list or not but I think
		it might still be more verbose than it needs to be. -->
	<#assign ids=[] />
	<#if list>
		<#assign ids=status.value />
	<#elseif status.value??>
		<#assign ids=[status.value] />
	</#if>
	<@render_userpicker expression=status.expression value=ids />
	</@spring.bind>
<#else>
	<@render_userpicker expression=name value=[] />
</#if>
</#macro>

<#macro render_userpicker expression value>
	<#if value?? && value?size gt 0>
	<#list value as id>
		<input type="text" class="user-code-picker" name="${expression}" value="${id}">
	</#list>
	<#else>
		<input type="text" class="user-code-picker" name="${expression}">
	</#if>
</#macro>

<#macro filewidget basename>
	<#-- <#local command=.vars[Request[commandVarName]] /> -->
	<@f.errors path="${basename}" cssClass="error" />
	<@f.errors path="${basename}.upload" cssClass="error" />
	<@f.errors path="${basename}.attached" cssClass="error" />
	<@row>
	<@f.label path="${basename}.upload">
	File
	</@f.label>
	<@field>
	<@spring.bind path="${basename}">
	<#local f=status.actualValue />
	<#if f.exists>
	<#list f.attached as attached>
		<#assign uploadedId=attached.id />
		<div id="attachment-${uploadedId}">
		<input type="hidden" name="${basename}.attached" value="${uploadedId}">
		${attached.name} <a id="remove-attachment-${uploadedId}" href="#">Remove attachment</a>
		</div>
		<div id="upload-${uploadedId}" style="display:none">
		<input type="file" name="${basename}.upload" >
		</div>
		<script>
		jQuery(function($){
		$('#remove-attachment-${uploadedId}').click(function(ev){
		  ev.preventDefault();
		  $('#attachment-${uploadedId}').remove(); 
		  $('#upload-${uploadedId}').show();
		  return false;
		});
		});
		</script>
	</#list>
	<#else>
	<input type="file" name="${basename}.upload" >
	</#if>
	</@spring.bind>
	</@field>
	</@row>
</#macro>