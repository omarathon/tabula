Adjustments have been made to your ${whatAdjusted} for the assignment '${assignment.name}' for ${assignment.module.code?upper_case} ${assignment.module.name}.

<#if feedback.adjustedMark??>- Adjusted mark: ${feedback.adjustedMark}</#if>
<#if feedback.adjustedGrade??>- Adjusted grade: ${feedback.adjustedGrade}</#if>
<#if feedback.adjustmentReason??>- Reason for adjustment: ${feedback.adjustmentReason}</#if>

${feedback.adjustmentComments!""}