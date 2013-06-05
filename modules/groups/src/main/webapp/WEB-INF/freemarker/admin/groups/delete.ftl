<#escape x as x?html>

	<h1>Delete small groups for <@fmt.module_name module /></h1>

	<#assign submitAction><@routes.deleteset smallGroupSet /></#assign>
	<@f.form method="post" action="${submitAction}" commandName="deleteSmallGroupSetCommand">
		<h2>${smallGroupSet.name} (${smallGroupSet.academicYear.label})</h2>
		
		<!-- global errors -->
		<@f.errors cssClass="error" />
		
		<p>
			You can delete small groups if they've been created in error. 
		</p>
		
		<@f.errors path="confirm" cssClass="error" />
		<@form.label checkbox=true>
			<@f.checkbox path="confirm" id="confirmCheck" />
			<strong> I definitely will not need these groups again and wish to delete them entirely.</strong>
		</@form.label>
		
		<div class="submit-buttons">
			<input type="submit" value="Delete" class="btn btn-danger">
			<a href="<@routes.editset smallGroupSet />" class="btn">Cancel</a>
		</div>
	</@f.form>

	<script type="text/javascript">
		jQuery(function($){
			$('#confirmCheck').change(function(){
				$('.submit-buttons input[type=submit]').attr('disabled', !this.checked).toggleClass('disabled', !this.checked);
			});
			$('.submit-buttons input[type=submit]').attr('disabled',true).addClass('disabled');
		})
	</script>

</#escape>