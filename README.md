# 🌦 Real Weather App - Now with Memory!

Hey there! 👋  
Welcome to my Weather App — a fun little Java Swing project that fetches real-time weather data and remembers where you've been searching! ☁️🌡️

This is a desktop GUI application that uses the **OpenWeatherMap API** to fetch weather data for any city you type in. It’s got a clean interface, animated backgrounds, weather icons, and even a search history you can double-click to reuse!

---

## 🧠 Features

- 🔍 Get real-time weather info by typing a city name  
- 🌈 Cute weather icons and animated background based on time of day  
- 💾 Search history saved locally (`weather_history.txt`)  
- 🧠 Double-click a past city to fetch its weather again  
- 🌐 Powered by OpenWeatherMap API  
- 💡 Simple, smooth, and responsive UI with Swing  

---

## 🛠 How It Works

1. Enter a city name and click "Get Weather"
2. The app sends a request to OpenWeatherMap API
3. Displays temperature, humidity, wind speed, and description
4. Background color changes based on day/night in the city
5. Your search gets saved to a local history file
6. You can click any item in the history list to instantly reload it

---

## 📦 Tech Stack

- Java (Swing for UI)
- OpenWeatherMap API
- `SwingWorker` for background API calls
- Plain file I/O for search history

---

## 🚀 Getting Started

### 1. Clone the Repo

```bash
git clone https://github.com/yourusername/weather-gui-app.git
cd weather-gui-app
```

### 2. Add your API key
Go to OpenWeatherMap, sign up and grab your free API key.
Then, replace this line in the code:
 ```java
private final String apikey = "your_api_key_here";
```

### 3. Compile and run
```bash
javac WeatherGUIApp.java
java WeatherGUIApp
```
That’s it! 🎉

## Known Limitations
- No advanced error handling yet (gibberish city names crash it)
- Weather forecast (hourly/daily) not supported (yet!)
- History file can only grow — no delete/edit from the GUI

## 🙌 Credits
- [Openweather API](https://openweathermap.org/)
- Java Swing documentation
- Stack Overflow (yes, we all do it 😄)
