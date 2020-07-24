import ScalingAlgorithm from "id7/scaling-algorithm";

describe('ScalingAlgorithm', () => {

  it('should scale in line with ComponentScalingCommandTest#defaults', () => {
    const passMarkAdjustment = 5;
    const upperClassAdjustment = 5;
    const passMark = 40;

    assert.equal(ScalingAlgorithm.doScaleMark(0, passMark, passMarkAdjustment, upperClassAdjustment), 0);
    assert.equal(ScalingAlgorithm.doScaleMark(5, passMark, passMarkAdjustment, upperClassAdjustment), 6);
    assert.equal(ScalingAlgorithm.doScaleMark(10, passMark, passMarkAdjustment, upperClassAdjustment), 11);
    assert.equal(ScalingAlgorithm.doScaleMark(15, passMark, passMarkAdjustment, upperClassAdjustment), 17);
    assert.equal(ScalingAlgorithm.doScaleMark(20, passMark, passMarkAdjustment, upperClassAdjustment), 23);
    assert.equal(ScalingAlgorithm.doScaleMark(25, passMark, passMarkAdjustment, upperClassAdjustment), 29);
    assert.equal(ScalingAlgorithm.doScaleMark(30, passMark, passMarkAdjustment, upperClassAdjustment), 34);
    assert.equal(ScalingAlgorithm.doScaleMark(35, passMark, passMarkAdjustment, upperClassAdjustment), 40);
    assert.equal(ScalingAlgorithm.doScaleMark(40, passMark, passMarkAdjustment, upperClassAdjustment), 45);
    assert.equal(ScalingAlgorithm.doScaleMark(45, passMark, passMarkAdjustment, upperClassAdjustment), 50);
    assert.equal(ScalingAlgorithm.doScaleMark(50, passMark, passMarkAdjustment, upperClassAdjustment), 55);
    assert.equal(ScalingAlgorithm.doScaleMark(55, passMark, passMarkAdjustment, upperClassAdjustment), 60);
    assert.equal(ScalingAlgorithm.doScaleMark(60, passMark, passMarkAdjustment, upperClassAdjustment), 65);
    assert.equal(ScalingAlgorithm.doScaleMark(65, passMark, passMarkAdjustment, upperClassAdjustment), 70);
    assert.equal(ScalingAlgorithm.doScaleMark(70, passMark, passMarkAdjustment, upperClassAdjustment), 74);
    assert.equal(ScalingAlgorithm.doScaleMark(75, passMark, passMarkAdjustment, upperClassAdjustment), 79);
    assert.equal(ScalingAlgorithm.doScaleMark(80, passMark, passMarkAdjustment, upperClassAdjustment), 83);
    assert.equal(ScalingAlgorithm.doScaleMark(85, passMark, passMarkAdjustment, upperClassAdjustment), 87);
    assert.equal(ScalingAlgorithm.doScaleMark(90, passMark, passMarkAdjustment, upperClassAdjustment), 91);
    assert.equal(ScalingAlgorithm.doScaleMark(95, passMark, passMarkAdjustment, upperClassAdjustment), 96);
    assert.equal(ScalingAlgorithm.doScaleMark(100, passMark, passMarkAdjustment, upperClassAdjustment), 100);
  });

  it('should scale in line with ComponentScalingCommandTest#options', () => {
    const passMarkAdjustment = 17;
    const upperClassAdjustment = 12;
    const passMark = 50;

    assert.equal(ScalingAlgorithm.doScaleMark(0, passMark, passMarkAdjustment, upperClassAdjustment), 0);
    assert.equal(ScalingAlgorithm.doScaleMark(5, passMark, passMarkAdjustment, upperClassAdjustment), 8);
    assert.equal(ScalingAlgorithm.doScaleMark(10, passMark, passMarkAdjustment, upperClassAdjustment), 15);
    assert.equal(ScalingAlgorithm.doScaleMark(15, passMark, passMarkAdjustment, upperClassAdjustment), 23);
    assert.equal(ScalingAlgorithm.doScaleMark(20, passMark, passMarkAdjustment, upperClassAdjustment), 30);
    assert.equal(ScalingAlgorithm.doScaleMark(25, passMark, passMarkAdjustment, upperClassAdjustment), 38);
    assert.equal(ScalingAlgorithm.doScaleMark(30, passMark, passMarkAdjustment, upperClassAdjustment), 45);
    assert.equal(ScalingAlgorithm.doScaleMark(35, passMark, passMarkAdjustment, upperClassAdjustment), 52);
    assert.equal(ScalingAlgorithm.doScaleMark(40, passMark, passMarkAdjustment, upperClassAdjustment), 56);
    assert.equal(ScalingAlgorithm.doScaleMark(45, passMark, passMarkAdjustment, upperClassAdjustment), 60);
    assert.equal(ScalingAlgorithm.doScaleMark(50, passMark, passMarkAdjustment, upperClassAdjustment), 64);
    assert.equal(ScalingAlgorithm.doScaleMark(55, passMark, passMarkAdjustment, upperClassAdjustment), 68);
    assert.equal(ScalingAlgorithm.doScaleMark(60, passMark, passMarkAdjustment, upperClassAdjustment), 71);
    assert.equal(ScalingAlgorithm.doScaleMark(65, passMark, passMarkAdjustment, upperClassAdjustment), 75);
    assert.equal(ScalingAlgorithm.doScaleMark(70, passMark, passMarkAdjustment, upperClassAdjustment), 79);
    assert.equal(ScalingAlgorithm.doScaleMark(75, passMark, passMarkAdjustment, upperClassAdjustment), 82);
    assert.equal(ScalingAlgorithm.doScaleMark(80, passMark, passMarkAdjustment, upperClassAdjustment), 86);
    assert.equal(ScalingAlgorithm.doScaleMark(85, passMark, passMarkAdjustment, upperClassAdjustment), 89);
    assert.equal(ScalingAlgorithm.doScaleMark(90, passMark, passMarkAdjustment, upperClassAdjustment), 93);
    assert.equal(ScalingAlgorithm.doScaleMark(95, passMark, passMarkAdjustment, upperClassAdjustment), 96);
    assert.equal(ScalingAlgorithm.doScaleMark(100, passMark, passMarkAdjustment, upperClassAdjustment), 100);
  });

})