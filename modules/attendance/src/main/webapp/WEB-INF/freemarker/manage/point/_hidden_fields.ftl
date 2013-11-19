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
	<input type="hidden" name="monitoringPoints[${thisPointIndex}].meetingFormatsSpring" value="${format.description}" />
</#list>
<input type="hidden" name="monitoringPoints[${thisPointIndex}].meetingQuantity" value="${point.meetingQuantity}" />