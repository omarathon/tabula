<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/feedback/${assignment.id}" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

 
<@spring.bind path="file">
<#if status.value??>
<h2>Upload received (but discarded)</h2>
<#if status.value.empty>
<h2>File was empty (or no file was uploaded)</h2>
<#else>
<h2>File size: ${status.value.size} bytes</h2>
<h2>File name: <code>${status.value.originalFilename}</code></h2>
</#if>
</#if>
</@spring.bind>


<div>
<@f.label path="uniNumber">
<@f.errors path="uniNumber" cssClass="error" />
Student university number
</@f.label>
<@f.input path="uniNumber" />
</div>

<div>
<@f.label path="file">
<@f.errors path="file" cssClass="error" />
File
</@f.label>
<input type="file" name="file" >

<#if addFeedbackCommand.fileAttachment??>
<input type="hidden" name="fileAttachment" value="${addFeedbackCommand.fileAttachment.id}">
${addFeedbackCommand.fileAttachment.name} (remove)
</#if>

</div>

<input type="submit" value="Submit">
</@f.form>

</#escape>