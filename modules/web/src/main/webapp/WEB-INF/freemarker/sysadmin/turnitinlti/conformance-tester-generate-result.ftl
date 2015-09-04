<#escape x as x?html>

<h1>LTI Conformance tester (basic-lti-launch-request)</h1>

	<@f.form method="post" action="${url('${endpoint}')}" commandName="ltiConformanceTesterPopulateFormCommand" cssClass="form-horizontal">

	<#list response?keys as key>
		<#if response[key]?has_content>
			<div class="control-group"><label class="control-label" for="$key}">
				<div class="controls"><input type="text" value="${response[key]}" class="text" name="${key}" id="${key}" />
			</div>
		</#if>
	</#list>
	<@f.errors cssClass="error form-errors" />

	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>