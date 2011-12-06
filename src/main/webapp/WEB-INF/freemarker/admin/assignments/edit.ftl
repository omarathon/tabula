<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="/admin/module/${module.code}/assignments/edit/${assignment.id}" commandName="editAssignmentCommand">

<#--
<div id="form-editor-sidebar">

<@f.errors cssClass="error form-errors">
</@f.errors>

<div id="form-editor-tabs">

<div class="form-editor-tab" id="form-editor-addfield">
<h4>Generic form fields</h4>
<ul class="form-widget-list">
  <li class="widget widget-file">File attachment</li>
  <li class="widget widget-text">Text (1 line)</li>
  <li class="widget widget-textarea">Text area</li>
  <li class="widget widget-checkboxes">Checkboxes</li>
  <li class="widget widget-select">Select box</li>
  <li class="widget widget-radio">Multiple choice</li>
</ul>
</div>
<div class="form-editor-tab" id="form-editor-fieldprops">

</div>
<div class="form-editor-tab" id="form-editor-formprops">
-->

<#include "_fields.ftl" />
<#--
</div>

</div>
</div>

<div id="form-editor-canvas">

</div>
-->
<div class="submit-buttons actions">
<input type="submit" value="Save">
</div>
</@f.form>

</#escape>