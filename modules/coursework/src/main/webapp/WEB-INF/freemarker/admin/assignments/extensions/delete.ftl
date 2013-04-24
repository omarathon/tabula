<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Revoke extension for ${userFullName}</h3>
	</div>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/extensions/delete/${universityId}')}" commandName="deleteExtensionCommand">
		<div class="modal-body">
			<p>
				${userFirstName} currently has an extension until <@fmt.date date=extension.expiryDate />
			</p>
			<@f.input type="hidden" path="universityIds[0]" />
		</div>
		<div class="modal-footer">
			<input type="submit" class="btn btn-danger" value="Revoke" />
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>