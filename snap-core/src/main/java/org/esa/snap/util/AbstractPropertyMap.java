package org.esa.snap.util;

import java.awt.Color;
import java.awt.Font;

/**
 * The <code>PropertyMap</code> class can be used instead of the standard JDK <code>java.util.Properties</code>
 * class.<code>PropertyMap</code> provides a generally more useful interface by adding a couple type conversion methods
 * for a set of most frequently used data types, such as <code>Boolean</code>, <code>Integer</code>,
 * <code>Double</code>, <code>Color</code> and <code>Font</code>.
 * <p>Additionally the class provides property change support.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public abstract class AbstractPropertyMap implements PropertyMap {
    public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Color DEFAULT_COLOR = Color.BLACK;

    /**
     * Gets a value of type <code>boolean</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>false</code> if the key is not contained in this property set.
     */
    @Override
    public boolean getPropertyBool(String key) {
        return getPropertyBool(key, false);
    }

    /**
     * Gets a value of type <code>Boolean</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Boolean getPropertyBool(String key, Boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.valueOf(value) : defaultValue;
    }

    /**
     * Sets a value of type <code>Boolean</code>.
     *
     * @param key      the key
     * @param newValue the new value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyBool(String key, Boolean newValue) {
        set(key, newValue != null ? Boolean.toString(newValue) : null);
    }

    /**
     * Gets a value of type <code>int</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0</code> (zero) if the key is not contained in this property set.
     */
    @Override
    public int getPropertyInt(String key) {
        return getPropertyInt(key, 0);
    }

    /**
     * Gets a value of type <code>Integer</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Integer getPropertyInt(String key, Integer defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value of type <code>Integer</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyInt(String key, Integer value) {
        set(key, value != null ? Integer.toString(value) : null);
    }

    /**
     * Gets a value of type <code>double</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0.0</code> (zero) if the key is not contained in this property
     * set.
     */
    @Override
    public double getPropertyDouble(String key) {
        return getPropertyDouble(key, 0.0);
    }

    /**
     * Gets a value of type <code>Double</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Double getPropertyDouble(String key, Double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value of type <code>Double</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyDouble(String key, Double value) {
        set(key, value != null ? Double.toString(value) : null);
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>""</code> (empty string) if the key is not contained in this
     * property set, never <code>null</code>.
     */
    @Override
    public String getPropertyString(String key) {
        return get(key, "");
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public String getPropertyString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Sets a value of type <code>String</code>.
     *
     * @param key   the key
     * @param value the new value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyString(String key, String value) {
        set(key, value);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>Color.black</code> if the key is not contained in this property
     * set, never <code>null</code>.
     */
    @Override
    public Color getPropertyColor(String key) {
        return getPropertyColor(key, DEFAULT_COLOR);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Color getPropertyColor(String key, Color defaultValue) {
        Guardian.assertNotNullOrEmpty("key", key);
        String value = get(key);
        if (value != null) {
            Color color = StringUtils.parseColor(value);
            if (color != null) {
                return color;
            }
        }
        return defaultValue;
    }

    /**
     * Sets a value of type <code>Color</code>.
     *
     * @param key      the key
     * @param newValue the value
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyColor(String key, Color newValue) {
        set(key, StringUtils.formatColor(newValue));
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key the key
     * @return the value for the given key, or a plain, 12-point "SandSerif" font if the key is not contained in this
     * property set, never <code>null</code>.
     */
    @Override
    public Font getPropertyFont(String key) {
        return getPropertyFont(key, DEFAULT_FONT);
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    @Override
    public Font getPropertyFont(String key, Font defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        String[] parts = value.split(";");
        if (parts.length == 0 || parts.length > 3) {
            return defaultValue;
        }

        String fontName = parts[0];
        int fontStyle = DEFAULT_FONT.getStyle();
        int fontSize = DEFAULT_FONT.getSize();

        if (parts.length >= 2) {
            String styleValue = parts[1];
            if ("BOLD".equalsIgnoreCase(styleValue)) {
                fontStyle = Font.BOLD;
            } else if ("ITALIC".equalsIgnoreCase(styleValue)) {
                fontStyle = Font.ITALIC;
            }
        }
        if (parts.length >= 3) {
            fontSize = Integer.parseInt(parts[2], 10);
        }

        return new Font(fontName, fontStyle, fontSize);
    }

    /**
     * Sets a font of type <code>Font</code>. The method actually puts three keys in this property set in order to store
     * the font's properties:
     * <ul>
     * <li><code>&lt;key&gt;.name</code> for the font's name</li>
     * <li><code>&lt;key&gt;.style</code> for the font's style (an integer font)</li>
     * <li><code>&lt;key&gt;.name</code> for the font's size in points (an integer font)</li>
     * </ul>
     *
     * @param key  the key
     * @param font the font
     * @throws IllegalArgumentException
     */
    @Override
    public void setPropertyFont(String key, Font font) {
        String value = null;
        if (font != null) {
            String styleValue = "PLAIN";
            int style = font.getStyle();
            if (style == Font.ITALIC) {
                styleValue = "ITALIC";
            } else if (style == Font.BOLD) {
                styleValue = "BOLD";
            }
            value = String.format("%s;%s;%s", font.getName(), styleValue, font.getSize());
        }
        set(key, value);
    }

    protected abstract String get(String key);

    protected abstract String get(String key, String defaultValue);

    protected abstract String set(String key, String value);

    protected abstract void firePropertyChange(String key, String oldValue, String newValue);
}
