<#-- companion to _events.ftl, so that the modals
	don't have to be rendered right next to each event. -->

<#escape x as x?html>
<@spring.bind path="academicYear">
<#assign academicYear=status.actualValue />
</@spring.bind>

<#macro modal group_index event_index>
	<div id="group${group_index}-event${event_index}-modal" class="modal hide fade refresh-form" tabindex="-1" role="dialog" aria-labelledby="group${group_index}-event${event_index}-modal-label" aria-hidden="true">
		<div class="modal-header">
			<h3 id="group${group_index}-event${event_index}-modal-label"><#nested/></h3>
		</div>
		<div class="modal-body">
			<@form.labelled_row "tutors" "Tutors">
				<@form.flexipicker path="tutors" placeholder="User name" htmlId="group${group_index}-event${event_index}-tutors" list=true multiple=true />
			</@form.labelled_row>

			<@form.labelled_row path="weeks" label="Terms" fieldCssClass="controls-row">
				<@spring.bind path="weeks">
					<#local allWeeks=(status.actualValue)![] />
				</@spring.bind>

				<#list allTermWeekRanges as term_week_range>
					<#local weeks = term_week_range.weekRange.toWeeks />
					<#local full = term_week_range.isFull(allWeeks) />
					<#local partial = !full && term_week_range.isPartial(allWeeks) />

					<div class="span1">
						<@form.label checkbox=true>
							<input id="group${group_index}-event${event_index}-weeks${term_week_range_index}-checkbox" type="checkbox" value="true" <#if full || partial>checked="checked"</#if> data-indeterminate="<#if partial>true<#else>false</#if>" data-target="#group${group_index}-event${event_index}-weeks${term_week_range_index}">
							Term ${term_week_range_index+1}
						</@form.label>

						<@f.select path="weeks" id="group${group_index}-event${event_index}-weeks${term_week_range_index}" size="${weeks?size?c}" multiple="true" cssClass="individual-weeks span1">
						<#list weeks as week>
							<@f.option value="${week}" label="${week_index+1}" />
						</#list>
						</@f.select>
					</div>
				</#list>

				<div class="very-subtle individual-weeks span3" style="margin-top: 30px;">
					Drag to select a week range. Hold Ctrl and click to select and deselect individual weeks.
				</div>

				<div class="clearfix"></div>
				<button type="button" class="btn btn-mini" data-toggle="elements" data-target=".individual-weeks">Select individual weeks</button>
			</@form.labelled_row>

			<@form.labelled_row "day" "Day">
				<@f.select path="day" id="group${group_index}-event${event_index}-day">
					<@f.option value="" label=""/>
					<@f.options items=allDays itemLabel="name" itemValue="asInt" />
				</@f.select>
			</@form.labelled_row>

			<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
			<@form.labelled_row "startTime" "Start time">
				<@f.input path="startTime" cssClass="time-picker" />
			</@form.labelled_row>

			<@form.labelled_row "endTime" "End time">
				<@f.input path="endTime" cssClass="time-picker" />
			</@form.labelled_row>

			<@form.labelled_row "location" "Location">
				<@f.input path="location" />
			</@form.labelled_row>
		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Done</button>
		</div>
	</div>
</#macro>

<#list groups as group>
<@spring.nestedPath path="groups[${group_index}]">
	<@spring.nestedPath path="events[${group.events?size}]">
		<@modal group_index group.events?size>
		Add event for ${group.name!""}
		</@modal>
	</@spring.nestedPath>
	<#list group.events as event>
		<@spring.nestedPath path="events[${event_index}]">
		<#if !((event.isEmpty())!false) || event_has_next>
			<@modal group_index event_index>
				Edit event for ${group.name!""}
			</@modal>
		</#if>
		</@spring.nestedPath>
	</#list>
</@spring.nestedPath>
</#list>

</#escape>