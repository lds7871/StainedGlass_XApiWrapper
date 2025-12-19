package LDS.Person.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置管理工具类 - 单例模式
 * 集中管理应用配置，避免重复加载config.properties文件
 * 使用读写锁保证线程安全的同时提高读取性能
 */
public class ConfigManager {
    
    private static volatile ConfigManager instance;
    private final Properties properties;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 私有构造函数，加载配置文件
     */
    private ConfigManager() {
        properties = new Properties();
        loadConfig();
    }
    
    /**
     * 获取ConfigManager单例实例（双重检查锁定）
     * @return ConfigManager实例
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                printConfigDetails();
               
            } else {
                System.out.println("[WARN] config.properties 未找到，使用默认配置");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 加载 config.properties 失败: " + e.getMessage());
        }
    }

    
    /**
     * 打印完整的配置信息
     */
    private void printConfigDetails() {
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("[ConfigManager] 配置文件加载成功：");
        System.out.println("════════════════════════════════════════════════════════════");
        
        if (properties.isEmpty()) {
            System.out.println("  （无配置项）");
        } else {
            properties.keySet().stream()
                    .map(Object::toString)
                    .sorted()
                    .forEach(key -> {
                        String value = properties.getProperty(key);
                        // 对敏感信息进行脱敏处理
                        String displayValue = maskSensitiveValue(key, value);
                        System.out.println("  " + key + " = " + displayValue);
                    });
        }
        
        System.out.println("════════════════════════════════════════════════════════════");
    }

    /**
     * 对敏感信息进行脱敏处理
     */
    private String maskSensitiveValue(String key, String value) {
        String lowerKey = key.toLowerCase();
        
        // 对密码、令牌、密钥等敏感字段进行脱敏
        if (lowerKey.contains("password") || lowerKey.contains("token") || 
            lowerKey.contains("secret") || lowerKey.contains("key") ||
            lowerKey.contains("auth")) {
            if (value != null && value.length() > 0) {
                return "***" + (value.length() > 3 ? value.substring(value.length() - 3) : "");
            }
            return "***";
        }
        
        return value;
    }
    
    
    /**
     * 获取字符串配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            return properties.getProperty(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取整数配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public int getInt(String key, int defaultValue) {
        lock.readLock().lock();
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            System.err.println("[WARN] 无效的整数配置 " + key + ": " + properties.getProperty(key));
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取布尔配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        lock.readLock().lock();
        try {
            String value = properties.getProperty(key);
            return value != null ? Boolean.parseBoolean(value) : defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 重新加载配置文件
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            properties.clear();
            loadConfig();
            System.out.println("[ConfigManager] 配置文件已重新加载");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // 常用配置的便捷方法
    
    public String getNapCatApiBase() {
        return getString("NapCatApiBase", "http://127.0.0.1:3000");
    }
    
    public String getNapCatAuthToken() {
        return getString("NapCatAuthToken", "");
    }
    
    public String getNapcatQQID() {
        return getString("NapcatQQID", "");
    }
    
    public String getWsUrlRemote() {
        return getString("WS_URL_REMOTE", "ws://127.0.0.1:3001");
    }
    
    public String getWsToken() {
        return getString("WS_TOKEN", "");
    }
    
    public boolean isProxyOpen() {
        return getBoolean("proxy.is.open", false);
    }
    
    public String getProxyHost() {
        return getString("proxy.host", "127.0.0.1");
    }
    
    public int getProxyPort() {
        return getInt("proxy.port", 33210);
    }

    /**
     * 获取 SOCKS 代理端口，优先读取专用配置，回退到通用代理端口
     *
     * @return SOCKS 代理端口号；如果未配置专用 SOCKS 端口，则返回通用代理端口号
     */
    public int getSocksProxyPort() {
        int socksPort = getInt("proxy.port.socks", -1);
        if (socksPort != -1) {
            return socksPort;
        }
        return getProxyPort();
    }
    
    /**
     * 获取默认的 Twitter 用户 ID
     */
    public String getDefaultUID() {
        return getString("DefaultUID", "000000000");
    }
}
