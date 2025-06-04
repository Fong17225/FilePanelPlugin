package firstplugin.filepanelplugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class FilePanelPlugin extends JavaPlugin {
    private String key;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        key = getConfig().getString("key");
        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString();
            getConfig().set("key", key);
            saveConfig();
        }

        getLogger().info("Web access key: " + key);

        // Lấy thư mục gốc của server (thường là nơi chứa server.jar)
        File rootDir = null;
        try {
            rootDir = getServer().getWorldContainer();
            if (rootDir == null) {
                getLogger().warning("getWorldContainer() returned null, fallback to parent of plugin folder");
                rootDir = getDataFolder().getParentFile().getParentFile();
            }
            if (rootDir == null) {
                getLogger().warning("Fallback rootDir still null, using plugin data folder");
                rootDir = getDataFolder();
            }
        } catch (Exception e) {
            getLogger().warning("Error getting rootDir: " + e.getMessage());
            rootDir = getDataFolder();
        }

        getLogger().info("Using root directory: " + (rootDir == null ? "null" : rootDir.getAbsolutePath()));

        // Khởi động HTTP server với thư mục gốc
        SimpleHttpServer.start(25567, key, rootDir);
    }

    @Override
    public void onDisable() {
        SimpleHttpServer.stop();
    }
}
