<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="addFeedbackCommand" />

<@spring.bind path=commandName>
<#assign hasErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>

<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" commandName=commandName>

<h1>Submit feedback for ${assignment.name}</h1>

<@spring.bind path="items">
<p>
	I've unpacked your zip file and I found feedback for ${status.value?size} students.

	<#if hasErrors>
	However, there were some problems with its contents, which are shown below.
	You'll need to correct these problems with the zip and try again.
	</#if>

</p>
</@spring.bind>

<#if addFeedbackCommand.unrecognisedFiles?size gt 0>
<div class="unrecognised-files">
<div>There were some files in the zip which I didn't understand, and will be ignored:</div>
<ul class="file-list">
<#list addFeedbackCommand.unrecognisedFiles as unrecognisedFile>
<li>
	<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].path" />
	<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}].file" />
	${unrecognisedFile.path}
</li>
</#list>
</ul>
</div>
</#if>

<#if addFeedbackCommand.invalidFiles?size gt 0>
<div class="invalid-files">
<div>There were some files with problem names. You'll need to fix these and then try uploading again.</div>
<ul class="file-list">
<#list addFeedbackCommand.invalidFiles as invalidFile>
<li>
	<@f.hidden path="invalidFiles[${invalidFile_index}].path" />
	<@f.hidden path="invalidFiles[${invalidFile_index}].file" />
	${invalidFile.path}
</li>
</#list>
</ul>
</div>
</#if>

<table class="batch-feedback-summary">
	<tr>
		<th>University ID</th>
		<th>Files</th>
	</tr>
	
<@spring.bind path="items">
<#list status.value as item>
	<tr>
	<@spring.nestedPath path="items[${item_index}]">
		<@f.hidden path="uniNumber" />
		<td>
			<@f.errors path="uniNumber" cssClass="error" />
			<@spring.bind path="uniNumber">
			${status.value}
			</@spring.bind>
		</td>
		<#noescape>
		<@spring.bind path="file.attached" htmlEscape="false">
		<td>
			<#-- FIXME should be able to spring:bind to a list, not have to manually specify it like this -->
			<ul class="file-list">
			<#list addFeedbackCommand.items[item_index].file.attached as attached>
				<@f.hidden path="file.attached[${attached_index}]" />
				<li>${attached.name}</li>
			</#list>
			</ul>
		</td>
		</@spring.bind>
		</#noescape>
	</@spring.nestedPath>
	</tr>
</#list>
</@spring.bind>
</table>

<div class="submit-buttons">
<#if hasErrors>
<input type="submit" value="Confirm" disabled="true">
<#else>
<input type="hidden" name="confirm" value="true">
<input type="submit" value="Confirm">
</#if>
or <a href="<@routes.depthome module=assignment.module />">Cancel</a>
</div>
</@f.form>

</#escape>