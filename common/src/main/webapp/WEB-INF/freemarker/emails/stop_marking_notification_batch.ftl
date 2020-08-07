Marking has been stopped for the following assignments:

<#list assignments as entry>
<#assign assignment = entry._1() />
<#assign numStoppedFeedbacks = entry._2() />
- ${assignment.module.code?upper_case} - ${assignment.name} (<@fmt.p number=numStoppedFeedbacks singular="submission" />)
</#list>

You won't be able to add marks or feedback for <@fmt.p number=totalNumStoppedFeedbacks singular="this submission" plural="these submissions" shownumber=false /> until the admin releases them for marking. Any feedback you have entered so far has been saved.
