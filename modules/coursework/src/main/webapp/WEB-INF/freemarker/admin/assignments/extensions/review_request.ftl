<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Review extension request by ${universityId}</h3>
	</div>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/extensions/edit')}" commandName="modifyExtensionCommand">
		<div class="modal-body">
			<div class="control-group">
				<label><strong>Reason for extension request</strong></label>
				<div class="controls">
					${extension.reason}
				</div>
			</div>
			<div class="control-group">
				<label><strong>Requested extension deadline</strong></label>
				<div class="controls">
					<@fmt.date date=extension.requestedExpiryDate at=true/>
				</div>
			</div>
			<#if extension.attachments?has_content>
				<div class="control-group">
					<label><strong>Supporting documents</strong></label>
					<div class="controls">
						<ul>
							<#list extension.attachments as attachment>
								<li>
									<a href="<@routes.extensionreviewattachment assignment=assignment userid=universityId filename=attachment.name />">
										${attachment.name}
									</a>
								</li>
							</#list>
						</ul>
					</div>
				</div>
			</#if>
			<hr/>
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
					<@f.textarea class="big-textarea" path="extensionItems[0].approvalComments" />
				</div>
			</div>
		</div>
		<@f.hidden class="approveField" path="extensionItems[0].approved" />
		<@f.hidden class="rejectField" path="extensionItems[0].rejected" />
		<div class="modal-footer request-controls">
			<input id="approveButton" type="submit" class="btn btn-success" value="Approve" />
			<input id="rejectButton" type="submit" class="btn btn-danger" value="Reject" />
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>