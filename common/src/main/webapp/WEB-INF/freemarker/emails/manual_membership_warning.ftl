There are manually-added students in<#if numAssignments gt 0> <@fmt.p numAssignments "assignment"/></#if><#if numAssignments gt 0 && numSmallGroupSets gt 0> and</#if><#if numSmallGroupSets gt 0> <@fmt.p numSmallGroupSets "small group set"/></#if> in ${department.name}:

It's important that SITS holds up-to-date information on which students take each module.

Please take the following action:

1. Update the membership of the<#if numAssignments gt 0> <@fmt.p number=numAssignments singular="assignment" shownumber=false/></#if><#if numAssignments gt 0 && numSmallGroupSets gt 0> and</#if><#if numSmallGroupSets gt 0> <@fmt.p number=numSmallGroupSets singular="small group set" shownumber=false/></#if> in SITS.
2. Once the membership in SITS is accurate, remove any manually-added students from the<#if numAssignments gt 0> <@fmt.p number=numAssignments singular="assignment" shownumber=false/></#if><#if numAssignments gt 0 && numSmallGroupSets gt 0> and</#if><#if numSmallGroupSets gt 0> <@fmt.p number=numSmallGroupSets singular="small group set" shownumber=false/></#if> in Tabula.
