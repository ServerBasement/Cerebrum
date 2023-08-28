package it.ohalee.cerebrum.standalone.config;

import com.google.common.reflect.TypeToken;
import it.ohalee.basementlib.api.config.generic.adapter.ConfigurationAdapter;
import it.ohalee.basementlib.api.plugin.BasementPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CerebrumConfigurationNode extends SimpleConfigurationNode implements ConfigurationAdapter {

    private final Map<String, Object> self = new LinkedHashMap<>();

    public static CerebrumConfigurationNode of(ConfigurationNode node) {
        if (!(node instanceof SimpleConfigurationNode)) {
            throw new IllegalArgumentException("Cannot create a CerebrumConfigurationNode from a node that isn't a SimpleConfigurationNode");
        }
        return of((SimpleConfigurationNode) node);
    }

    public static CerebrumConfigurationNode of(SimpleConfigurationNode node) {
        return new CerebrumConfigurationNode(node.getKey(), node.getParent(), node.getOptions());
    }

    protected CerebrumConfigurationNode(@Nullable Object key, @Nullable SimpleConfigurationNode parent, @NonNull ConfigurationOptions options) {
        super(key, parent, options);

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : this.getChildrenMap().entrySet()) {
            String s = (entry.getKey() == null) ? "null" : entry.getKey().toString();
            self.put(s, entry.getValue());
        }
    }

    protected ConfigurationNode resolvePath(String path) {
        return this.getNode(Arrays.stream(path.split("\\.")).toArray());
    }

    @Override
    public BasementPlugin getPlugin() {
        return null;
    }

    @Override
    public void reload() {}

    public Set<String> getKeys() {
        return this.self.keySet();
    }

    @Override
    public String getString(String path, String s1) {
        return resolvePath(path).getString(s1);
    }

    @Override
    public int getInteger(String path, int i) {
        return resolvePath(path).getInt(i);
    }

    @Override
    public double getDouble(String path, double v) {
        return resolvePath(path).getDouble(v);
    }

    @Override
    public float getFloat(String path, float v) {
        return resolvePath(path).getFloat(v);
    }

    @Override
    public long getLong(String path, long l) {
        return resolvePath(path).getLong(l);
    }

    @Override
    public boolean getBoolean(String path, boolean b) {
        return resolvePath(path).getBoolean(b);
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        ConfigurationNode node = this.resolvePath(path);
        return !node.isVirtual() && node.isList() ? node.getList(Object::toString) : def;
    }

    @Override
    public CerebrumConfigurationNode section(String path) {
        return CerebrumConfigurationNode.of(resolvePath(path));
    }

    @Override
    public Map<String, String> getStringMap(String path, Map<String, String> def) {
        ConfigurationNode node = resolvePath(path);
        if (node.isVirtual()) {
            return def;
        }

        Map<String, Object> m = (Map<String, Object>) node.getValue(Collections.emptyMap());
        return m.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }

    @Override
    public Object get(String path, Object def) {
        ConfigurationNode node = resolvePath(path);
        return node.getValue(def);
    }

    @Override
    public Object set(String path, Object obj) {
        ConfigurationNode node = resolvePath(path);
        node.setValue(obj);
        return obj;
    }

}
