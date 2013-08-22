<#-- Pre-save modal for notifications -->
<div class="modal fade hide" id="notify-modal" tabindex="-1" role="dialog" aria-labelledby="notify-modal-label" aria-hidden="true">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3 id="notify-modal-label">Send notifications</h3>
	</div>

	<div class="modal-body">
		<p>Notify these people via email of this change. Note that only students/tutors 
		whose allocations have been changed will be notified.</p>
		
		<div class="control-group">
			<div class="controls">
				<label class="checkbox">
					<input type="checkbox" name="notifyTutee" class="notifyTutee" checked />
					Students
				</label>

				<label class="checkbox">
					<input type="checkbox" name="notifyOldTutor" class="notifyOldTutor" checked />
					Old tutors
				</label>

				<label class="checkbox">
					<input type="checkbox" name="notifyNewTutor" class="notifyNewTutor" checked />
					New tutors
				</label>
			</div>
		</div>
	</div>

	<div class="modal-footer">
		<button type="submit" class="btn btn-primary">Save</button>
		<button type="button" class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
	</div>
</div>