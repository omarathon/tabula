<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
			<h3>Modify extension for ${universityId}</h3>
	</div>
	<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/extensions/edit" commandName="modifyExtensionCommand">
		<div class="modal-body">
			<@f.input type="hidden" path="extensionItems[0].universityId" value="${universityId}" />
			<div class="control-group">
				<@form.label path="extensionItems[0].expiryDate">New submission deadline</@form.label>
				<div class="controls">
					<@f.input id="picker0" path="extensionItems[0].expiryDate" class="date-time-picker" />
				</div>
			</div>
			<div class="control-group">
				<@form.label path="extensionItems[0].approvalComments">Comments</@form.label>
				<div class="controls">
					<@f.textarea path="extensionItems[0].approvalComments" />
				</div>
			</div>
		</div>
		<@f.hidden path="extensionItems[0].approved" value="1" />
		<@f.hidden path="extensionItems[0].rejected" value="0" />
		<div class="modal-footer">
			<input type="submit" class="btn btn-primary" value="Save">
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>