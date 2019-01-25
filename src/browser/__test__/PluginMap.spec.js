const PluginMap = require('../PluginMap');

describe('PluginMap', () => {
  it('should exist', () => {
    expect(PluginMap).toBeDefined();
  });

  describe('toDataURL', () => {
    afterEach(() => {
      document.body.innerHTML = '';
    });

    it('should exist', () => {
      var mapId = 'map';
      document.body.innerHTML = '<div __pluginMapId="' + mapId + '"></div>';
      expect(new PluginMap(mapId, {}).toDataURL).toBeDefined();
    });
  });
});
