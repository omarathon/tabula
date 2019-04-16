/* eslint-env browser */
import $ from 'jquery';

class MitCircsSettingsForm {
  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    this.initGuidancePreview();
  }

  initGuidancePreview() {
    const { form } = this;
    const $form = $(form);

    $form
      .find(':input[name="mitCircsGuidance"][data-preview]')
      .on('input change', (e) => {
        const $textarea = $(e.target);
        const $preview = $form.find($textarea.data('preview'));

        $.post('/markdown/toHtml', { markdownString: $textarea.val() }, (res) => {
          $preview.html(res).toggle(res.trim().length > 0);
        });
      })
      .trigger('change');
  }
}

function init() {
  $('#displaySettingsCommand').each((i, el) => {
    $(el).data('tabula.mitigatingCircumstancesSettingsForm', new MitCircsSettingsForm(el));
  });
}

$(() => init());
