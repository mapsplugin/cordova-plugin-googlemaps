// jshint esversion: 6
const BaseArrayClass = require('../BaseArrayClass');

describe('[BaseArrayClass]', () => {

  // describe('getLength()', () => {
  //   it('() should return the same size with initial array', () => {
  //     const initArray = [0, 1, 2];
  //     const _ = new BaseArrayClass(initArray);
  //     expect(_.getLength()).toBe(initArray.length);
  //   });
  // });


  describe('mapSeries()', () => {
    it('should error if no callback is given', () => {
      const initArray = ['A', 'B', 'C'];
      const _ = new BaseArrayClass(initArray);
      expect(_.mapSeries(function() {})).rejects.toMatchSnapshot();
    });
    //
    // it('should keep item order', (done) => {
    //   const initArray = ['A', 'B', 'C'];
    //   const _ = new BaseArrayClass(initArray);
    //   let answer = '';
    //   _.mapSeries(function(item, next) {
    //     next(item);
    //   }, function(results) {
    //     console.log(results);
    //     expect(results).toEqual(initArray);
    //     done();
    //   });
    // });
  });

});
