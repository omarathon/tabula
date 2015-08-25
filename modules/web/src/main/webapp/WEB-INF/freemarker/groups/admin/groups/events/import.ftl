<#macro eventDetails event><#compress>
	<div class="day-time">
		${(event.day.name)!""}
		<#if event.startTime??><@fmt.time event.startTime /><#else>[no start time]</#if>
		-
		<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if>
	</div>
	<#if event.staffUniversityIds?size gt 0>
		Tutor<#if event.staffUniversityIds?size gt 1>s</#if>:
		<@userlookup ids=event.staffUniversityIds lookupByUniversityId=true>
			<#list returned_users?keys?sort as id> <#compress> <#-- intentional space -->
				<#local returned_user=returned_users[id] />
				<#if returned_user.foundUser>
					${returned_user.fullName}<#if id_has_next>,</#if>
				<#else>
					${id}<#if id_has_next>,</#if>
				</#if>
			</#compress></#list>
		</@userlookup>
	</#if>
	<#if ((event.location.name)!"")?has_content>
		<div class="location">
			Room: <@fmt.location event.location />
		</div>
	</#if>
	<div class="running">
		Running: <#compress>
			<#if event.weekRanges?size gt 0 && event.day??>
				${weekRangesFormatter(event.weekRanges, event.day, smallGroupSet.academicYear, module.adminDepartment)}
			<#elseif event.weekRanges?size gt 0>
				[no day of week selected]
			<#else>
				[no dates selected]
			</#if>
		</#compress>
	</div>
</#compress></#macro>

<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<h1>Import events from Syllabus+</h1>
	<h4><span class="muted">for</span> ${smallGroupSet.name}</h4>

	<p>Below are all of the scheduled small groups defined for this module in Syllabus+, the central timetabling system.</p>

	<p>Use the dropdown to select which group to import the timetable event into, and optionally specify an existing
	   event to overwrite the properties of that event.</p>

	<@f.form method="post" action="" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<table class="table table-bordered table-striped">
			<thead>
				<tr>
					<th>Timetable Event</th>
					<th>Group</th>
					<th>Overwrite event</th>
				</tr>
			</thead>
			<tbody>
				<#list command.eventsToImport as eventToImport>
					<@spring.nestedPath path="eventsToImport[${eventToImport_index}]">
						<tr>
							<td><@eventDetails eventToImport.timetableEvent /></td>
							<td>
								<@f.select path="group">
									<@f.option value="">Do not import</@f.option>
									<@f.options items=groups itemLabel="name" itemValue="id" />
								</@f.select>
							</td>
							<td>
								<@f.select path="overwrite" disabled=true>
									<@f.option value="">--</@f.option>
									<#list groups as group>
										<optgroup label="${group.name}" data-id="${group.id}">
											<#list group.events as event>
												<@f.option value=event.id><#if event.startTime??><@fmt.time event.startTime /></#if> ${(event.day.name)!""}</@f.option>
											</#list>
										</optgroup>
									</#list>
								</@f.select>
							</td>
						</tr>
					</@spring.nestedPath>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
				/>
			<a class="btn" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>

	<script type="text/javascript">
		jQuery(function($) {
			$('select[name$="overwrite"]').each(function () {
				var $overwrite = $(this);
				var $group = $overwrite.closest('tr').find('select[name$="group"]');

				// Parse out a map of group ID to event options
				var events = {};
				$overwrite.find('optgroup').remove().each(function () {
					var $optgroup = $(this);
					var $events = $optgroup.find('option');
					var groupId = $optgroup.data('id');

					events[groupId] = $events;
				});

				var onChange = function () {
					$overwrite.find('option:not([value=""])').remove();

					if ($group.val().length > 0) {
						$overwrite.prop('disabled', false);
						$overwrite.append(events[$group.val()]);
					} else {
						$overwrite.prop('disabled', true);
					}
				};

				onChange();
				$group.on('change', onChange);
			});
		});
	</script>
</#escape>