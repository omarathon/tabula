<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments for marking</h1>

<@components.marker_assignment_list id="marker-todo" title="To do" assignments=markerInformation.unmarkedAssignments />
<@components.marker_assignment_list id="marker-doing" title="Doing" assignments=markerInformation.inProgressAssignments />
<@components.marker_assignment_list id="marker-done" title="Done" assignments=markerInformation.pastAssignments />

</#escape>