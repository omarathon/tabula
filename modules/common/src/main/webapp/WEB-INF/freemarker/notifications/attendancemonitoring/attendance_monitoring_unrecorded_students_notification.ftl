${users?size} <#if users?size == 1>student<#else>students</#if> in ${department.name} need attendance recording:

<#list truncatedUsers as user>
${user.fullName}
</#list>
<#if truncatedUsers?size < users?size>...and ${users?size - truncatedUsers?size} more students.</#if>