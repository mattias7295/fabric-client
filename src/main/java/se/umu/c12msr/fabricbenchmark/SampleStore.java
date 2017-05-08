package se.umu.c12msr.fabricbenchmark;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Mattias Scherer on 3/20/17.
 */
public class SampleStore {
    private String file;
    private Log logger = LogFactory.getLog(SampleStore.class);

    public SampleStore(File file) {

        this.file = file.getAbsolutePath();
    }

    /**
     * Get the value associated with name.
     *
     * @param name
     * @return value associated with the name
     */
    public String getValue(String name) {
        Properties properties = loadProperties();
        return properties.getProperty(name);
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            input.close();
        } catch (FileNotFoundException e) {
            logger.warn(String.format("Could not find the file \"%s\"", file));
        } catch (IOException e) {
            logger.warn(String.format("Could not load keyvalue store from file \"%s\", reason:%s",
                    file, e.getMessage()));
        }

        return properties;
    }

    /**
     * Set the value associated with name.
     *
     * @param name  The name of the parameter
     * @param value Value for the parameter
     */
    public void setValue(String name, String value) {
        Properties properties = loadProperties();
        try (
                OutputStream output = new FileOutputStream(file)
        ) {
            properties.setProperty(name, value);
            properties.store(output, "");
            output.close();

        } catch (IOException e) {
            logger.warn(String.format("Could not save the keyvalue store, reason:%s", e.getMessage()));
        }
    }
    private final Map<String, SampleUser> members = new HashMap<>();
    /**
     * Get the user with a given name
     *
     * @return user
     */
    public SampleUser getMember(String name) {

        // Try to get the SampleUser state from the cache
        SampleUser sampleUser = members.get(name);
        if (null != sampleUser) return sampleUser;

        // Create the SampleUser and try to restore it's state from the key value store (if found).
        sampleUser = new SampleUser(name, this);

        return sampleUser;

    }
}
