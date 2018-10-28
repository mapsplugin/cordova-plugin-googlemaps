// jshint esversion: 6
const BaseArrayClass = require('../BaseArrayClass');

describe('[BaseArrayClass]', () => {

  it('getLength() should return the same size with initial array.', () => {
    const initArray = [0, 1, 2];
    const _ = new BaseArrayClass(initArray);
    expect(_.getLength()).toBe(initArray.length);
  });

});
