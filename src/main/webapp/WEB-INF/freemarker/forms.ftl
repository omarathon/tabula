<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
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

<#macro filewidget basename>
	<#local command=.vars[Request[commandVarName]] />
	<@f.errors path="${basename}" cssClass="error" />
	<@f.errors path="${basename}.upload" cssClass="error" />
	<@f.errors path="${basename}.attached" cssClass="error" />
	<@row>
	<@f.label path="${basename}.upload">
	File
	</@f.label>
	<@field>
	<#if command[basename].uploaded>
	<#list addFeedbackCommand[basename].attached as attached>
		<#assign uploadedId=attached.id />
		<div id="attachment-${uploadedId}">
		<input type="hidden" name="file.attached" value="${uploadedId}">
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
	</@field>
	</@row>
</#macro>