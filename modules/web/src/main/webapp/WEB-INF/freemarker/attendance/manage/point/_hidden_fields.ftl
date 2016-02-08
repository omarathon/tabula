<#if pointCount?? >
	<#assign thisPointIndex = pointCount />
<#else>
	<#assign thisPointIndex = point_index />
</#if>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].name" value="${point.name}" />
<input type="hidden" name="monitoringPoints[${thisPointIndex}].validFromWeek" value="${point.validFromWeek}" />
<input type="hidden" name="monitoringPoints[${thisPointIndex}].requiredFromWeek" value="${point.requiredFromWeek}" />
<input type="hidden" name="monitoringPoints[${thisPointIndex}].pointType" value="<#if point.pointType??>${point.pointType.dbValue}</#if>" />
<#list point.meetingRelationships as relationship>
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].meetingRelationshipsSpring" value="${relationship.urlPart}" />
</#list>
<#list point.meetingFormats as format>
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].meetingFormatsSpring" value="${format.code}" />
</#list>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].meetingQuantity" value="${point.meetingQuantity}" />
<#list point.smallGroupEventModules as module>
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].smallGroupEventModulesSpring" value="${module.id}" />
</#list>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].smallGroupEventQuantity" value="${point.smallGroupEventQuantity}" />
<#list point.assignmentSubmissionModules as module>
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].assignmentSubmissionModulesSpring" value="${module.id}" />
</#list>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].assignmentSubmissionQuantity" value="${point.assignmentSubmissionQuantity}" />
<#list point.assignmentSubmissionAssignments as assignment>
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].assignmentSubmissionAssignmentsSpring" value="${assignment.id}" />
</#list>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].assignmentSubmissionIsDisjunction" value="<#if point.assignmentSubmissionIsDisjunction>true<#else>false</#if>" />