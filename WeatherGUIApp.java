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
    private boolean isDarkMode = false; // Dark mode flag (starts in light mode)

    public WeatherGUIApp() {
        setTitle("🌦 Real Weather App - Now with Memory!");
        setSize(460, 480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top Panel
        JLabel label = new JLabel("Enter City:");
        cityInput = new JTextField(18);
        JButton getWeatherButton = new JButton("🔍 Get Weather");
        JButton refreshButton = new JButton("🔄 Refresh");
        JCheckBox darkModeCheckBox = new JCheckBox("Dark Mode", false); // Default to Light Mode

        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(new Color(210, 230, 255));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(label);
        inputPanel.add(cityInput);
        inputPanel.add(getWeatherButton);
        inputPanel.add(refreshButton);
        inputPanel.add(darkModeCheckBox);

        // Center Panel
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setBackground(new Color(240, 248, 255)); // Light mode default
        resultArea.setForeground(Color.BLACK); // Default text color (black for light mode)

        JScrollPane scrollPane = new JScrollPane(resultArea);
        iconLabel = new JLabel("", SwingConstants.CENTER);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 64));

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(240, 248, 255)); // Light mode default
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
        footer.setForeground(Color.GRAY);

        add(inputPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(historyScroll, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        // Fetch weather when "Get Weather" button is clicked
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

        // Refresh weather when "Refresh" button is clicked
        refreshButton.addActionListener(e -> {
            String city = cityInput.getText().trim();
            if (!city.isEmpty()) {
                fetchWeather(city);
            }
            // Clear the search history
            historyModel.clear();
            // Optionally, delete the file to reset saved history
            if (historyFile.exists()) {
                historyFile.delete();
            }
        });
        

        // Toggle dark mode when checkbox is checked/unchecked
        darkModeCheckBox.addActionListener(e -> {
            isDarkMode = darkModeCheckBox.isSelected();
            applyMode();
        });

        setVisible(true);
    }

    private void applyMode() {
        // Light Mode
        if (!isDarkMode) {
            resultArea.setBackground(new Color(240, 248, 255)); // Light mode background
            resultArea.setForeground(Color.BLACK); // Black text in light mode
            centerPanel.setBackground(new Color(240, 248, 255)); // Light mode background
            cityInput.setBackground(Color.WHITE); // Light background for input
            cityInput.setForeground(Color.BLACK); // Black text in input field
        }
        // Dark Mode
        else {
            resultArea.setBackground(new Color(30, 30, 60)); // Dark mode background
            resultArea.setForeground(Color.WHITE); // White text in dark mode
            centerPanel.setBackground(new Color(30, 30, 60)); // Dark mode background
            cityInput.setBackground(new Color(50, 50, 70)); // Dark background for input
            cityInput.setForeground(Color.WHITE); // White text in input field
        }
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
        Color target = (hour >= 6 && hour < 18) ? new Color(225, 245, 254) : new Color(30, 30, 60);
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
                resultArea.setForeground(hour >= 6 && hour < 18 ? Color.DARK_GRAY : Color.WHITE);
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