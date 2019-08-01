/* eslint-env browser */
/* global jQuery */

require('./scripts');

jQuery(($) => {
  const $showReportBtn = $('#show-report');
  const $reportResult = $('#report-result');

  $showReportBtn.on('click', () => {
    const params = '?format=html&' + $('#report-form').serialize();

    $.ajax({
      url: params,
      success: (response) => {
        $showReportBtn.hide();
        $reportResult.html(response).find('.table-sortable').sortableTable();
      }
    });
  });
});

