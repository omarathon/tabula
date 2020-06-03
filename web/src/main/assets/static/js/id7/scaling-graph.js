/* eslint-env browser */
import $ from 'jquery';
import log from 'loglevel';
import Chart from 'chart.js';
import ScalingAlgorithm from './scaling-algorithm';

class ScalingError extends Error {
  constructor(message, cause) {
    super(message);
    this.cause = cause;
    this.name = 'ScalingError';
  }
}

$(() => {
  function validateScalingParams(passMark, scaledPassMark, scaledUpperClassMark) {
    const passMarkAdjustment = passMark - scaledPassMark;
    const upperClassThreshold = 70;
    const upperClassAdjustment = upperClassThreshold - scaledUpperClassMark;

    if (passMark < 0 || passMark > 100) {
      throw new ScalingError('Pass mark out of range');
    }

    if (scaledUpperClassMark <= scaledPassMark) {
      throw new ScalingError('Scaled pass mark is less than scaled upper class mark');
    }

    if (scaledPassMark < 0 || scaledPassMark > 100) {
      throw new ScalingError('Scaled pass mark out of range');
    }

    if (scaledUpperClassMark < 0 || scaledUpperClassMark > 100) {
      throw new ScalingError('Scaled upper class mark out of range');
    }

    // Don't use exceptions for flow control, they said, but it's so damn convenient
    if (passMarkAdjustment >= passMark) {
      throw new ScalingError('Pass mark adjustment is larger than the pass mark');
    } else if (passMarkAdjustment <= passMark - 100) {
      throw new ScalingError('Pass mark adjustment is less than pass mark - 100');
    }

    if (upperClassAdjustment <= upperClassThreshold - 100) {
      throw new ScalingError('Upper class adjustment is less than -30');
    }

    if (Math.abs(upperClassAdjustment - passMarkAdjustment) >= upperClassThreshold) {
      throw new ScalingError('Difference between marks is larger than 70');
    }
  }

  function computeNewMarks(marks) {
    const passMark = parseInt($('#passMark').val(), 10);
    const scaledPassMark = parseInt($('#scaledPassMark').val(), 10);
    const scaledUpperClassMark = parseInt($('#scaledUpperClassMark').val(), 10);

    validateScalingParams(passMark, scaledPassMark, scaledUpperClassMark);

    const passMarkAdjustment = passMark - scaledPassMark;
    const upperClassAdjustment = 70 - scaledUpperClassMark;
    return marks.map(m => ({
      x: m,
      y: ScalingAlgorithm.doScaleMark(m, passMark, passMarkAdjustment, upperClassAdjustment),
    }));
  }

  function scaleLine() {
    const passMark = parseInt($('#passMark').val(), 10);
    const scaledPassMark = parseInt($('#scaledPassMark').val(), 10);
    const scaledUpperClassMark = parseInt($('#scaledUpperClassMark').val(), 10);

    validateScalingParams(passMark, scaledPassMark, scaledUpperClassMark);

    const passMarkAdjustment = passMark - scaledPassMark;
    const upperClassAdjustment = 70 - scaledUpperClassMark;
    return [0, (passMark - passMarkAdjustment), (70 - upperClassAdjustment), 100].map(m => ({
      x: m,
      y: ScalingAlgorithm.doScaleMark(m, passMark, passMarkAdjustment, upperClassAdjustment),
    }));
  }

  $('#marks').each((i, el) => {
    const markData = JSON.parse($('<textarea/>').html(el.innerHTML).text());
    const marks = Object.values(markData);
    Chart.platform.disableCSSInjection = true;

    const chart = new Chart(document.getElementById('chart').getContext('2d'), {
      type: 'scatter',
      data: {
        labels: Object.keys(markData),
        datasets: [{
          data: computeNewMarks(marks),
          pointBackgroundColor: '#3e7a73',
          fill: false,
          tension: 0,
        }, {
          fill: false,
          tension: 0,
          pointRadius: 0,
          pointHoverRadius: 0,
          borderWidth: 0,
          showLine: true,
          data: [{
            x: 0,
            y: 0,
          }, {
            x: 100,
            y: 100,
          }],
        }],
      },
      options: {
        aspectRatio: 1.5,
        legend: false,
        tooltips: {
          filter(tooltipItem) {
            return tooltipItem.datasetIndex === 0;
          },
          callbacks: {
            label(tooltipItem) {
              const label = Object.keys(markData)[tooltipItem.index];
              if (tooltipItem.xLabel === tooltipItem.yLabel) {
                return `${tooltipItem.xLabel} is unchanged (${label})`;
              }
              return `${tooltipItem.xLabel} scales to ${tooltipItem.yLabel} (${label})`;
            },
          },
        },
        scales: {
          xAxes: [{
            ticks: {
              min: 0,
              max: 100,
            },
            scaleLabel: {
              display: true,
              labelString: 'Pre-scaled mark',
            },
            gridLines: {
              color: '#eee',
              drawOnChartArea: true,
            },
          }],
          yAxes: [{
            ticks: {
              min: 0,
              max: 100,
              padding: 10,
            },
            scaleLabel: {
              display: true,
              labelString: 'Scaled mark',
            },
            gridLines: {
              color: '#eee',
              drawOnChartArea: true,
            },
          }],
        },
      },
    });

    $('.form-control').on('change', () => {
      try {
        chart.data.datasets[0].data = computeNewMarks(marks);
        chart.data.datasets[1].data = scaleLine();
        chart.update();
        document.getElementById('chart').style.opacity = 1.0;
      } catch (e) {
        log.error('Invalid scaling parameters', e);
        document.getElementById('chart').style.opacity = 0.4;
      }
    });
  });
});
