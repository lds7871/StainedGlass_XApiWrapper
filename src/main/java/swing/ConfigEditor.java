package swing;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 快速配置编辑器 —— 用于可视化修改 config.properties 和 application.yml
 */
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
    private JTextField serverPortField;

    // ip-whitelist UI
    private DefaultListModel<String> ipListModel;
    private JList<String> ipList;

    // pass-tokens UI
    private DefaultListModel<String> passListModel;
    private JList<String> passList;

    // 状态栏
    private JLabel statusLabel;

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private static final String YML_PATH    = "src/main/resources/application.yml";

    public ConfigEditor() {
        setTitle("配置编辑器 — config.properties & application.yml");
        setSize(860, 720);
        setMinimumSize(new Dimension(720, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("config.properties", createPropertiesPanel());
        tabbedPane.addTab("application.yml",    createYmlPanel());
        add(tabbedPane, BorderLayout.CENTER);

        // 底部区域（按钮 + 状态栏）
        JPanel bottomPanel = new JPanel(new BorderLayout(4, 4));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        JButton loadButton = new JButton("加载配置");
        JButton saveButton = new JButton("保存配置");
        loadButton.addActionListener(e -> loadConfig());
        saveButton.addActionListener(e -> saveConfig());
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(new Color(0, 128, 0));
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
        loadConfig();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // config.properties 面板
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        var gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;

        row = addFormRow(panel, gbc, row, "媒体图片本地抽取路径 (随机 .png):",
                saveImgDirField = new JTextField(32));
        row = addFormRow(panel, gbc, row, "数据库读取最近媒体数量限制:",
                mediaGetLimitField = new JTextField(10));
        row = addFormRow(panel, gbc, row, "默认 Twitter 用户 ID:",
                defaultUIDField = new JTextField(22));

        // 代理区域
        JPanel proxySection = new JPanel(new GridBagLayout());
        proxySection.setBorder(BorderFactory.createTitledBorder("代理配置"));
        var gp = new GridBagConstraints();
        gp.insets = new Insets(4, 4, 4, 4);
        gp.fill   = GridBagConstraints.HORIZONTAL;

        gp.gridx = 0; gp.gridy = 0; gp.weightx = 0;
        proxySection.add(new JLabel("是否启用代理:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        proxyOpenCheckBox = new JCheckBox();
        proxySection.add(proxyOpenCheckBox, gp);

        gp.gridx = 0; gp.gridy = 1; gp.weightx = 0;
        proxySection.add(new JLabel("代理主机地址:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        proxyHostField = new JTextField(20);
        proxySection.add(proxyHostField, gp);

        gp.gridx = 0; gp.gridy = 2; gp.weightx = 0;
        proxySection.add(new JLabel("代理端口:"), gp);
        gp.gridx = 1; gp.weightx = 1;
        proxyPortField = new JTextField(8);
        proxySection.add(proxyPortField, gp);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1;
        panel.add(proxySection, gbc);
        gbc.gridwidth = 1;

        return panel;
    }

    /** Adds a label+field row and returns the next row index. */
    private int addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                           String labelText, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panel.add(field, gbc);
        return row + 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // application.yml 面板
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createYmlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // 服务器端口
        JPanel serverPanel = buildGridPanel("服务器配置 (server)");
        var gs = defaultGbc();
        gs.gridx = 0; gs.gridy = 0; gs.weightx = 0;
        serverPanel.add(new JLabel("服务器端口:"), gs);
        gs.gridx = 1; gs.weightx = 1;
        serverPortField = new JTextField(8);
        serverPanel.add(serverPortField, gs);
        contentPanel.add(serverPanel);
        contentPanel.add(vGap());

        // 数据库
        JPanel dbPanel = buildGridPanel("数据库配置 (datasource)");
        var gd = defaultGbc();
        gd.gridx = 0; gd.gridy = 0; gd.weightx = 0;
        dbPanel.add(new JLabel("数据库 URL:"), gd);
        gd.gridx = 1; gd.weightx = 1;
        dbUrlField = new JTextField(36);
        dbPanel.add(dbUrlField, gd);

        gd.gridx = 0; gd.gridy = 1; gd.weightx = 0;
        dbPanel.add(new JLabel("数据库用户名:"), gd);
        gd.gridx = 1; gd.weightx = 1;
        dbUsernameField = new JTextField(20);
        dbPanel.add(dbUsernameField, gd);

        gd.gridx = 0; gd.gridy = 2; gd.weightx = 0;
        dbPanel.add(new JLabel("数据库密码:"), gd);
        gd.gridx = 1; gd.weightx = 1;
        dbPasswordField = new JPasswordField(20);
        dbPanel.add(dbPasswordField, gd);
        contentPanel.add(dbPanel);
        contentPanel.add(vGap());

        // Twitter OAuth
        JPanel twitterPanel = buildGridPanel("Twitter OAuth 2.0 配置");
        var gt = defaultGbc();
        gt.gridx = 0; gt.gridy = 0; gt.weightx = 0;
        twitterPanel.add(new JLabel("Client ID:"), gt);
        gt.gridx = 1; gt.weightx = 1;
        twitterClientIdField = new JTextField(28);
        twitterPanel.add(twitterClientIdField, gt);

        gt.gridx = 0; gt.gridy = 1; gt.weightx = 0;
        twitterPanel.add(new JLabel("Client Secret:"), gt);
        gt.gridx = 1; gt.weightx = 1;
        twitterClientSecretField = new JTextField(28);
        twitterPanel.add(twitterClientSecretField, gt);

        gt.gridx = 0; gt.gridy = 2; gt.weightx = 0;
        twitterPanel.add(new JLabel("Callback URL:"), gt);
        gt.gridx = 1; gt.weightx = 1;
        twitterCallbackUrlField = new JTextField(36);
        twitterPanel.add(twitterCallbackUrlField, gt);
        contentPanel.add(twitterPanel);
        contentPanel.add(vGap());

        // IP 白名单
        contentPanel.add(buildListPanel("IP WhiteList (ip-whitelist)",
                ipListModel = new DefaultListModel<>(),
                ipList      = new JList<>()));
        contentPanel.add(vGap());

        // Pass Tokens
        contentPanel.add(buildListPanel("Pass Tokens (pass-tokens)",
                passListModel = new DefaultListModel<>(),
                passList      = new JList<>()));
        contentPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    /** Creates a titled panel with GridBagLayout. */
    private JPanel buildGridPanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    /** Default GridBagConstraints for form rows. */
    private GridBagConstraints defaultGbc() {
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    /** Small vertical spacer for BoxLayout. */
    private Component vGap() {
        return Box.createVerticalStrut(10);
    }

    /**
     * Builds a list management panel (IP whitelist or pass-token list)
     * with 增加 / 编辑 / 删除 buttons.
     */
    private JPanel buildListPanel(String title,
                                  DefaultListModel<String> model,
                                  JList<String> list) {
        list.setModel(model);
        list.setVisibleRowCount(5);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JButton addBtn    = new JButton("增加");
        JButton editBtn   = new JButton("编辑");
        JButton removeBtn = new JButton("删除");

        addBtn.addActionListener(e -> {
            String v = JOptionPane.showInputDialog(this, "请输入值:", "增加", JOptionPane.PLAIN_MESSAGE);
            if (v != null && !v.isBlank()) model.addElement(v.strip());
        });
        editBtn.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                showInfo("请先选择一项再编辑");
                return;
            }
            String v = JOptionPane.showInputDialog(this, "编辑值:", model.get(idx));
            if (v != null && !v.isBlank()) model.set(idx, v.strip());
        });
        removeBtn.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx < 0) {
                showInfo("请先选择一项再删除");
                return;
            }
            model.remove(idx);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(removeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load / Save
    // ─────────────────────────────────────────────────────────────────────────

    private void loadConfig() {
        loadPropertiesConfig();
        loadYmlConfig();
        setStatus("配置已加载", false);
    }

    private void saveConfig() {
        savePropertiesConfig();
        saveYmlConfig();
    }

    private void loadPropertiesConfig() {
        var props = new Properties();
        try (var fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
            saveImgDirField.setText(props.getProperty("saveimgdir", ""));
            mediaGetLimitField.setText(props.getProperty("mediagetlimit", ""));
            proxyOpenCheckBox.setSelected(Boolean.parseBoolean(props.getProperty("proxy.is.open", "false")));
            proxyHostField.setText(props.getProperty("proxy.host", ""));
            proxyPortField.setText(props.getProperty("proxy.port", ""));
            defaultUIDField.setText(props.getProperty("DefaultUID", ""));
        } catch (IOException e) {
            showError("加载 config.properties 失败: " + e.getMessage());
        }
    }

    private void loadYmlConfig() {
        try (var reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(YML_PATH), StandardCharsets.UTF_8))) {
            var sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            String yml = sb.toString();

            dbUrlField.setText(extractYmlValue(yml, "url:"));
            dbUsernameField.setText(extractYmlValue(yml, "username:"));
            dbPasswordField.setText(extractYmlValue(yml, "password:"));
            twitterClientIdField.setText(extractYmlValue(yml, "client-id:"));
            twitterClientSecretField.setText(extractYmlValue(yml, "client-secret:"));
            twitterCallbackUrlField.setText(extractYmlValue(yml, "callback-url:"));
            serverPortField.setText(extractYmlValue(yml, "port:"));

            ipListModel.clear();
            extractYmlList(yml, "ip-whitelist:").forEach(ipListModel::addElement);

            passListModel.clear();
            extractYmlList(yml, "pass-tokens:").forEach(passListModel::addElement);
        } catch (IOException e) {
            showError("加载 application.yml 失败: " + e.getMessage());
        }
    }

    private void savePropertiesConfig() {
        var props = new Properties();
        props.setProperty("saveimgdir",      saveImgDirField.getText());
        props.setProperty("mediagetlimit",   mediaGetLimitField.getText());
        props.setProperty("proxy.is.open",   String.valueOf(proxyOpenCheckBox.isSelected()));
        props.setProperty("proxy.host",      proxyHostField.getText());
        props.setProperty("proxy.port",      proxyPortField.getText());
        props.setProperty("DefaultUID",      defaultUIDField.getText());
        try (var fos = new FileOutputStream(CONFIG_PATH)) {
            props.store(fos, "Config Properties");
        } catch (IOException e) {
            showError("保存 config.properties 失败: " + e.getMessage());
            return;
        }
        setStatus("config.properties 保存成功", false);
    }

    private void saveYmlConfig() {
        try {
            String yml;
            try (var reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(YML_PATH), StandardCharsets.UTF_8))) {
                var sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                yml = sb.toString();
            }

            yml = replaceYmlValue(yml, "url:",           dbUrlField.getText());
            yml = replaceYmlValue(yml, "username:",      dbUsernameField.getText());
            yml = replaceYmlValue(yml, "password:",      new String(dbPasswordField.getPassword()));
            yml = replaceYmlValue(yml, "client-id:",     twitterClientIdField.getText());
            yml = replaceYmlValue(yml, "client-secret:", twitterClientSecretField.getText());
            yml = replaceYmlValue(yml, "callback-url:",  twitterCallbackUrlField.getText());
            yml = replaceYmlValue(yml, "port:",          serverPortField.getText());

            var ipVals = new ArrayList<String>();
            for (int i = 0; i < ipListModel.size(); i++) ipVals.add(ipListModel.get(i));
            yml = replaceYmlList(yml, "ip-whitelist:", ipVals);

            var passVals = new ArrayList<String>();
            for (int i = 0; i < passListModel.size(); i++) passVals.add(passListModel.get(i));
            yml = replaceYmlList(yml, "pass-tokens:", passVals);

            try (var writer = new OutputStreamWriter(
                    new FileOutputStream(YML_PATH), StandardCharsets.UTF_8)) {
                writer.write(yml);
            }
            setStatus("application.yml 保存成功 ✓", false);
            JOptionPane.showMessageDialog(this, "配置保存成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            showError("保存 application.yml 失败: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // YAML helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the scalar value of a YAML key from the raw content string.
     * Skips comment lines; returns empty string if the key is not found.
     */
    private String extractYmlValue(String content, String key) {
        for (String line : content.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("#")) continue;
            int keyIdx = line.indexOf(key);
            if (keyIdx < 0) continue;
            // Ensure it is actually a key (not a value that happens to contain the key string)
            String before = line.substring(0, keyIdx).strip();
            if (!before.isEmpty()) continue;
            String after = line.substring(keyIdx + key.length()).strip();
            // Strip inline comment
            int commentIdx = after.indexOf(" #");
            if (commentIdx >= 0) after = after.substring(0, commentIdx).strip();
            return after;
        }
        return "";
    }

    /**
     * Extracts a YAML sequence (list items starting with "-") for the given key.
     */
    private List<String> extractYmlList(String content, String key) {
        List<String> result = new ArrayList<>();
        String[] lines = content.split("\n");
        boolean inSection = false;
        int keyIndent = -1;

        for (String line : lines) {
            String trimmed = line.strip();
            if (!inSection) {
                if (trimmed.startsWith(key) && line.strip().equals(trimmed)) {
                    inSection  = true;
                    keyIndent  = line.indexOf(trimmed.charAt(0));
                }
            } else {
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("-")) {
                    String value = trimmed.substring(1).strip();
                    // Strip surrounding quotes
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    result.add(value);
                } else {
                    // Non-list, non-empty line: exit if indent <= key indent
                    int indent = line.indexOf(trimmed.charAt(0));
                    if (indent <= keyIndent) break;
                }
            }
        }
        return result;
    }

    /**
     * Replaces the scalar value of a YAML key in the raw content string.
     * Only the first non-comment line containing the key as a leading token is replaced.
     */
    private String replaceYmlValue(String content, String key, String newValue) {
        String[] lines = content.split("\n", -1);
        var result = new StringBuilder();
        boolean replaced = false;
        for (String line : lines) {
            String trimmed = line.strip();
            if (!replaced && !trimmed.startsWith("#")) {
                int keyIdx = line.indexOf(key);
                if (keyIdx >= 0) {
                    String before = line.substring(0, keyIdx).strip();
                    if (before.isEmpty()) {
                        // Preserve original indentation
                        String indent = line.substring(0, line.indexOf(trimmed.charAt(0)));
                        result.append(indent).append(key).append(' ').append(newValue).append('\n');
                        replaced = true;
                        continue;
                    }
                }
            }
            result.append(line).append('\n');
        }
        return result.toString();
    }

    /**
     * Replaces the list items under a YAML key with the provided values.
     */
    private String replaceYmlList(String content, String key, List<String> newValues) {
        String[] lines = content.split("\n", -1);
        var result = new StringBuilder();
        boolean inSection = false;
        boolean replaced  = false;
        int     keyIndent = 0;

        for (int i = 0; i < lines.length; ) {
            String line    = lines[i];
            String trimmed = line.strip();

            if (!replaced && !inSection && trimmed.startsWith(key)) {
                inSection = true;
                keyIndent = line.indexOf(trimmed.charAt(0));
                result.append(line).append('\n');
                i++;

                // Detect item indent from existing items (default: keyIndent + 2 spaces)
                String itemIndent = " ".repeat(keyIndent + 2);
                while (i < lines.length) {
                    String l = lines[i];
                    String t = l.strip();
                    if (t.isEmpty()) { i++; continue; }
                    if (t.startsWith("-")) {
                        int dash = l.indexOf('-');
                        if (dash > 0) itemIndent = l.substring(0, dash);
                        i++;
                    } else {
                        break; // reached the next sibling key
                    }
                }
                // Write new values
                for (String v : newValues) {
                    String out = "\"" + v + "\"";
                    result.append(itemIndent).append("- ").append(out).append('\n');
                }
                replaced = true;
                continue;
            }
            result.append(line).append('\n');
            i++;
        }

        if (!replaced) {
            result.append('\n').append(key).append('\n');
            for (String v : newValues) result.append("  - \"").append(v).append("\"\n");
        }
        return result.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : new Color(0, 128, 0));
    }

    private void showError(String message) {
        setStatus(message, true);
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConfigEditor().setVisible(true));
    }
}
