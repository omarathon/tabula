/* eslint-env browser */
import $ from 'jquery';

export default class CsrfForm {
  static getCsrfValue() {
    const $meta = $('meta[name=_csrf]');
    if ($meta.length > 0 && $meta.attr('content') !== undefined) return $meta.attr('content');
    return '';
  }

  static makeCsrfField = () => $('<input>').attr('type', 'hidden').attr('value', CsrfForm.getCsrfValue()).attr('name', 'urn:websignon:csrf');

  static appendCsrfField = $form => $form.append(CsrfForm.makeCsrfField());

  static generate = () => CsrfForm.appendCsrfField($('<form>').attr('method', 'POST'));
}
