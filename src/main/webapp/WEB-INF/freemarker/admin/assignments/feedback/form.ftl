<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" enctype="multipart/form-data" action="/admin/module/${module.code}/assignments/feedback/${assignment.id}" commandName="addFeedbackCommand">

<h1>Submit feedback for ${assignment.name}</h1>

<div>
<@f.label path="uniNumber">
<@f.errors path="uniNumber" cssClass="error" />
Student university number
</@f.label>
<@f.input path="uniNumber" />
</div>

<div>

<#-- TODO Move this macro to a shared place -->
<#macro filewidget basename>
<@f.errors path="${basename}" cssClass="error" />
<@f.errors path="${basename}.upload" cssClass="error" />
<@f.errors path="${basename}.attached" cssClass="error" />
<@f.label path="${basename}.upload">
File
</@f.label>

<#if addFeedbackCommand[basename].uploaded>
<#assign uploadedId=addFeedbackCommand[basename].attached.id />
<div id="attachment-${uploadedId}">
<input type="hidden" name="file.attached" value="${uploadedId}">
${addFeedbackCommand[basename].attached.name} <a id="remove-attachment-${uploadedId}" href="#">Remove attachment</a>
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
<#else>
<input type="file" name="${basename}.upload" >
</#if>

</#macro>

<@filewidget "file" />

</div>
<div class="submit-buttons">
<input type="submit" value="Submit">
</div>
</@f.form>

</#escape>