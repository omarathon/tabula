You need to sign up for the following small teaching groups:

<#list groupsets as groupSet>
${groupSet.name} ${groupSet.format.description} for ${groupSet.module.code?upper_case} - ${groupSet.module.name}
</#list>

Please visit <@url page=profileUrl context="/" /> to sign up for these groups.
