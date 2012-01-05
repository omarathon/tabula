<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#escape x as x?html>

<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

<@spring.bind path="items">
<p>I've unpacked your zip file and I found feedback for ${status.value?size} students.</p>
</@spring.bind>

<#if addFeedbackCommand.unrecognisedFiles?size gt 0>
<p>There were some files in the zip which I didn't understand, and will be ignored:</p>
<ul class="file-list">
<#list addFeedbackCommand.unrecognisedFiles as unrecognisedFile>
<li>
	<#-- FIXME these values don't get bound on a submit -->
	<@f.hidden path="unrecognisedFiles[${unrecognisedFile_index}]" />
	${unrecognisedFile.name}
</li>
</#list>
</ul>
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
		<@spring.bind path="uniNumber">
		<td>
		
			${status.value}
			
		</td>
		</@spring.bind>
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
<input type="hidden" name="confirm" value="true">
<input type="submit" value="Confirm">
</div>
</@f.form>

</#escape>