<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Grant extension for ${userFullName}</h3>
	</div>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/extensions/add')}" commandName="modifyExtensionCommand">
		<#include "_extension_fields.ftl" />
		<div class="modal-footer">
			<input type="submit" class="btn btn-success" value="Grant">
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>