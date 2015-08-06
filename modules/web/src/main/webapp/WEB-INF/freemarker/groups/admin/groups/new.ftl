<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<h1>Create small groups</h1>
<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

<div class="fix-area">
<@f.form method="post" action="${url('/groups/admin/module/${module.code}/groups/new')}" commandName="createSmallGroupSetCommand" cssClass="form-horizontal dirty-check">

	<#if smallGroupSet??>
		<@components.set_wizard true 'properties' smallGroupSet />
	<#else>
		<#assign fakeSet = {'linked':false, 'groups':[]} />
		<@components.set_wizard true 'properties' fakeSet />
	</#if>

	<@f.errors cssClass="error form-errors" />

	<#assign newRecord=true />
	<#include "_fields.ftl" />

	<div class="submit-buttons fix-footer">
		<input
			type="submit"
			class="btn btn-success"
			name="${ManageSmallGroupsMappingParameters.createAndAddGroups}"
			value="Save and add groups"
			/>
		<input
			type="submit"
			class="btn btn-primary"
			name="create"
			value="Save and exit"
			/>
		<a class="btn dirty-check-ignore" href="<@routes.groups.depthome module=module />">Cancel</a>
	</div>

</@f.form>
</div>

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