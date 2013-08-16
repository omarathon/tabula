This is going to be the student timetable
<ul>
<#list timetable as event>
<li> ${event.name} ${event.description} ${event.eventType.code} <@fmt.date event.start /> ${event.location}
</li>
</#list>
</ul>