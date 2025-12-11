package swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Properties;

public class ConfigEditor extends JFrame {
    // config.properties 相关字段
    private JTextField saveImgDirField;
    private JTextField mediaGetLimitField;
    private JCheckBox proxyOpenCheckBox;
    private JTextField proxyHostField;
    private JTextField proxyPortField;
    private JTextField defaultUIDField;

    // application.yml 相关字段
    private JTextField dbUrlField;
    private JTextField dbUsernameField;
    private JPasswordField dbPasswordField;
    private JTextField twitterClientIdField;
    private JTextField twitterClientSecretField;
    private JTextField twitterCallbackUrlField;

    private JButton saveButton;
    private JButton loadButton;

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private static final String YML_PATH = "src/main/resources/application.yml";

    public ConfigEditor() {
        setTitle("Config Editor - config.properties & application.yml");
        setSize(820, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 创建选项卡界面
        JTabbedPane tabbedPane = new JTabbedPane();

        // config.properties 选项卡
        JPanel propertiesPanel = createPropertiesPanel();
        tabbedPane.addTab("config.properties", propertiesPanel);

        // application.yml 选项卡
        JPanel ymlPanel = createYmlPanel();
        tabbedPane.addTab("application.yml", ymlPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel();
        loadButton = new JButton("加载配置");
        loadButton.addActionListener(new LoadAction());
        buttonPanel.add(loadButton);

        saveButton = new JButton("保存配置");
        saveButton.addActionListener(new SaveAction());
        buttonPanel.add(saveButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 初始加载配置
        loadConfig();
    }

    private JPanel createPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("媒体图片本地抽取路径(用于抽取随机.png):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        saveImgDirField = new JTextField();
        saveImgDirField.setMaximumSize(new Dimension(680, 28));
        saveImgDirField.setColumns(30);
        panel.add(saveImgDirField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("数据库读取最近媒体数量限制:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        mediaGetLimitField = new JTextField();
        mediaGetLimitField.setMaximumSize(new Dimension(160, 28));
        mediaGetLimitField.setColumns(12);
        panel.add(mediaGetLimitField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("是否启用代理:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        proxyOpenCheckBox = new JCheckBox();
        proxyOpenCheckBox.setMaximumSize(new Dimension(160, 28));
        panel.add(proxyOpenCheckBox, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("代理主机地址:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        proxyHostField = new JTextField();
        proxyHostField.setMaximumSize(new Dimension(240, 28));
        proxyHostField.setColumns(20);
        panel.add(proxyHostField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("代理端口:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        proxyPortField = new JTextField();
        proxyPortField.setMaximumSize(new Dimension(120, 28));
        proxyPortField.setColumns(8);
        panel.add(proxyPortField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("默认 Twitter 用户 ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        defaultUIDField = new JTextField();
        defaultUIDField.setMaximumSize(new Dimension(240, 28));
        defaultUIDField.setColumns(22);
        panel.add(defaultUIDField, gbc);

        return panel;
    }

    private JPanel createYmlPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));

        JPanel dbPanel = new JPanel(new GridBagLayout());
        dbPanel.setBorder(BorderFactory.createTitledBorder("数据库配置 (datasource)"));
        GridBagConstraints gbcDb = new GridBagConstraints();
        gbcDb.insets = new Insets(4, 4, 4, 4);
        gbcDb.fill = GridBagConstraints.HORIZONTAL;
        gbcDb.gridx = 0; gbcDb.gridy = 0; gbcDb.weightx = 0;
        dbPanel.add(new JLabel("数据库 URL:"), gbcDb);
        gbcDb.gridx = 1; gbcDb.weightx = 1;
        dbUrlField = new JTextField();
        dbUrlField.setMaximumSize(new Dimension(680, 28));
        dbUrlField.setColumns(30);
        dbPanel.add(dbUrlField, gbcDb);

        gbcDb.gridx = 0; gbcDb.gridy = 1; gbcDb.weightx = 0;
        dbPanel.add(new JLabel("数据库用户名:"), gbcDb);
        gbcDb.gridx = 1; gbcDb.weightx = 1;
        dbUsernameField = new JTextField();
        dbUsernameField.setMaximumSize(new Dimension(420, 28));
        dbUsernameField.setColumns(20);
        dbPanel.add(dbUsernameField, gbcDb);

        gbcDb.gridx = 0; gbcDb.gridy = 2; gbcDb.weightx = 0;
        dbPanel.add(new JLabel("数据库密码:"), gbcDb);
        gbcDb.gridx = 1; gbcDb.weightx = 1;
        dbPasswordField = new JPasswordField();
        dbPasswordField.setMaximumSize(new Dimension(420, 28));
        dbPasswordField.setColumns(20);
        dbPanel.add(dbPasswordField, gbcDb);


        contentPanel.add(dbPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        JPanel twitterPanel = new JPanel(new GridBagLayout());
        twitterPanel.setBorder(BorderFactory.createTitledBorder("Twitter OAuth 2.0 配置"));
        GridBagConstraints gbcTw = new GridBagConstraints();
        gbcTw.insets = new Insets(4, 4, 4, 4);
        gbcTw.fill = GridBagConstraints.HORIZONTAL;
        gbcTw.gridx = 0; gbcTw.gridy = 0; gbcTw.weightx = 0;
        twitterPanel.add(new JLabel("Client ID:"), gbcTw);
        gbcTw.gridx = 1; gbcTw.weightx = 1;
        twitterClientIdField = new JTextField();
        twitterClientIdField.setMaximumSize(new Dimension(420, 28));
        twitterClientIdField.setColumns(24);
        twitterPanel.add(twitterClientIdField, gbcTw);

        gbcTw.gridx = 0; gbcTw.gridy = 1; gbcTw.weightx = 0;
        twitterPanel.add(new JLabel("Client Secret:"), gbcTw);
        gbcTw.gridx = 1; gbcTw.weightx = 1;
        twitterClientSecretField = new JTextField();
        twitterClientSecretField.setMaximumSize(new Dimension(420, 28));
        twitterClientSecretField.setColumns(24);
        twitterPanel.add(twitterClientSecretField, gbcTw);

        gbcTw.gridx = 0; gbcTw.gridy = 2; gbcTw.weightx = 0;
        twitterPanel.add(new JLabel("Callback URL:"), gbcTw);
        gbcTw.gridx = 1; gbcTw.weightx = 1;
        twitterCallbackUrlField = new JTextField();
        twitterCallbackUrlField.setMaximumSize(new Dimension(680, 28));
        twitterCallbackUrlField.setColumns(30);
        twitterPanel.add(twitterCallbackUrlField, gbcTw);


        contentPanel.add(twitterPanel);
        contentPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private void loadConfig() {
        loadPropertiesConfig();
        loadYmlConfig();
    }

    private void loadPropertiesConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
            saveImgDirField.setText(props.getProperty("saveimgdir", ""));
            mediaGetLimitField.setText(props.getProperty("mediagetlimit", ""));
            proxyOpenCheckBox.setSelected(Boolean.parseBoolean(props.getProperty("proxy.is.open", "false")));
            proxyHostField.setText(props.getProperty("proxy.host", ""));
            proxyPortField.setText(props.getProperty("proxy.port", ""));
            defaultUIDField.setText(props.getProperty("DefaultUID", ""));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载 config.properties 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadYmlConfig() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(YML_PATH), "UTF-8"))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            String ymlContent = content.toString();

            // 解析 datasource 配置
            dbUrlField.setText(extractYmlValue(ymlContent, "url:", "jdbc:mysql://数据库地址:3306/数据库名?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"));
            dbUsernameField.setText(extractYmlValue(ymlContent, "username:", "数据库用户名"));
            dbPasswordField.setText(extractYmlValue(ymlContent, "password:", "数据库密码"));

            // 解析 twitter oauth 配置
            twitterClientIdField.setText(extractYmlValue(ymlContent, "client-id:", "你的_CLIENT_ID"));
            twitterClientSecretField.setText(extractYmlValue(ymlContent, "client-secret:", "你的_CLIENT_SECRET"));
            twitterCallbackUrlField.setText(extractYmlValue(ymlContent, "callback-url:", "http://localhost:8090/api/twitter/callback"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载 application.yml 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extractYmlValue(String content, String key, String defaultValue) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#")) {
                continue;
            }
            if (line.contains(key)) {
                String[] parts = line.split(key);
                if (parts.length > 1) {
                    String value = parts[1].trim();
                    if (value.contains("#")) {
                        value = value.split("#")[0].trim();
                    }
                    return value;
                }
            }
        }
        return defaultValue;
    }

    private void saveConfig() {
        savePropertiesConfig();
        saveYmlConfig();
    }

    private void savePropertiesConfig() {
        Properties props = new Properties();
        props.setProperty("saveimgdir", saveImgDirField.getText());
        props.setProperty("mediagetlimit", mediaGetLimitField.getText());
        props.setProperty("proxy.is.open", String.valueOf(proxyOpenCheckBox.isSelected()));
        props.setProperty("proxy.host", proxyHostField.getText());
        props.setProperty("proxy.port", proxyPortField.getText());
        props.setProperty("DefaultUID", defaultUIDField.getText());

        try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH)) {
            props.store(fos, "Config Properties");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存 config.properties 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveYmlConfig() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(YML_PATH), "UTF-8"))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            String ymlContent = content.toString();

            // 替换数据库配置
            ymlContent = replaceYmlValue(ymlContent, "url:", dbUrlField.getText());
            ymlContent = replaceYmlValue(ymlContent, "username:", dbUsernameField.getText());
            ymlContent = replaceYmlValue(ymlContent, "password:", new String(dbPasswordField.getPassword()));

            // 替换 twitter oauth 配置
            ymlContent = replaceYmlValue(ymlContent, "client-id:", twitterClientIdField.getText());
            ymlContent = replaceYmlValue(ymlContent, "client-secret:", twitterClientSecretField.getText());
            ymlContent = replaceYmlValue(ymlContent, "callback-url:", twitterCallbackUrlField.getText());

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(YML_PATH), "UTF-8")) {
                writer.write(ymlContent);
            }
            JOptionPane.showMessageDialog(this, "配置保存成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存 application.yml 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String replaceYmlValue(String content, String key, String newValue) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#")) {
                result.append(line).append("\n");
                continue;
            }
            if (line.contains(key)) {
                String[] parts = line.split(key);
                if (parts.length > 1) {
                    result.append(parts[0]).append(key).append(" ").append(newValue).append("\n");
                } else {
                    result.append(line).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private class LoadAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadConfig();
        }
    }

    private class SaveAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveConfig();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConfigEditor().setVisible(true);
        });
    }
}