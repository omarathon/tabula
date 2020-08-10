The following ${formatsString} are open for sign up:

<#list groupsets as groupSet>
- ${groupSet.module.code?upper_case} ${groupSet.nameWithoutModulePrefix}
</#list>
