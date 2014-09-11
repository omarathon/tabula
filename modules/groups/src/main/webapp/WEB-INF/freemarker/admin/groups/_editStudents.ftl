<#escape x as x?html>
	<#import "*/membership_picker_macros.ftl" as membership_picker />

	<input type="hidden" name="action" value="submit" id="action-submit" >

	<#if smallGroupSet.linked>
		<@form.row "members" "groupEnrolment">
			<p>There are <@fmt.p smallGroupSet.members.size "student" /> in ${smallGroupSet.name} (from ${smallGroupSet.linkedDepartmentSmallGroupSet.name}).</p>

			<div id="enrolment">
				<table id="enrolment-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-orangeLight">
					<thead>
						<tr>
							<th class="sortable">First name</th>
							<th class="sortable">Last name</th>
							<th class="sortable">ID</th>
							<th class="sortable">User</th>
						</tr>
					</thead>
					<tbody>
						<#list smallGroupSet.members.users as user>

							<tr class="membership-item item-type-sits">
								<td>
									<#if user.foundUser>
										${user.firstName}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if user.foundUser>
										${user.lastName}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if user.warwickId??>
										${user.warwickId}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if user.userId??>
										${user.userId}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
							</tr>
						</#list>
					</tbody>
				</table>
			</div>
		</@form.row>

		<script type="text/javascript">
			jQuery(function($) {
				$('#enrolment').find('.table-sortable').sortableTable();
			});
		</script>
	<#else>
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
	</#if>

	<script type="text/javascript">
		jQuery(function ($) {
			$('#action-submit').closest('form').on('click', '.update-only', function() {
				$('#action-submit').val('update');
				$('#action-submit').closest('form').find('[type=submit]').attr('disabled', true);
				$(this).attr('disabled', false);
			});
		});
	</script>
</#escape>