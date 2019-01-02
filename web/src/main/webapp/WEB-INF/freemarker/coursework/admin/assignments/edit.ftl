<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#--
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines.
-->
<#assign commandName="editAssignmentCommand"/>
<#assign command=editAssignmentCommand />
<#assign canUpdateMarkingWorkflow=command.canUpdateMarkingWorkflow/>
<#assign route><@routes.coursework.assignmentedit assignment /></#assign>

<@f.form method="post" action=route modelAttribute=commandName cssClass="form-horizontal edit-assignment">

	<div class="alert alert-success">
		<i class="icon-info-sign"></i>
		This assignment was created in an old version of coursework management (CM1). 
		It's no longer possible to save changes to the assignment details. 
		For assistance, contact <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
	</div>

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
<@f.errors cssClass="error form-errors" />

<#assign newRecord=false />

<#include "_fields.ftl" />
<#--
</div>

</div>
</div>

<div id="form-editor-canvas">

</div>
-->
<div class="form-actions">
	<a class="btn" href="<@routes.cm2.departmenthome department=assignment.module.adminDepartment />">Cancel</a>
</div>
</@f.form>

<#if canDelete>
	<p class="alert alert-info">Did you create this assignment in error?
	You may <a href="<@routes.coursework.assignmentdelete assignment=assignment />" class="btn btn-danger">delete</a> it if you definitely won't need it again.</p>
<#else>
	<p class="alert alert-info">
	It's not possible to delete or edit this assignment. For assistance, contact <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
	</p>
</#if>

</#escape>