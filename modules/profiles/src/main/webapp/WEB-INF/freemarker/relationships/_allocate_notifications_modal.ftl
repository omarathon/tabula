<#-- Pre-save modal for notifications -->
<div class="modal fade hide" id="notify-modal" tabindex="-1" role="dialog" aria-labelledby="notify-modal-label" aria-hidden="true">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3 id="notify-modal-label">Send notifications</h3>
	</div>

	<div class="modal-body">
		<p>Notify these people via email of this change. Note that only students/${relationshipType.agentRole}s 
		whose allocations have been changed will be notified.</p>
		
		<div class="control-group">
			<div class="controls">
				<label class="checkbox">
					<input type="checkbox" name="notifyStudent" class="notifyStudent" checked />
					${relationshipType.studentRole?cap_first}s
				</label>

				<label class="checkbox">
					<input type="checkbox" name="notifyOldAgent" class="notifyOldAgent" checked />
					Old ${relationshipType.agentRole}s
				</label>

				<label class="checkbox">
					<input type="checkbox" name="notifyNewAgent" class="notifyNewAgent" checked />
					New ${relationshipType.agentRole}s
				</label>
			</div>
		</div>
	</div>

	<div class="modal-footer">
		<button type="submit" class="btn btn-primary">Save</button>
		<button type="button" class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
	</div>
</div>