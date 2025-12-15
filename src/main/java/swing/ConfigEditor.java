package swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

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

    // ip-whitelist UI
    private DefaultListModel<String> ipListModel;
    private JList<String> ipList;
    private JButton ipAddButton;
    private JButton ipEditButton;
    private JButton ipRemoveButton;

    // pass-tokens UI
    private DefaultListModel<String> passListModel;
    private JList<String> passList;
    private JButton passAddButton;
    private JButton passEditButton;
    private JButton passRemoveButton;

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
        contentPanel.add(Box.createVerticalStrut(15));

        // IP 白名单面板
        JPanel ipPanel = new JPanel(new BorderLayout(6, 6));
        ipPanel.setBorder(BorderFactory.createTitledBorder("IP WhiteList (ip-whitelist)"));
        ipListModel = new DefaultListModel<>();
        ipList = new JList<>(ipListModel);
        ipList.setVisibleRowCount(6);
        ipPanel.add(new JScrollPane(ipList), BorderLayout.CENTER);
        JPanel ipBtnPanel = new JPanel();
        ipAddButton = new JButton("增加");
        ipEditButton = new JButton("编辑");
        ipRemoveButton = new JButton("删除");
        ipBtnPanel.add(ipAddButton);
        ipBtnPanel.add(ipEditButton);
        ipBtnPanel.add(ipRemoveButton);
        ipPanel.add(ipBtnPanel, BorderLayout.SOUTH);
        contentPanel.add(ipPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Pass Tokens 面板
        JPanel passPanel = new JPanel(new BorderLayout(6, 6));
        passPanel.setBorder(BorderFactory.createTitledBorder("Pass Tokens (pass-tokens)"));
        passListModel = new DefaultListModel<>();
        passList = new JList<>(passListModel);
        passList.setVisibleRowCount(6);
        passPanel.add(new JScrollPane(passList), BorderLayout.CENTER);
        JPanel passBtnPanel = new JPanel();
        passAddButton = new JButton("增加");
        passEditButton = new JButton("编辑");
        passRemoveButton = new JButton("删除");
        passBtnPanel.add(passAddButton);
        passBtnPanel.add(passEditButton);
        passBtnPanel.add(passRemoveButton);
        passPanel.add(passBtnPanel, BorderLayout.SOUTH);
        contentPanel.add(passPanel);
        contentPanel.add(Box.createVerticalGlue());

        // 按钮事件绑定
        ipAddButton.addActionListener(e -> {
            String v = JOptionPane.showInputDialog(this, "请输入要添加的 IP:", "添加 IP", JOptionPane.PLAIN_MESSAGE);
            if (v != null && !v.trim().isEmpty()) {
                ipListModel.addElement(v.trim());
            }
        });
        ipEditButton.addActionListener(e -> {
            int idx = ipList.getSelectedIndex();
            if (idx >= 0) {
                String cur = ipListModel.get(idx);
                String v = JOptionPane.showInputDialog(this, "编辑 IP:", cur);
                if (v != null && !v.trim().isEmpty()) {
                    ipListModel.set(idx, v.trim());
                }
            } else {
                JOptionPane.showMessageDialog(this, "请选择一项进行编辑", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        ipRemoveButton.addActionListener(e -> {
            int idx = ipList.getSelectedIndex();
            if (idx >= 0) {
                ipListModel.remove(idx);
            } else {
                JOptionPane.showMessageDialog(this, "请选择一项进行删除", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        passAddButton.addActionListener(e -> {
            String v = JOptionPane.showInputDialog(this, "请输入要添加的 Pass Token:", "添加 Pass Token", JOptionPane.PLAIN_MESSAGE);
            if (v != null && !v.trim().isEmpty()) {
                passListModel.addElement(v.trim());
            }
        });
        passEditButton.addActionListener(e -> {
            int idx = passList.getSelectedIndex();
            if (idx >= 0) {
                String cur = passListModel.get(idx);
                String v = JOptionPane.showInputDialog(this, "编辑 Pass Token:", cur);
                if (v != null && !v.trim().isEmpty()) {
                    passListModel.set(idx, v.trim());
                }
            } else {
                JOptionPane.showMessageDialog(this, "请选择一项进行编辑", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        passRemoveButton.addActionListener(e -> {
            int idx = passList.getSelectedIndex();
            if (idx >= 0) {
                passListModel.remove(idx);
            } else {
                JOptionPane.showMessageDialog(this, "请选择一项进行删除", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

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

            // 解析 ip-whitelist 列表
            List<String> ipListVals = extractYmlList(ymlContent, "ip-whitelist:");
            ipListModel.clear();
            for (String v : ipListVals) ipListModel.addElement(v);

            // 解析 pass-tokens 列表
            List<String> passVals = extractYmlList(ymlContent, "pass-tokens:");
            passListModel.clear();
            for (String v : passVals) passListModel.addElement(v);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载 application.yml 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> extractYmlList(String content, String key) {
        List<String> res = new ArrayList<>();
        String[] lines = content.split("\n");
        boolean inSection = false;
        int keyIndent = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (!inSection) {
                if (trimmed.startsWith(key)) {
                    inSection = true;
                    keyIndent = line.indexOf(trimmed);
                }
            } else {
                if (trimmed.startsWith("-")) {
                    String value = trimmed.substring(1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    res.add(value);
                } else {
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    int indent = line.indexOf(trimmed);
                    if (indent <= keyIndent) break;
                    else break;
                }
            }
        }
        return res;
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

            // 替换 ip-whitelist 列表
            List<String> ipVals = new ArrayList<>();
            for (int i = 0; i < ipListModel.size(); i++) ipVals.add(ipListModel.get(i));
            ymlContent = replaceYmlList(ymlContent, "ip-whitelist:", ipVals);

            // 替换 pass-tokens 列表
            List<String> passVals = new ArrayList<>();
            for (int i = 0; i < passListModel.size(); i++) passVals.add(passListModel.get(i));
            ymlContent = replaceYmlList(ymlContent, "pass-tokens:", passVals);

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(YML_PATH), "UTF-8")) {
                writer.write(ymlContent);
            }
            JOptionPane.showMessageDialog(this, "配置保存成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "保存 application.yml 失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String replaceYmlList(String content, String key, List<String> newValues) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        boolean replaced = false;
        for (int i = 0; i < lines.length; ) {
            String line = lines[i];
            String trimmed = line.trim();
            if (!replaced && trimmed.startsWith(key)) {
                // keep the key line
                result.append(line).append("\n");
                i++;
                // detect indent from existing items if present
                String itemIndent = "  ";
                while (i < lines.length) {
                    String l = lines[i];
                    String t = l.trim();
                    if (t.startsWith("-")) {
                        int dash = l.indexOf("-");
                        if (dash > 0) itemIndent = l.substring(0, dash);
                        i++;
                    } else if (t.isEmpty()) {
                        i++;
                    } else {
                        int indent = l.indexOf(l.trim());
                        int keyIndent = line.indexOf(line.trim());
                        if (indent <= keyIndent) break;
                        else break;
                    }
                }
                // append new values
                for (String v : newValues) {
                    String out = v;
                    if (!((out.startsWith("\"") && out.endsWith("\"")) || (out.startsWith("'") && out.endsWith("'")))) {
                        out = "\"" + out + "\"";
                    }
                    result.append(itemIndent).append("- ").append(out).append("\n");
                }
                replaced = true;
                continue;
            } else {
                result.append(line).append("\n");
                i++;
            }
        }
        if (!replaced) {
            result.append("\n").append(key).append("\n");
            for (String v : newValues) {
                result.append("  - \"").append(v).append("\"\n");
            }
        }
        return result.toString();
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