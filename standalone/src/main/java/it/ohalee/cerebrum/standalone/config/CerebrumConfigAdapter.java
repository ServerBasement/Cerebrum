package it.ohalee.cerebrum.standalone.config;

import it.ohalee.basementlib.api.config.generic.adapter.ConfigurateConfigAdapter;
import it.ohalee.basementlib.api.config.generic.adapter.ConfigurationAdapter;
import it.ohalee.basementlib.api.plugin.BasementPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CerebrumConfigAdapter extends ConfigurateConfigAdapter implements ConfigurationAdapter {

    private final Map<String, Object> self = new LinkedHashMap<>();

    public CerebrumConfigAdapter(BasementPlugin plugin, Path path) {
        super(plugin, path);

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : this.root.getChildrenMap().entrySet()) {
            String key = (entry.getKey() == null) ? "null" : entry.getKey().toString();
            self.put(key, entry.getValue());
        }
    }

    @Override
    protected ConfigurationLoader<? extends ConfigurationNode> createLoader(Path path) {
        return YAMLConfigurationLoader.builder().setPath(path).build();
    }

    public Set<String> getKeys() {
        return this.self.keySet();
    }

    @Override
    public CerebrumConfigurationNode section(String path) {
        return CerebrumConfigurationNode.of(super.resolvePath(path));
    }

}