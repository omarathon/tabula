<#if !pointCount?? >
	<#assign pointCount = point_index />
</#if>
<input type="hidden" name="monitoringPoints[${pointCount}].name" value="${point.name}" />
<input type="hidden" name="monitoringPoints[${pointCount}].validFromWeek" value="${point.validFromWeek}" />
<input type="hidden" name="monitoringPoints[${pointCount}].requiredFromWeek" value="${point.requiredFromWeek}" />
<input type="hidden" name="monitoringPoints[${pointCount}].pointType" value="${point.pointType.dbValue}" />
<#list point.meetingRelationships as relationship>
	<input type="hidden" name="monitoringPoints[${pointCount}].meetingRelationshipsSpring" value="${relationship.urlPart}" />
</#list>
<#list point.meetingFormats as format>
	<input type="hidden" name="monitoringPoints[${pointCount}].meetingFormatsSpring" value="${format.description}" />
</#list>
<input type="hidden" name="monitoringPoints[${pointCount}].meetingQuantity" value="${point.meetingQuantity}" />