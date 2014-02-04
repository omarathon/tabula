<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<h1>
		Attendance for ${set.name}
	</h1>
	
	<@components.single_groupset_attendance set groups />
	
	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade">
	</div>
</#escape>