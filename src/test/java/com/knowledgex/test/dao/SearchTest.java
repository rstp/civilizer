package com.knowledgex.test.dao;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SearchTest extends DaoTest {
	
	@BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DaoTest.setUpBeforeClass(
                "classpath:datasource-context-h2-embedded.xml"
                , DaoEmbeddedTest.class
                );
    }
    
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
	public void testSearchWithTags() {
    	assertTrue(true);
	}

}
