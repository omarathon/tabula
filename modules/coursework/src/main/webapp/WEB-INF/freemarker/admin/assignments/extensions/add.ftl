<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Grant extension for ${universityId}</h3>
	</div>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/extensions/add')}" commandName="modifyExtensionCommand">
		<#include "_extension_fields.ftl" />
	</@f.form>
</#escape>