<#import "meeting_list_macros.ftl" as meeting_macros />
<@meeting_macros.list studentCourseDetails meetings role/>

<script>
	if (typeof Profiles !== "undefined") { // Only call once loaded into the main page
		Profiles.SetupMeetingRecords();
	}
</script>