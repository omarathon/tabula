<#import "*/assignment_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<@cm2.assignmentHeader "Audit log" assignment "for" />

<table class="table" style="table-layout: fixed">
	<thead>
		<tr>
			<th style="width: 32px"></th>
			<th>Date</th>
			<th>Event</th>
			<th>User</th>
			<th>Changes</th>
		</tr>
	</thead>
	<tbody>
	<#list auditEvents as event>
		<tr class="expands-next-row" style="cursor: pointer; <#if event_index % 2 == 0>background-color: #f9f9f9</#if>">
			<td>
				<i class="expand-icon fa fa-fw fa-chevron-right"></i>
			</td>
			<td><@fmt.date event.date /></td>
			<td>${event.name}</td>
			<td>
					<@userlookup id=event.userId>
						<#if returned_user.foundUser>
							${returned_user.fullName} (${event.userId})
						<#else>
							${event.userId}
						</#if>
					</@userlookup>
			</td>
			<td>${event.summary!""}</td>
		</tr>
		<tr class="hidden">
			<td colspan="5" style="overflow: hidden; overflow-wrap: break-word">
				<dl>
					<dt>Event type</dt>
					<dd>
						<p>
							${event.name}
						</p>
					</dd>

					<#list event.fieldPairs as field>
						<#assign key=field._1() />
						<#assign value=field._2() />
						<#if event.fields[key]??>
							<dt>${key}</dt>
							<dd>
								<#if value?is_sequence>
									<ul>
										<#list value as item>
											<li>${item}</li>
										</#list>
									</ul>
								<#else>
									<p>
										<#if value?is_hash && value.class.simpleName == "DateTime">
											<@fmt.date value/>
										<#elseif value?is_boolean>
											${value?string("Yes", "No")}
										<#else>
											${value}
										</#if>
									</p>
								</#if>
							</dd>
						</#if>
					</#list>
				</dl>
			</td>
		</tr>
	</#list>
	</tbody>
</table>

</#escape>

<script>
	jQuery(function () {
		$('.expands-next-row').on('click', function () {
			var $row = $(this).next();
			var $icon = $(this).find('.expand-icon');

			if ($row.is(':visible')) {
				$row.addClass('hidden');
				$icon.removeClass('fa-chevron-down').addClass('fa-chevron-right');
			} else {
				$row.removeClass('hidden');
				$icon.removeClass('fa-chevron-right').addClass('fa-chevron-down');
			}
		});
	});
</script>

