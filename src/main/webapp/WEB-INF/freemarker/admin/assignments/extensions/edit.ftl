<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Modify extension for ${universityId}</h3>
	</div>
	<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/extensions/edit" commandName="editExtensionCommand">
		<div class="modal-body">
			<@f.input type="hidden" path="extensionItems[0].universityId" />

			<@form.label path="extensionItems[0].expiryDate">New submission deadline</@form.label>
			<@f.input id="picker0" path="extensionItems[0].expiryDate" class="date-time-picker" />

			<@form.label path="extensionItems[0].reason">Reason for extension</@form.label>
			<@f.textarea path="extensionItems[0].reason" />
		</div>
		<div class="modal-footer">
			<input type="submit" class="btn btn-primary" value="Save">
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>