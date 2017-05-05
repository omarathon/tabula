<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments for marking</h1>

<@components.marker_assignment_list id="marker-upcoming" title="Upcoming" assignments=markerInformation.upcomingAssignments />
<@components.marker_assignment_list id="marker-action" title="Action required" assignments=markerInformation.actionRequiredAssignments />
<@components.marker_assignment_list id="marker-noaction" title="No action required" assignments=markerInformation.noActionRequiredAssignments />
<@components.marker_assignment_list id="marker-completed" title="Completed" assignments=markerInformation.completedAssignments />

</#escape>