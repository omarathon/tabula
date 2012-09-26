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
<@f.hidden path="extensionItems[0].approved" value="true" />
<@f.hidden path="extensionItems[0].rejected" value="false" />
<div class="modal-footer">
	<input type="submit" class="btn btn-primary" value="Save">
	<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
</div>