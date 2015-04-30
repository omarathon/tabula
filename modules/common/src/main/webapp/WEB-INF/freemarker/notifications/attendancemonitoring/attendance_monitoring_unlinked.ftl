There are <@fmt.p schemes?size "monitoring scheme" /> in ${department.name} that are set to automatically update their membership based on changes in SITS.

Soon SITS will be updated for the next academic year (${academicYear.next.toString}) and this means that the students in each of these schemes could change in ways that you did not intend.

For example, if a scheme is set to only include students in their first year of study, soon this scheme will become empty as all the current first year students have their enrolment records updated (and become second year students).

In order to prevent these unwanted changes all of these schemes have now had the link to SITS disabled, and the students on the scheme has been fixed to what it is currently.

You can use the links below to view the students on each scheme and make changes where necessary:

<#list schemeLinks as schemeLink>* ${schemeLink}
</#list>