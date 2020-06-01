/**
 * Please keep this up to date with
 * uk.ac.warwick.tabula.commands.marks.ComponentScalingCommand
 */
export default class ScalingAlgorithm {
  static doScaleMark(mark, passMark = 40, passMarkAdjustment = 0, upperClassAdjustment = 0) {
    if (mark < 0 || mark > 100) {
      throw new Error('Mark must be between 0 and 100');
    }
    const upperClassThreshold = 70;
    const passMarkRange = upperClassThreshold - passMark;

    let scaledMark;
    if (mark <= passMark - passMarkAdjustment) {
      scaledMark = (mark * passMark) / (passMark - passMarkAdjustment);
    } else if (mark >= upperClassThreshold - upperClassAdjustment) {
      scaledMark = ((mark * (100 - upperClassThreshold)) + 100 * upperClassAdjustment)
        / ((100 - upperClassThreshold) + upperClassAdjustment);
    } else {
      scaledMark = passMark + passMarkRange * (passMarkAdjustment + mark - passMark)
        / (passMarkRange - upperClassAdjustment + passMarkAdjustment);
    }
    return Math.round(scaledMark);
  }
}
