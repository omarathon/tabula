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

<h1>Update event from Syllabus+</h1>
<h4><span class="muted">for</span>
	${smallGroup.name},
	<@components.eventShortDetails smallGroupEvent />

	<#noescape><#assign popoverContent><@components.eventDetails smallGroupEvent /></#assign>
	<a class="use-popover"
	   data-html="true"
	   data-content="${popoverContent?html}"><i class="icon-question-sign" style="font-size: 0.85em;"></i></a></#noescape>
</h4>

<p>Here are all the small group events for <@fmt.module_name module false /> in the central timetabling system Syllabus+.</p>

	<@f.form method="post" action="" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<table class="table table-bordered table-striped">
			<thead>
				<tr>
					<th>Timetable event</th>
				</tr>
			</thead>
			<tbody>
				<#list command.timetableEvents as timetableEvent>
					<tr>
						<td>
							<div class="pull-left">
								<@f.radiobutton path="index" value=timetableEvent_index cssClass="radio inline" />
							</div>
							<div style="margin-left: 20px;">
								<@eventDetails timetableEvent />
							</div>
						</td>
					</tr>
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
</#escape>