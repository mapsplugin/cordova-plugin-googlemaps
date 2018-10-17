exports.defineAutoTests = function () {

  describe('install check', function () {

    it('plugin.google.maps should exist', function () {
        expect(plugin.google.maps).toBeDefined();
    });

    // temporally fail test
    it('fail.google.maps should exist', function () {
        expect(fail.google.maps).toBeDefined();
    });

  });

};
