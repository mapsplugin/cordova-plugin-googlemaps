exports.defineAutoTests = function () {

  describe('install check', function () {

    it('plugin.google.maps should exist', function () {
        expect(plugin.google.maps).toBeDefined();
    });

  });

};
