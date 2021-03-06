package com.civilizer.test.helper;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.civilizer.config.AppOptions;
import com.civilizer.config.Configurator;
import com.civilizer.dao.FileEntityDao;
import com.civilizer.domain.FileEntity;
import com.civilizer.utils.FsUtil;

public final class TestUtil {

    private static final Logger logger = newLogger();
    private static final Random random = newRandomGenerator();
    
    private static Logger newLogger() {
    	String log4jPropName = "log4j-test.properties";
    	URL url = ClassLoader.getSystemClassLoader().getResource(log4jPropName);
    	assertNotNull(url);
    	String log4jPropPath = url.getPath();
    	assertTrue(log4jPropPath != null && log4jPropPath.isEmpty() == false);
    	PropertyConfigurator.configure(log4jPropPath);

    	return newLogger(TestUtil.class);
    }

    public static Logger newLogger(Class<?> clazz) {
        Logger output = LoggerFactory.getLogger(clazz);
        assertNotNull(output);      
        return output;
    }
    
    private static Random newRandomGenerator() {
		Calendar cal = Calendar.getInstance();
        assertNotNull(cal);

        long seed = cal.getTimeInMillis();
//        seed = 1439185021758L;
        
        logger.info("random seed = " + seed);

        Random random = new Random(seed);
        assertNotNull(random);

        return random;
	}

    public static Random getRandom() {
        assertNotNull(random);
        return random;
    }

    public static String randomString(Random r, int minCount, int maxCount) {
        minCount = Math.max(Math.min(minCount, maxCount), 1);
        maxCount = Math.max(minCount, maxCount);
        final int stringCount = minCount + r.nextInt(maxCount - minCount);
        char[] s = new char[stringCount];

        for (int i = 0; i < stringCount; ++i) {
            s[i] = (char) (32 + r.nextInt(127 - 32));
        }

        return new String(s);
    }
    
    public static int[] randomIndices(Random r, int minCount, int maxCount) {
    	minCount = Math.max(Math.min(minCount, maxCount), 1);
        maxCount = Math.max(minCount, maxCount);
        List<Integer> tmp = new ArrayList<>(maxCount);
        for (int i = 0; i < maxCount; i++) {
        	tmp.add(i);
		}
        Collections.shuffle(tmp);
        final int outputCount = minCount + r.nextInt(maxCount - minCount);
        int[] output = new int[outputCount];
        for (int i = 0; i < outputCount; i++) {
        	output[i] = tmp.get(i);
		}
        return output;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj, Class<T> type) {
        assertNotNull(obj);
        assertSame(obj.getClass().getName(), type.getName());
        return (T) obj;
    }

    public static <T> void checkIfNoDuplicateExistsIn(Collection<T> coll) {
    	if (coll instanceof Set == false) {
    		Set<T> s = new HashSet<T>(coll);
    		assertEquals(s.size(), coll.size());
    	}
    }
    
    public static String getPrivateHomePath() {
        final String path = FsUtil.concatPath(System.getProperty("user.dir"), "test/private-home");
        return path;
    }
    
    public static void configure() {
    	final String path = getPrivateHomePath();
    	System.setProperty(AppOptions.PRIVATE_HOME_PATH, path);
    	new Configurator();
    }

    public static void unconfigure() {
    	System.clearProperty(AppOptions.PRIVATE_HOME_PATH);
    }
    
    public static String getDatabaseFilePath() {
        assertNotNull(System.getProperty(AppOptions.DB_FILE_PREFIX));
        assertNotNull(System.getProperty(AppOptions.DB_FILE_SUFFIX));
        return System.getProperty(AppOptions.DB_FILE_PREFIX) + System.getProperty(AppOptions.DB_FILE_SUFFIX);
    }

    public static String getFilesHomePath() {
        if (System.getProperty(AppOptions.FILE_BOX_HOME) != null)
            return System.getProperty(AppOptions.FILE_BOX_HOME);
        assertNotNull(System.getProperty(AppOptions.PRIVATE_HOME_PATH));
    	return FsUtil.concatPath(System.getProperty(AppOptions.PRIVATE_HOME_PATH),"files");
    }

    public static String getTempFolderPath() {
        if (System.getProperty(AppOptions.TEMP_PATH) != null)
            return System.getProperty(AppOptions.TEMP_PATH);
        assertNotNull(System.getProperty(AppOptions.PRIVATE_HOME_PATH));
        return FsUtil.concatPath(System.getProperty(AppOptions.PRIVATE_HOME_PATH), ".temp");
    }
    
    public static void touchTestFilesForFileBox(FileEntityDao fileEntityDao) {
    	final String filesHome = getFilesHomePath();
    	List<FileEntity> fileEntitiesFromDB = fileEntityDao.findAll();
		assertNotNull(fileEntitiesFromDB);
		
		for (FileEntity fe : fileEntitiesFromDB) {
			final File f = fe.toFile(filesHome);
			try {
				FileUtils.touch(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			assertEquals(true, f.isFile());
		}
    }
}
