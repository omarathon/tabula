<div class="exam-grid-preview">
  <div class="deptheader">
    <h1>Graduation benchmark breakdown</h1>
    <#if department.examGridOptions.nameToShow.toString != 'none'>
      <h4 class="with-related">for ${member.fullName!}, ${member.universityId}</h4>
    <#else>
      <h4 class="with-related">for ${member.universityId}</h4>
    </#if>
  </div>

  <section>
    <div class="alert alert-info ">
      <p>The weighted average has been calculated following the
      <a href="https://warwick.ac.uk/insite/coronavirus/staff/teaching/policyguidance/pgt/" target = "_blank">
        Teaching Continuity Policy Guidance for Postgraduate Taught students</a>, which includes
      <a href="https://warwick.ac.uk/insite/coronavirus/staff/teaching/policyguidance/pgt/examples/" target="_blank">
        specific examples of calculating the best credits for those students</a>.
      </p>
    </div>
  </section>

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
          <div>${((moduleReg.module.code)!"")?upper_case}</div>
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
      until a minimum of ${breakdown.minCatsToConsider} credits is reached; in this case, the sum of the credits is ${breakdown.catsConsidered}:</p>
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
        <#list breakdown.usedModulesWithCumulativeSums as cumulative>

          <tr>
            <td class="assessment_details_col assessment_details_col2">
              <div>${((cumulative.moduleRegistration.module.code)!"")?upper_case}</div>
              <div>${cumulative.moduleRegistration.module.name}</div>
            </td>
            <td class="assessment_details_col assessment_details_col2">${cumulative.moduleRegistration.cats}</td>
            <td class="assessment_details_col assessment_details_col2">${(cumulative.moduleRegistration.firstDefinedMark)!"X"}</td>
            <td>
              <div class="benchmark-breakdown-calculation">
                <div>
                  <#if cumulative_index gt 0>
                    ${breakdown.usedModulesWithCumulativeSums[cumulative_index - 1].cats}
                    +
                    ${(cumulative.moduleRegistration.cats)}
                    =
                  </#if>
                  ${cumulative.cats}
                </div>
              </div>
            </td>
          </tr>
        </#list>
        <#list breakdown.unusedModules as modulereg>
          <tr>
            <td class="assessment_details_col assessment_details_col2">
              <div>${((modulereg.module.code)!"")?upper_case}</div>
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
        <th>Credits × Mark</th>
        <th>Cumulative sum of Credits × Mark</th>
        <th>Cumulative sum of credits</th>
        <th>Weighted Average</th>
      </tr>
      </thead>
      <tbody>
      <#list breakdown.usedModulesWithCumulativeSums as cumulative>
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <div>${((cumulative.moduleRegistration.module.code)!"")?upper_case}</div>
            <div>${cumulative.moduleRegistration.module.name}</div>
          </td>
          <td class="assessment_details_col assessment_details_col2">${cumulative.moduleRegistration.cats}</td>
          <td class="assessment_details_col assessment_details_col2">${(cumulative.moduleRegistration.firstDefinedMark)!"X"}</td>
          <td class="assessment_details_col assessment_details_col2">
            <div class="benchmark-breakdown-calculation">
              <div>
                ${cumulative.moduleRegistration.firstDefinedMark}
                ×
                ${cumulative.moduleRegistration.cats}
                =
                ${(cumulative.moduleRegistration.firstDefinedMark*cumulative.moduleRegistration.cats)}
              </div>
            </div>
          </td>
          <td class="assessment_details_col assessment_details_col2">
            <div class="benchmark-breakdown-calculation">
              <div>
                <#if cumulative_index gt 0>
                  ${breakdown.usedModulesWithCumulativeSums[cumulative_index - 1].marks}
                  +
                  ${(cumulative.moduleRegistration.firstDefinedMark*cumulative.moduleRegistration.cats)}
                  =
                </#if>
                ${cumulative.marks}
              </div>
            </div>
          </td>
          <td>
            <div class="benchmark-breakdown-calculation">
              <div>
                <#if cumulative_index gt 0>
                  ${breakdown.usedModulesWithCumulativeSums[cumulative_index - 1].cats}
                  +
                  ${(cumulative.moduleRegistration.cats)}
                  =
                </#if>
                ${cumulative.cats}
              </div>
            </div>
          </td>
          <td>
            <#if cumulative?is_last>
              <div class="benchmark-breakdown-calculation">
                <#if breakdown.usedModulesWithCumulativeSums?has_content>
                  <div class="formula">
                    <div class="numerator">${breakdown.usedModulesWithCumulativeSums?last.marks}</div>
                    <div class="denominator">${breakdown.catsConsidered}</div>
                  </div>
                  <div>=</div>
                </#if>
                <div class="result">
                  ${breakdown.graduationBenchmark}%
                </div>
              </div>
            </#if>
          </td>
        </tr>
      </#list>
      <#list breakdown.unusedModules as moduleReg>
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <div>${((moduleReg.module.code)!"")?upper_case}</div>
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
</div>
