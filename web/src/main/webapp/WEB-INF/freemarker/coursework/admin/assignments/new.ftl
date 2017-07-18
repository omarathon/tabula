<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#--
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines.
-->

<h1>Create assignment for <@fmt.module_name module /></h1>
<#assign commandName="addAssignmentCommand" />
<#assign command=addAssignmentCommand />
<#assign submitUrl><@routes.coursework.createAssignment module /></#assign>
<@f.form method="post" action=submitUrl commandName=commandName cssClass="form-horizontal">

	<div class="alert alert-success">
		<i class="icon-info-sign"></i>
		We've built a new version of coursework management. Please try it out and
		<a href="<@routes.cm2.createassignmentdetails module academicYear/>">create this assignment in the new version</a>
	</div>

	<#if command.prefilled>
		<div class="alert alert-success">
			<i class="icon-info-sign"></i>
			Some fields have been pre-filled from another recently created assignment for convenience.
			<a href="<@routes.coursework.createAssignment module />?prefillFromRecent=false">Don't do this</a>
		</div>
	</#if>

	<#if command.prefillAssignment??>
		<div class="alert alert-success">
			<i class="icon-info-sign"></i>
			Some fields have been pre-filled from assignment ${command.prefillAssignment.name}.
		</div>
	</#if>

	<@f.errors cssClass="error form-errors" />

<#assign newRecord=true />

<#include "_fields.ftl" />

	<div class="submit-buttons form-actions">
		<input type="submit" value="Create" class="btn btn-primary">
		<a class="btn" href="<@routes.cm2.departmenthome department=module.adminDepartment />">Cancel</a>
	</div>
</@f.form>

</#escape>