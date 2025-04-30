// (imports remain unchanged)
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class WeatherGUIApp extends JFrame {
    private JTextField cityInput;
    private JTextArea resultArea;
    private JLabel iconLabel;
    private JPanel centerPanel;
    private DefaultListModel<String> historyModel;
    private final String apikey = "2a789ea35a48e047c50152a3af21d4d9";
    private final File historyFile = new File("weather_history.txt");
    private final File themeFile = new File("theme_preference.txt");
    private boolean isDarkMode = false;
    
    // Theme colors
    private final Color LIGHT_BG = new Color(240, 248, 255);
    private final Color DARK_BG = new Color(30, 30, 60);
    private final Color LIGHT_HEADER = new Color(210, 230, 255);
    private final Color DARK_HEADER = new Color(40, 40, 80);
    private final Color LIGHT_TEXT = Color.DARK_GRAY;
    private final Color DARK_TEXT = Color.WHITE;

    public WeatherGUIApp() {
        loadThemePreference();
        setTitle("🌦 Real Weather App - Now with Memory!");
        setSize(460, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top Panel
        JLabel label = new JLabel("Enter City:");
        cityInput = new JTextField(18);
        JButton getWeatherButton = new JButton("🔍 Get Weather");
        JToggleButton darkModeToggle = new JToggleButton(isDarkMode ? "☀️" : "🌙");
        darkModeToggle.setToolTipText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
        darkModeToggle.setSelected(isDarkMode);
        darkModeToggle.setFocusPainted(false);

        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(isDarkMode ? DARK_HEADER : LIGHT_HEADER);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(label);
        inputPanel.add(cityInput);
        inputPanel.add(getWeatherButton);
        inputPanel.add(darkModeToggle);
        
        // Set initial colors based on theme
        label.setForeground(isDarkMode ? DARK_TEXT : LIGHT_TEXT);

        // Center Panel
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBackground(isDarkMode ? DARK_BG : new Color(245, 245, 245));
        resultArea.setForeground(isDarkMode ? DARK_TEXT : LIGHT_TEXT); // Set based on theme

        JScrollPane scrollPane = new JScrollPane(resultArea);
        iconLabel = new JLabel("", SwingConstants.CENTER);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 64));

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG); // Set based on theme
        centerPanel.add(iconLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // History Panel
        historyModel = new DefaultListModel<>();
        loadHistory();
        JList<String> historyList = new JList<>(historyModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Search History"));
        historyScroll.setPreferredSize(new Dimension(150, 100));

        historyList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String city = historyList.getSelectedValue();
                    if (city != null) {
                        cityInput.setText(city);
                        fetchWeather(city);
                    }
                }
            }
        });

        // Footer
        JLabel footer = new JLabel("Powered by OpenWeatherMap", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(isDarkMode ? new Color(180, 180, 180) : Color.GRAY);

        add(inputPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(historyScroll, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        getWeatherButton.addActionListener(e -> {
            String city = cityInput.getText().trim();
            if (!city.isEmpty()) {
                resultArea.setText("⏳ Fetching weather...");
                iconLabel.setIcon(null);
                iconLabel.setText("");
                fetchWeather(city);
                saveToHistory(city);
            } else {
                resultArea.setText("⚠ Please enter a city name.");
                iconLabel.setIcon(null);
                iconLabel.setText("❗");
            }
        });
        
        darkModeToggle.addActionListener(e -> {
            isDarkMode = darkModeToggle.isSelected();
            darkModeToggle.setText(isDarkMode ? "☀️" : "🌙");
            darkModeToggle.setToolTipText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
            applyTheme();
            saveThemePreference();
        });

        setVisible(true);
    }

    private void fetchWeather(String city) {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                try {
                    String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" +
                            city + "&appid=" + apikey + "&units=metric";

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder responseContent = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            responseContent.append(inputLine);
                        }
                        in.close();

                        String json = responseContent.toString();
                        String temp = extractValue(json, "\"temp\":", ",");
                        String feelsLike = extractValue(json, "\"feels_like\":", ",");
                        String humidity = extractValue(json, "\"humidity\":", "}");
                        String windSpeed = extractValue(json, "\"speed\":", ",");
                        String description = extractValue(json, "\"description\":\"", "\"");
                        String cityName = extractValue(json, "\"name\":\"", "\"");
                        String iconCode = extractValue(json, "\"icon\":\"", "\"");
                        String timezoneOffset = extractValue(json, "\"timezone\":", ",");
                        String dt = extractValue(json, "\"dt\":", ",");

                        int offset = Integer.parseInt(timezoneOffset);
                        long timestamp = Long.parseLong(dt);
                        long localTime = timestamp + offset;
                        int hour = (int) ((localTime % 86400) / 3600);

                        animateBackground(hour);

                        Image iconImage = ImageIO.read(new URL("https://openweathermap.org/img/wn/" + iconCode + "@2x.png"));
                        SwingUtilities.invokeLater(() -> {
                            iconLabel.setIcon(new ImageIcon(iconImage));
                            bounceIcon();

                            resultArea.setText("🌈 Weather in " + cityName + "\n\n" +
                                    "📋 Condition   : " + capitalize(description) + "\n" +
                                    "🌡 Temperature : " + temp + "°C\n" +
                                    "🤗 Feels Like  : " + feelsLike + "°C\n" +
                                    "💧 Humidity    : " + humidity + "%\n" +
                                    "💨 Wind Speed  : " + windSpeed + " m/s");
                        });

                    } else {
                        SwingUtilities.invokeLater(() -> {
                            resultArea.setText("❌ Couldn't fetch weather (Code: " + responseCode + ")");
                            iconLabel.setIcon(null);
                            iconLabel.setText("❌");
                        });
                    }

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        resultArea.setText("⚠ Error: " + ex.getMessage());
                        iconLabel.setIcon(null);
                        iconLabel.setText("🚫");
                    });
                }
                return null;
            }
        }.execute();
    }

    private void animateBackground(int hour) {
        // If in dark mode, use dark colors regardless of time
        // If in light mode, use time-based colors
        Color target;
        if (isDarkMode) {
            target = DARK_BG;
        } else {
            target = (hour >= 6 && hour < 18) ? new Color(225, 245, 254) : new Color(30, 30, 60);
        }
        
        Color start = centerPanel.getBackground();

        new Thread(() -> {
            for (int i = 0; i <= 20; i++) {
                int r = start.getRed() + (target.getRed() - start.getRed()) * i / 20;
                int g = start.getGreen() + (target.getGreen() - start.getGreen()) * i / 20;
                int b = start.getBlue() + (target.getBlue() - start.getBlue()) * i / 20;
                Color step = new Color(r, g, b);

                SwingUtilities.invokeLater(() -> centerPanel.setBackground(step));
                try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            }

            // Update text color for visibility after background change
            SwingUtilities.invokeLater(() -> {
                if (isDarkMode) {
                    resultArea.setForeground(DARK_TEXT);
                } else {
                    resultArea.setForeground(hour >= 6 && hour < 18 ? LIGHT_TEXT : DARK_TEXT);
                }
            });

        }).start();
    }

    private void bounceIcon() {
        new Thread(() -> {
            Font original = iconLabel.getFont();
            for (int i = 0; i <= 3; i++) {
                Font f = original.deriveFont(original.getSize() + 6f);
                SwingUtilities.invokeLater(() -> iconLabel.setFont(f));
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(() -> iconLabel.setFont(original));
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void saveToHistory(String city) {
        if (!historyModel.contains(city)) {
            historyModel.addElement(city);
            try (PrintWriter out = new PrintWriter(new FileWriter(historyFile, true))) {
                out.println(city);
            } catch (IOException e) {
                System.out.println("Could not save history: " + e.getMessage());
            }
        }
    }

    private void loadHistory() {
        if (historyFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank() && !historyModel.contains(line.trim())) {
                        historyModel.addElement(line.trim());
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not load history.");
            }
        }
    }

    private String extractValue(String json, String start, String end) {
        int startIndex = json.indexOf(start);
        if (startIndex == -1) return "N/A";
        startIndex += start.length();
        int endIndex = json.indexOf(end, startIndex);
        if (endIndex == -1) return "N/A";
        return json.substring(startIndex, endIndex).replace("\"", "");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void applyTheme() {
        // Apply theme to all components
        SwingUtilities.invokeLater(() -> {
            // Update panel backgrounds
            centerPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
            Component[] components = getContentPane().getComponents();
            for (Component c : components) {
                if (c instanceof JPanel) {
                    if (c == centerPanel) continue; // Skip center panel as it's handled separately
                    
                    if (((JPanel) c).getComponentCount() > 0 && ((JPanel) c).getComponent(0) instanceof JLabel 
                            && "Enter City:".equals(((JLabel)((JPanel) c).getComponent(0)).getText())) {
                        // This is the input panel
                        c.setBackground(isDarkMode ? DARK_HEADER : LIGHT_HEADER);
                        // Update label color
                        ((JLabel)((JPanel) c).getComponent(0)).setForeground(isDarkMode ? DARK_TEXT : LIGHT_TEXT);
                    }
                }
            }
            
            // Update result area
            resultArea.setBackground(isDarkMode ? DARK_BG : new Color(245, 245, 245));
            resultArea.setForeground(isDarkMode ? DARK_TEXT : LIGHT_TEXT);
            
            // Update footer
            for (Component c : components) {
                if (c instanceof JLabel && ((JLabel) c).getText().contains("OpenWeatherMap")) {
                    ((JLabel) c).setForeground(isDarkMode ? new Color(180, 180, 180) : Color.GRAY);
                }
            }
        });
    }
    
    private void saveThemePreference() {
        try (PrintWriter out = new PrintWriter(new FileWriter(themeFile))) {
            out.println(isDarkMode ? "dark" : "light");
        } catch (IOException e) {
            System.out.println("Could not save theme preference: " + e.getMessage());
        }
    }
    
    private void loadThemePreference() {
        if (themeFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
                String theme = reader.readLine();
                if (theme != null) {
                    isDarkMode = "dark".equalsIgnoreCase(theme.trim());
                }
            } catch (IOException e) {
                System.out.println("Could not load theme preference.");
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Nimbus Look-and-Feel not available.");
        }

        SwingUtilities.invokeLater(WeatherGUIApp::new);
    }
}
