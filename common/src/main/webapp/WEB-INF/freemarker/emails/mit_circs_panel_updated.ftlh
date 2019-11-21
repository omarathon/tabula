The mitigating circumstances panel has been updated.

<#if nameChanged>The panel is now called ${panel.name}.</#if><#--
--><#if dateChanged>

<#if panel.date??>The panel will now take place on <@fmt.date date=panel.date includeTime=false relative=false />: <@fmt.time panel.startTime /> &mdash; <@fmt.time panel.endTime />.<#else>The panel date is to be confirmed.</#if></#if><#--
--><#if locationChanged>

<#if panel.location??>The panel will now be held at ${panel.location.name}.<#else>The location of the panel is to be confirmed.</#if></#if><#--
--><#if submissionsAdded>

New mitigating circumstances submissions have been added to ${panel.name}.<#elseif submissionsRemoved>Mitigating circumstances submissions have been removed from ${panel.name}.</#if>