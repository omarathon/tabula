<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#escape x as x?html>
	<div class="content">
		<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/extensions/detail/${universityId}')}" commandName="modifyExtensionCommand" cssClass="form-horizontal double-submit-protection">
			<@f.input type="hidden" path="extensionItems[0].universityId" value="${universityId}" />
			<div class="control-group">
				<@form.label path="extensionItems[0].expiryDate">New submission deadline</@form.label>
				<div class="controls">
					<@f.input id="picker0" path="extensionItems[0].expiryDate" class="date-time-picker" />
				</div>
			</div>
			<div class="control-group">
				<@form.label path="extensionItems[0].reviewerComments">Comments</@form.label>
				<div class="controls">
					<@f.textarea path="extensionItems[0].reviewerComments" />
				</div>
			</div>
			<input type="hidden" name="action" class="action" />
			<div class="submit-buttons">
				<input class="btn btn-primary" type="submit" value="Save">
				<a class="btn cancel-feedback" href="">Discard</a>
			</div>
		</@f.form>
	</div>
</#escape>
