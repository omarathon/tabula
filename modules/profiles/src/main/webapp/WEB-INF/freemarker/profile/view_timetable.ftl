<#import "profile_macros.ftl" as profile_macros />
<#escape x as x?html>
<div id="timetable-pane">
	<h4>
		Timetable
		<#if profile.timetableHash?has_content && can.do("Profiles.Read.TimetablePrivateFeed", profile)>
			<a href="<@routes.timetable_ical profile />" title="Subscribe to timetable"><i class="icon-calendar"></i></a>
			<span class="subscribe">
				<a href="<@routes.timetable_ical profile />" title="Subscribe to timetable">
					Subscribe
				</a>
			</span>
		</#if>
	</h4>

	<script type="text/javascript">
		var weeks = ${weekRangesDumper()}
	</script>

	<@profile_macros.timetable_placeholder profile "month" true renderDate />

	<#if profile.timetableHash?has_content>
		<@profile_macros.timetable_ical_modal profile />
	</#if>
</div>
</#escape>