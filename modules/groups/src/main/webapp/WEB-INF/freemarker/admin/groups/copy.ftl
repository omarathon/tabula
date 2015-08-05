<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<#if department??>
	<@fmt.deptheader "Copy small groups" "for" department routes "copyDepartment" />
<#else>
	<h1>Copy small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>
</#if>

<#if department??>
	<#assign submitUrl><@routes.copyDepartment department /></#assign>
	<#assign cancelUrl><@routes.departmenthome department=department year=copySmallGroupSetsCommand.targetAcademicYear /></#assign>
<#else>
	<#assign submitUrl><@routes.copyModule module /></#assign>
	<#assign cancelUrl><@routes.depthome module=module academicYear=copySmallGroupSetsCommand.targetAcademicYear /></#assign>
</#if>

<div class="fix-area">
	<@f.form method="post" action=submitUrl commandName="copySmallGroupSetsCommand" cssClass="form-horizontal">
		<input type="hidden" name="action" value="submit" id="action-submit">

		<@form.labelled_row "sourceAcademicYear" "From academic year">
			<@f.select path="sourceAcademicYear" id="sourceAcademicYear">
				<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
			</@f.select>
		</@form.labelled_row>

		<script type="text/javascript">
			jQuery(function($) {
				$('#sourceAcademicYear').on('change', function(e) {
					var $form = $(this).closest('form');
					$('#action-submit').val('refresh');

					$form.submit();
				});
			});
		</script>

		<@form.labelled_row "targetAcademicYear" "To academic year">
			<@f.select path="targetAcademicYear" id="targetAcademicYear">
				<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
			</@f.select>
		</@form.labelled_row>

		<p>Select sets of groups that you'd wish to copy:</p>

		<table class="table table-bordered table-striped small-group-sets-list" id="copy-groups-table">
			<thead>
				<tr>
					<th>
						<div class="check-all checkbox">
							<label><span class="very-subtle"></span>
								<input type="checkbox" class="collection-check-all use-tooltip" title="Select/unselect all">
							</label>
						</div>
					</th>
					<th>Set name</th>
					<th>
						Copy groups?
						<a class="use-popover" data-html="true"
						   title="Copy groups"
						   data-content="Copy groups as well as the set information (these won't have any students on them)">
							<i class="icon-question-sign"></i>
						</a>
					</th>
					<th>
						Copy events?
						<a class="use-popover" data-html="true"
						   title="Copy events"
						   data-content="Copy events to the copied groups">
							<i class="icon-question-sign"></i>
						</a>
					</th>
				</tr>
			</thead>
			<tbody>
				<#list copySmallGroupSetsCommand.smallGroupSets as state>
					<tr>
						<@spring.nestedPath path="smallGroupSets[${state_index}]">
							<td><@f.checkbox path="copy" cssClass="collection-checkbox" /></td>
							<td>
								<@f.hidden path="smallGroupSet" />
								<span class="h6 colour-h6"><@fmt.groupset_name state.smallGroupSet /></span>
							</td>
							<td><@f.checkbox path="copyGroups" /></td>
							<td><@f.checkbox path="copyEvents" /></td>
						</@spring.nestedPath>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons fix-footer">
			<input type="submit" class="btn btn-primary" value="Create" />
			<a class="btn dirty-check-ignore" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</div>

<script type="text/javascript">
	jQuery(function($) {
		$('input[type=checkbox][name$=copyEvents]').each(function () {
			var $eventsCb = $(this);
			var $groupsCb = $eventsCb.closest('tr').find('input[type=checkbox][name$=copyGroups]');
			var $copyCb = $eventsCb.closest('tr').find('input[type=checkbox][name$=copy]');

			$eventsCb.prop('disabled', !$groupsCb.prop('checked'));
			$groupsCb.on('change', function () {
				$eventsCb.prop('checked', $groupsCb.prop('checked'));
				$eventsCb.prop('disabled', !$groupsCb.prop('checked'));
			});

			$groupsCb.prop('disabled', !$copyCb.prop('checked'));
			$copyCb.on('change', function () {
				$groupsCb.prop('checked', $copyCb.prop('checked'));
				$groupsCb.prop('disabled', !$copyCb.prop('checked'));

				$eventsCb.prop('checked', $copyCb.prop('checked'));
				$eventsCb.prop('disabled', !$copyCb.prop('checked'));
			});
		});

		$('#copy-groups-table').bigList({
			onChange : function() {
				var $copyCb = this;
				$copyCb.closest("tr").toggleClass("selected", this.prop('checked'));

				var $eventsCb = $copyCb.closest('tr').find('input[type=checkbox][name$=copyEvents]');
				var $groupsCb = $copyCb.closest('tr').find('input[type=checkbox][name$=copyGroups]');

				$groupsCb.prop('checked', $copyCb.prop('checked'));
				$groupsCb.prop('disabled', !$copyCb.prop('checked'));

				$eventsCb.prop('checked', $copyCb.prop('checked'));
				$eventsCb.prop('disabled', !$copyCb.prop('checked'));

				var x = $('#copy-groups-table .collection-checkbox:checked').length;
				$('#selected-count').text(x+" selected");
			}
		});
	});
</script>

</#escape>