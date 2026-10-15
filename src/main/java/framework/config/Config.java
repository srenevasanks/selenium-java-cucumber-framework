package framework.config;

import java.io.IOException;
import java.util.Properties;

public final class Config {
  private static final Properties VALUES = new Properties();

  static {
    try (var stream =
        Config.class.getClassLoader().getResourceAsStream("config/config.properties")) {
      if (stream == null) throw new IllegalStateException("Missing config/config.properties");
      VALUES.load(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load configuration", e);
    }
  }

  private Config() {}

  public static String get(String key) {
    return System.getProperty(key, VALUES.getProperty(key, ""));
  }

  public static int integer(String key) {
    return Integer.parseInt(get(key));
  }

  public static boolean bool(String key) {
    return Boolean.parseBoolean(get(key));
  }
}
