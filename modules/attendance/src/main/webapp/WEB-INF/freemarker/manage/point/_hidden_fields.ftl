<#if !point_index?? >
	<#assign point_index = pointCount />
</#if>
<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
<input type="hidden" name="monitoringPoints[${point_index}].validFromWeek" value="${point.validFromWeek}" />
<input type="hidden" name="monitoringPoints[${point_index}].requiredFromWeek" value="${point.requiredFromWeek}" />
<input type="hidden" name="monitoringPoints[${point_index}].pointType" value="<#if point.pointType??>${point.pointType.dbValue}</#if>" />
<#list point.meetingRelationships as relationship>
	<input type="hidden" name="monitoringPoints[${point_index}].meetingRelationshipsSpring" value="${relationship.urlPart}" />
</#list>
<#list point.meetingFormats as format>
	<input type="hidden" name="monitoringPoints[${point_index}].meetingFormatsSpring" value="${format.description}" />
</#list>
<input type="hidden" name="monitoringPoints[${point_index}].meetingQuantity" value="${point.meetingQuantity}" />