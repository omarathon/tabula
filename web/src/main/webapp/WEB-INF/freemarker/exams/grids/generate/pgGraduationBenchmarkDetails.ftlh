<div class="exam-grid-preview">
  <div class="deptheader">
    <h1>PG Graduation Benchmark Breakdown</h1>
  </div>

  <section>
    <p>
      <#if department.examGridOptions.nameToShow.toString != 'none'>
          ${member.fullName!} (${member.universityId})
      <#else>
          Student ${member.universityId}
      </#if>
      has taken the following modules totalling ${breakdown.totalCatsTaken} credits,
      with credit weightings and marks shown and listed in order based on the highest mark:
    </p>

    <table class="table table-condensed grid">
      <thead>
      <tr class="assessment_componentdetails_header">
        <th>Module</th>
        <th>Module Credit</th>
        <th>Module Mark</th>
      </tr>
      </thead>
      <tbody>
      <#list breakdown.modules as moduleReg>
       <tr>
        <td class="assessment_details_col assessment_details_col2">
          <div>${(moduleReg.module.code)!""?upper_case}</div>
          <div>${moduleReg.module.name}</div>
        </td>
         <td class="assessment_details_col assessment_details_col2">
          ${moduleReg.cats}
         </td>
         <td class="assessment_details_col assessment_details_col2">
          ${(moduleReg.firstDefinedMark)!"X"}
         </td>
      </tr>
      </#list>
      </tbody>
    </table>
  </section>

  <section>
    <p>The cumulative value of the credits is calculated in order starting with highest credit value,
      until a minimum of ${breakdown.totalCats} credits is reached; in this case, the sum of the credits is ${breakdown.catsConsidered}:</p>
    <table class="table table-condensed grid no-wide-tables">
      <thead>
        <tr class="assessment_componentdetails_header">
          <th>Module</th>
          <th>Module Credit</th>
          <th>Module Mark</th>
          <th>Cumulative sum of credits</th>
        </tr>
      </thead>
      <tbody>
        <#list breakdown.usedModulesWithCumulativeSums as moduleRegSumsPair>
          <#assign moduleReg = moduleRegSumsPair._1() />
          <#assign cumulativeSums = moduleRegSumsPair._2() />
          <tr>
            <td class="assessment_details_col assessment_details_col2">
              <div>${(moduleReg.module.code)!""?upper_case}</div>
              <div>${moduleReg.module.name}</div>
            </td>
            <td class="assessment_details_col assessment_details_col2">${moduleReg.cats}</td>
            <td class="assessment_details_col assessment_details_col2">${(moduleReg.firstDefinedMark)!"X"}</td>
            <td>${cumulativeSums.cumulativeCats}</td>
          </tr>
        </#list>
        <#list breakdown.unusedModules as modulereg>
          <tr>
            <td class="assessment_details_col assessment_details_col2">
              <div>${(modulereg.module.code)!""?upper_case}</div>
              <div>${modulereg.module.name}</div>
            </td>
            <td class="assessment_details_col assessment_details_col2">${modulereg.cats}</td>
            <td class="assessment_details_col assessment_details_col2">${(modulereg.firstDefinedMark)!"X"}</td>
            <td></td>
            </tr>
        </#list>
    </table>
  </section>

  <section>
    <p>The weighted average can then be calculated based on ${breakdown.catsConsidered} credits in this case:</p>
    <table class="table table-condensed grid no-wide-tables">
      <thead>
      <tr class="assessment_componentdetails_header">
        <th>Module</th>
        <th>Module Credit</th>
        <th>Module Mark</th>
        <th>Credits*Mark</th>
        <th>Cumulative sum of Credits*Mark</th>
        <th>Cumulative sum of credits</th>
        <th>Weighted Average</th>
      </tr>
      </thead>
      <tbody>
      <#list breakdown.usedModulesWithCumulativeSums as moduleRegSumsPair>
          <#assign moduleReg = moduleRegSumsPair._1() />
          <#assign cumulativeSums = moduleRegSumsPair._2() />
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <div>${(moduleReg.module.code)!""?upper_case}</div>
            <div>${moduleReg.module.name}</div>
          </td>
          <td class="assessment_details_col assessment_details_col2">${moduleReg.cats}</td>
          <td class="assessment_details_col assessment_details_col2">${(moduleReg.firstDefinedMark)!"X"}</td>
          <td class="assessment_details_col assessment_details_col2">${(moduleReg.firstDefinedMark*moduleReg.cats)}</td>
          <td class="assessment_details_col assessment_details_col2">${cumulativeSums.cumulativeMarks}</td>
          <td>${cumulativeSums.cumulativeCats}</td>
          <td><#if moduleRegSumsPair?is_last>${breakdown.graduationBenchmark}*</#if></td>
        </tr>
      </#list>
      <#list breakdown.unusedModules as moduleReg>
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <div>${(moduleReg.module.code)!""?upper_case}</div>
            <div>${moduleReg.module.name}</div>
          </td>
          <td class="assessment_details_col assessment_details_col2">${moduleReg.cats}</td>
          <td class="assessment_details_col assessment_details_col2">${(moduleReg.firstDefinedMark)!"X"}</td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
      </#list>
    </table>
  </section>
  <h2>*Weighted average</h2>
    ${cumulativeSums.cumulativeMarks}/${breakdown.catsConsidered}=${cumulativeSums.cumulativeMarks / breakdown.catsConsidered}
</div>
