There <@fmt.p number=sets?size singular="is" plural="are" shownumber=false />
<@fmt.p sets?size "small group set" /> in ${department.name} which automatically
<@fmt.p number=sets?size singular="updates its" plural="update their" shownumber=false />
membership based on changes in SITS.

Soon SITS will be updated for the next academic year (${academicYear.next.toString}). This means that the students in each of these sets could change in ways that you did not intend.

For example, if a set is set to only include students in their first year of study, soon this set's membership will become empty - as all current first-year students have their enrolment records updated (and they become second-year students).

To prevent these unwanted changes, we have disabled the link to SITS for all of these sets. So, these sets will remain as they are – with their current sets of students – unless you manually make changes.

Follow the links below to view the students on each set and make changes where necessary:

<#list setLinks as setLink>* ${setLink}
</#list>