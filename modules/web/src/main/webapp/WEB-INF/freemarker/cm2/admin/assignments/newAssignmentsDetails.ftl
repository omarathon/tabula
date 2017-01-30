<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
	<#--
	HFC-166 Don't use #compress on this file because
	the comments textarea needs to maintain newlines.
	-->

<div class="deptheader">
	<h1>Create assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign commandName="command" />
	<#assign command=command />
	<@f.form method="post" action="${url('/cm2/admin/${module.code}/assignments/new')}"  cssClass="dirty-check">
		<@components.set_wizard true 'details'  />
		<#if command.prefilled>
			<div class="alert alert-success">
				<i class="icon-info-sign fa fa-info-circle"></i>
				Some fields have been pre-filled from another recently created assignment for convenience.
				<a href="${url('/cm2/admin/${module.code}/assignments/new')}?prefillFromRecent=false">Don't do this</a>
			</div>
		</#if>
		<#if command.prefillAssignment??>
			<div class="alert alert-success">
				<i class="fa fa-question-circle"></i>
				Some fields have been pre-filled from assignment ${command.prefillAssignment.name}.
			</div>
		</#if>
		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=true />
		<#include "_fields.ftl" />

		<div class="fix-footer">
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddFeedback}"
				value="Save and continue"
			/>
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>