<#escape x as x?html>

<h1>Create small groups</h1>
<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

<@f.form method="post" action="${url('/groups/admin/module/${module.code}/groups/new')}" commandName="createSmallGroupSetCommand" cssClass="form-horizontal">
	<p class="progress-arrows">
		<span class="arrow-right active">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.createAndAddStudents}">Students</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.createAndAddGroups}">Groups</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit events">Events</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups">Allocate</span>
	</p>

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=true />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input
			type="submit"
			class="btn btn-success"
			name="${ManageSmallGroupsMappingParameters.createAndAddStudents}"
			value="Save and add students"
			title="Add students to these groups"
			/>
		<input
			type="submit"
			class="btn btn-primary"
			name="create"
			value="Save and exit"
			title="Save your groups and add students later"
			/>
		<a class="btn" href="<@routes.depthome module=module />">Cancel</a>
	</div>

</@f.form>

<script type="text/javascript">
	jQuery(function($) {
		$('#format').on('change', function() {
			var value = $(this).val();
			if (value === 'lecture') {
				var $checkbox = $('input[name="collectAttendance"]');
				if ($checkbox.is(':checked')) {
					$checkbox.prop('checked', false);
				}				
			}
		});
	});
</script>


</#escape>