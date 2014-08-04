<#escape x as x?html>
	<#import "*/membership_picker_macros.ftl" as membership_picker />

	<input type="hidden" name="action" value="submit" id="action-submit" >

	<@form.row>
		<span class="legend" >Students <small>Select which students should be in this set of groups</small> </span>
	</@form.row>

	<@form.row "members" "groupEnrolment">
		<div class="alert alert-success" style="display: none;" data-display="fragment">
			The membership list for these groups has been updated
		</div>

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>
		<#assign hasMembers=(membersGroup?? && (membersGroup.allIncludedIds?size gt 0 || membersGroup.allExcludedIds?size gt 0)) />

		<#-- Members picker is pretty hefty so it is in a separate file -->
		<@membership_picker.header command />
		<#assign enrolment_url><@routes.enrolment smallGroupSet /></#assign>
		<@membership_picker.fieldset command 'group' 'group set' enrolment_url/>
	</@form.row>

<script type="text/javascript">
	jQuery(function ($) {
		$('#action-submit').closest('form').on('click', '.update-only', function() {
			$('#action-submit').val('update');
		});
	});
</script>
</#escape>