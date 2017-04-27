<#escape x as x?html>
	<#import "*/modal_macros.ftl" as modal />
	<h1>Create assignments from previous for ${title}</h1>

	<form action="" method="post" class="copy-assignments">
		<div class="submit-buttons">
			<input class="btn btn-primary confirm-btn" type="submit" value="Confirm">
			<a class='btn btn-default' href='<@url page=cancel />'>Cancel</a>
		</div>

		<#assign modules = copyAssignmentsCommand.modules />
		<#assign path = "copyAssignmentsCommand.assignments" />
		<#include "_assignment_list.ftl" />

		<@bs3form.labelled_form_group path="copyAssignmentsCommand.academicYear" labelText="Set academic year">
				<@f.select path="copyAssignmentsCommand.academicYear" id="academicYearSelect" cssClass="form-control">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			<div class="help-block">
				The new assignments' open and close dates will be offset by the appropriate number of years. You should check the open and close dates
				of all new assignments.
			</div>
		</@bs3form.labelled_form_group>


		<div class="submit-buttons">
			<input class="btn btn-primary confirm-btn" type="submit" value="Confirm">
			<a class='btn btn-default' href='<@url page=cancel />'>Cancel</a>
		</div>

	</form>


	<div class="modal fade" id="confirmModal">
		<@modal.wrapper cssClass="modal-xs">
			<@modal.body>
				<p>Are you sure you want to create these assignments?</p>
			</@modal.body>
			<@modal.footer>
				<div>
					<button type="button" class="btn btn-primary" name="submit">Confirm</button>
					<button type="button" class="btn btn-default" name="cancel">Cancel</button>
				</div>
			</@modal.footer>
		</@modal.wrapper>
	</div>

<script>
	jQuery(function($) {
		var $form = $('form.copy-assignments');
		var $confirmModal = $('#confirmModal');

		$('.modal-footer button[name="submit"]').on('click', function(e) {
			$form.submit();
			$confirmModal.modal('hide');
		});

		$('.modal-footer button[name="cancel"]').on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			$confirmModal.modal('hide');
		});

		$('.confirm-btn').on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			$confirmModal.modal('show');
		});

	});
</script>
</#escape>