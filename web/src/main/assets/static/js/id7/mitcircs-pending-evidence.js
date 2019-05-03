import $ from 'jquery';

class MitCircsPendingEvidence {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const { form } = this;
    const $form = $(form);

    // Contacted fields
    $form
      .find('input[name="morePending"]')
      .on('input change', () => {
        const val = $form.find('input[name="morePending"]:checked').val();
        if (val) {
          $form.find('.mitcircs-form__fields__morepending-subfield--yes').toggle(val === 'true');
          $form.find('.mitcircs-form__fields__morepending-subfield--no').toggle(val !== 'true');
        }
      })
      .trigger('change');
  }
}

function init() {
  $('#pendingEvidenceForm').each((i, el) => {
    $(el).data('tabula.pendingEvidenceForm', new MitCircsPendingEvidence(el));
  });
}

$(() => init());
