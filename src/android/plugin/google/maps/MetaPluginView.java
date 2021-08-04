package plugin.google.maps;

import android.view.ViewGroup;

public class MetaPluginView {
    int viewDepth;
    private String pluginId;
    String divId;
    boolean isVisible;
    boolean isClickable;

    public MetaPluginView(String pluginId) {
        this.pluginId = pluginId;
    }
    public String getPluginId() {
        return this.pluginId;
    }
}
