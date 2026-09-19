package awa.uxu.fastbuilder.file;

import awa.uxu.fastbuilder.FastBuilderPro;
import awa.uxu.fastbuilder.build.StructureCache;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StructureFileManager {

    private final Map<String, StructureCache> loaded = new HashMap<>();
    private final Map<String, String> authors = new HashMap<>();

    public void loadAll() {
        loaded.clear();
        authors.clear();

        File folder = new File(FastBuilderPro.get().getDataFolder(), "str");

        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        loadAuthors(folder);

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            StructureCache cache = WorldEditStructureLoader.load(file, 0);
            if (cache == null) {
                continue;
            }

            String clean = getBaseName(file.getName());
            String key = clean.toLowerCase();
            loaded.put(key, cache);
            authors.putIfAbsent(key, "未知");

            FastBuilderPro.get().getLogger().info("已加载结构: " + clean + " (" + file.getName() + ")");
        }
    }

    public Set<String> getNames() {
        return loaded.keySet();
    }

    public StructureCache get(String name) {
        return loaded.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return loaded.containsKey(name.toLowerCase());
    }


    public String getAuthor(String name) {
        if (name == null) {
            return "未知";
        }
        return authors.getOrDefault(name.toLowerCase(), "未知");
    }

    public void setAuthor(String name, String author) {
        if (name == null || name.isBlank()) {
            return;
        }

        String key = name.toLowerCase();
        String value = (author == null || author.isBlank()) ? "未知" : author;
        authors.put(key, value);

        File folder = new File(FastBuilderPro.get().getDataFolder(), "str");
        saveAuthors(folder);
    }

    private void loadAuthors(File folder) {
        File meta = new File(folder, "authors.yml");
        if (!meta.exists()) {
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(meta);
        for (String key : yml.getKeys(false)) {
            String author = yml.getString(key, "未知");
            authors.put(key.toLowerCase(), author == null || author.isBlank() ? "未知" : author);
        }
    }

    private void saveAuthors(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File meta = new File(folder, "authors.yml");
        YamlConfiguration yml = new YamlConfiguration();

        for (Map.Entry<String, String> entry : authors.entrySet()) {
            yml.set(entry.getKey(), entry.getValue());
        }

        try {
            yml.save(meta);
        } catch (IOException e) {
            FastBuilderPro.get().getLogger().warning("保存作者信息失败: " + e.getMessage());
        }
    }

    private String getBaseName(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0) {
            return fileName;
        }
        return fileName.substring(0, idx);
    }
}
