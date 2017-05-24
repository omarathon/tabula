<#escape x as x?html>
	<div class="deptheader">
		<h1>Delete assignment</h1>
		<h4 class="with-related">${assignment.name}</h4>
	</div>
	<#assign submitUrl><@routes.cm2.assignmentdelete assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName="deleteAssignmentCommand">
		<!-- global errors -->
		<@f.errors cssClass="error" />

		<p>
			You can delete an assignment if it's been created in error.
		</p>

		<@f.errors path="confirm" cssClass="error" />
		<@bs3form.labelled_form_group path="confirm" labelText="">
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" id="confirmCheck" />
				<strong> I definitely will not need this assignment again and wish to delete it entirely.</strong>
			</@bs3form.checkbox>
		</@bs3form.labelled_form_group>


		<div class="submit-buttons">
			<input type="submit" value="Delete" class="btn btn-danger">
			<a href="<@routes.cm2.editassignmentdetails assignment=assignment />" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>

	<script>
		jQuery(function($){
			$('#confirmCheck').change(function(){
				$('.submit-buttons input[type=submit]').attr('disabled', !this.checked).toggleClass('disabled', !this.checked);
			});
			$('.submit-buttons input[type=submit]').attr('disabled',true).addClass('disabled');
		})
	</script>

</#escape>