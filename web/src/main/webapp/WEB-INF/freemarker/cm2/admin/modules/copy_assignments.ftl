<#import "*/modal_macros.ftl" as modal />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<@cm2.headerMenu department academicYear />

	<#function route_function dept>
		<#local result><@routes.cm2.copy_assignments_previous dept academicYear /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Create assignments from previous" route_function "for" />

	<form action="" method="post" class="copy-assignments">
		<div class="submit-buttons">
			<input class="btn btn-primary confirm-btn" type="submit" value="Confirm">
			<a class='btn btn-default' href='<@url page=cancel />'>Cancel</a>
		</div>

		<#assign modules = copyAssignmentsCommand.modules />
		<#assign path = "copyAssignmentsCommand.assignments" />
		<#include "_assignment_list.ftl" />

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