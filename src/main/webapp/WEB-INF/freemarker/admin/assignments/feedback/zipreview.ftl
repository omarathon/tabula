<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/batch" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

<@spring.bind path="items">
<p>I've unpacked your zip file and I found feedback for ${status.value?size} students.</p>
</@spring.bind>

<table class="batch-feedback-summary">
	<tr>
		<th>University ID</th>
		<th>Files</th>
	</tr>
<#list addFeedbackCommand.items as item>
	<tr>
	<@spring.nestedPath path="items[${item_index}]">
		<@spring.bind path="uniNumber">
		<td>${status.value}</td>
		</@spring.bind>
		<@spring.bind path="file.attached.name">
		<td>${status.value}</td>
		</@spring.bind>
	</@spring.nestedPath>
	</tr>
</#list>
</table>

<@form.labelled_row "archive" "Zip archive">
<input type="file" name="archive" />
</@form.labelled_row>

<div class="submit-buttons">
<input type="submit" value="Upload">
</div>
</@f.form>

</#escape>