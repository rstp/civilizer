package com.civilizer.test.dao;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.hibernate.Hibernate;
import org.springframework.context.support.GenericXmlApplicationContext;

import com.civilizer.dao.*;
import com.civilizer.domain.*;
import com.civilizer.test.util.TestUtil;

class DaoTest {

	protected static Log logger;
	protected static GenericXmlApplicationContext ctx;
	private static int counter = 0;

	protected FragmentDao fragmentDao;
	protected TagDao tagDao;
	protected FileEntityDao fileEntityDao;

	private final List<Tag> temporalTags = new ArrayList<Tag>();
	private final List<Fragment> temporalFragments = new ArrayList<Fragment>();

	protected static void setUpBeforeClass(
	    String dataSourceContextPath
	  , Class<?> clazz
	) throws Exception {
		logger = TestUtil.newLogger(clazz);

		ctx = new GenericXmlApplicationContext();
		ctx.load(dataSourceContextPath);
		ctx.refresh();
		logger.info("GenericXmlApplicationContext initialized OK");
	}
	
	protected void deleteAllTemporalObjects() {
		for (Tag t : temporalTags) {
			if (tagDao.findById(t.getId()) != null) {
				tagDao.delete(t);
			}
		}

		for (Fragment frg : temporalFragments) {
			if (fragmentDao.findById(frg.getId()) != null) {
				fragmentDao.delete(frg);
			}
		}
	}

	protected Tag newTag(String name) {
		if (name == null) {
			name = "new tag " + temporalTags.size();
		}
		else {
			name = name.trim();
		}
		Tag result = new Tag(name);
		assertNotNull(result);
		temporalTags.add(result);
		return result;
	}

	protected Fragment newFragment() {
		Fragment frg = new Fragment(
		        "new fragment " + temporalFragments.size(),
				"Some content...", null);
		assertNotNull(frg);
		temporalFragments.add(frg);
		return frg;
	}

	protected Fragment newFragment(String title, String content) {
		Fragment frg = new Fragment(title, content, null);
		assertNotNull(frg);
		temporalFragments.add(frg);
		return frg;
	}

	protected Long getAndValidateId(Fragment f) {
		Long id = f.getId();
		assertTrue(id != null && id >= 0);
		return id;
	}

	protected Long getAndValidateId(Tag f) {
		Long id = f.getId();
		assertTrue(id != null);
		return id;
	}

	protected void assertEquality(Fragment expected, Fragment actual) {
		assertEquals(expected, actual);
		assertEquals(expected.getTitle(), actual.getTitle());
	}

	protected void assertEquality(Tag expected, Tag actual) {
		assertEquals(expected, actual);
		assertEquals(expected.getTagName(), actual.getTagName());
	}

	protected void setUp() throws Exception {
		fragmentDao = ctx.getBean("fragmentDao", FragmentDao.class);
		assertNotNull(fragmentDao);

		tagDao = ctx.getBean("tagDao", TagDao.class);
		assertNotNull(tagDao);
		
		fileEntityDao = ctx.getBean("fileEntityDao", FileEntityDao.class);
		assertNotNull(fileEntityDao);
		
		logger.info("all DAOs initialized OK");
	}

	protected void tearDown() throws Exception {
		deleteAllTemporalObjects();
	}
	
    protected void testExecuteArbitraryQuery() {
	    List<?> result0, result1;
	    
	    result0 = fragmentDao.executeQueryForResult("from Fragment");
	    result1 = fragmentDao.findAll(true);
	    assertEquals(result0, result1);

	    result0 = fragmentDao.executeQueryForResult("from Tag");
	    result1 = tagDao.findAll();
	    assertEquals(result0, result1);
	}

	protected void testFindAllTags() {
		Collection<Tag> tags = tagDao.findAllWithChildren(true);
		TestUtil.checkIfNoDuplicateExistsIn(tags);

		for (Tag t : tags) {
			Long id = getAndValidateId(t);
			Tag tag = tagDao.findById(id);
			assertEquality(tag, t);
			assertFalse(Hibernate.isInitialized(tag.getChildren()));
			assertFalse(Hibernate.isInitialized(tag.getFragments()));
		}
	}
	
	protected void testCountAll() {
	    final boolean includeTrashedOrNot[] = { true, false };
	    for (boolean b : includeTrashedOrNot) {
			final Collection<Fragment> fragments = fragmentDao.findAll(b);
			final long c = fragmentDao.countAll(b);
			assertEquals(fragments.size(), c);
		}
		{
			final Collection<Tag> tags = tagDao.findAll();
			final long c = tagDao.countAll();
			assertEquals(tags.size(), c);
		}
	}
	
	protected void testCountFragmentsPerTag() {
	    final List<Tag> tags = tagDao.findAll();
	    final boolean includeTrashedOrNot[] = { true, false };
	    for (Tag tag : tags) {
	        for (boolean b : includeTrashedOrNot) {
                final List<Fragment> fragments = fragmentDao.findByTagId(tag.getId(), b);
                final long fc = fragmentDao.countByTagId(tag.getId(), b);
                assertEquals(fc, fragments.size());
            }
        }
	}

	protected void testFindAllFragments() {
		Collection<Fragment> fragments = fragmentDao.findAll(TestUtil.getRandom().nextBoolean());

		for (Fragment f : fragments) {
			Long id = getAndValidateId(f);
			boolean withTags = false, withRelatedOnes = false;
			Fragment frg = fragmentDao.findById(id, withTags, withRelatedOnes);
			assertEquality(frg, f);
			assertFalse(Hibernate.isInitialized(frg.getTags()));
			assertFalse(Hibernate.isInitialized(frg.getRelatedOnes()));
			withTags = true;
			frg = fragmentDao.findById(id, withTags, withRelatedOnes);
			assertTrue(Hibernate.isInitialized(frg.getTags()));
			for (Tag t : frg.getTags()) {				
				assertTrue(Hibernate.isInitialized(t));
			}
			withRelatedOnes = true;
			frg = fragmentDao.findById(id, withTags, withRelatedOnes);
			assertTrue(Hibernate.isInitialized(frg.getRelatedOnes()));
			for (Fragment r : frg.getRelatedOnes()) {				
				assertTrue(Hibernate.isInitialized(r));
			}
		}
	}

	protected void testTagToFragmentRelationship() {
		Collection<Tag> tags = tagDao.findAll();
		Collection<Fragment> fragments = fragmentDao.findAll(true);
		Collection<String> fragmentNames = Fragment.getFragmentTitleCollectionFrom(fragments);

		for (Tag t : tags) {
		    Long id = getAndValidateId(t);
			Tag tag = tagDao.findById(id, true, false);
			Collection<Fragment> frgs = tag.getFragments();
			assertTrue(Hibernate.isInitialized(frgs));
			for (Fragment f : frgs) {
				assertTrue(fragmentNames.contains(f.getTitle()));
			}
		}
	}

	protected void testFragmentToTagRelationship() {
		Collection<Tag> tags = tagDao.findAll();
		assertFalse(tags.isEmpty());
		Collection<String> tagNames = Tag.getTagNameCollectionFrom(tags);
		Collection<Fragment> fragments = fragmentDao.findAll(true);

		for (Fragment f : fragments) {
			Long id = getAndValidateId(f);

			Fragment frgm = fragmentDao.findById(id, true, false);
			Collection<Tag> relatedTags = frgm.getTags();
			assertTrue(Hibernate.isInitialized(relatedTags));
			if (relatedTags.isEmpty() == false) {
				for (Tag t : relatedTags) {
					assertTrue(tagNames.contains(t.getTagName()));
				}
			}
		}
	}

	protected void testTagsHierarchy() {
		Collection<Tag> tags = tagDao.findAll();
		Collection<String> tagNames = Tag.getTagNameCollectionFrom(tags);

		for (Tag t : tags) {
			Long id = getAndValidateId(t);
			Tag tag = tagDao.findById(id, false, true);
			Collection<Tag> children = tag.getChildren();
			assertTrue(Hibernate.isInitialized(children));
			for (Tag c : children) {
				assertTrue(tagNames.contains(c.getTagName()));
			}
			
			final Set<Long> descendantIds = new HashSet<Long>();
			tagDao.findIdsOfAllDescendants(id, null, descendantIds);
			for (Long did : descendantIds) {
				final Tag descendant = tagDao.findById(did);
				final Set<Tag> ancestors = new HashSet<Tag>();
				findAllAncestorsOfTag(descendant, ancestors);
				assertTrue(ancestors.contains(tag));
			}
		}
	}
	
	private void findAllAncestorsOfTag(Tag tag, Set<Tag> idsInOut) {
		final List<Tag> parents = tagDao.findParentTags(tag.getId());
		idsInOut.addAll(parents);
		for (Tag t : parents) {
			findAllAncestorsOfTag(t, idsInOut);
		}
    }

	protected void testRelatedFragments() {
		Collection<Fragment> fragments = fragmentDao.findAll(true);
		Collection<String> fragmentNames = Fragment
				.getFragmentTitleCollectionFrom(fragments);

		for (Fragment f : fragments) {
			Long id = getAndValidateId(f);
			Fragment frgm = fragmentDao.findById(id, false, true);
			Collection<Fragment> relatedOnes = frgm.getRelatedOnes();
			assertTrue(Hibernate.isInitialized(relatedOnes));
			if (relatedOnes.isEmpty() == false) {
				for (Fragment r : relatedOnes) {
					assertTrue(fragmentNames.contains(r.getTitle()));
				}
			}
		}
	}

	protected void testPersistNewTag() {
		Tag tag = newTag(null);
		assertNull(tag.getId());
		tagDao.save(tag);
		assertNotNull(tag.getId());
	}

	protected void testPersistNewFragment() {
		final Fragment frg = newFragment();

		// Add new tag to this fragment
		final Tag tag = newTag("added tag " + counter++);
		frg.addTag(tag);

		assertNull(frg.getId());
		fragmentDao.save(frg);
		assertNotNull(frg.getId());

		// Check if the added tag has been persisted
		assertNotNull(tag.getId());
		assertNotNull(tagDao.findById(tag.getId()));

		final List<Fragment> fs = fragmentDao.findByTagId(tag.getId(), true);
		assertNotNull(fs);
		assertEquals(fs.size(), 1);
		final Fragment f = fs.get(0);
		assertEquality(f, frg);
	}

	protected void testUpdateTag() {
		for (int i = 0; i < 3; ++i) {
			testPersistNewTag();
		}

		for (int i = 1; i < temporalTags.size(); ++i) {
			Tag parent = temporalTags.get(i - 1);
			Tag child = temporalTags.get(i);
			parent.addChild(child);
		}
		for (Tag t : temporalTags) {
			tagDao.save(t);
			t.setChildren(null);
		}
		for (int i = 1; i < temporalTags.size(); ++i) {
			Tag parent = tagDao.findById(temporalTags.get(i - 1).getId(), false, true);
			Tag child = temporalTags.get(i);
			Collection<Tag> children = parent.getChildren();
			assertTrue(Hibernate.isInitialized(children));
			assertEquals(children.size(), 1);
			for (Tag c : children) {
				assertEquality(c, child);
				Collection<Tag> parents = tagDao.findParentTags(c.getId());
				assertEquals(parents.size(), 1);
				for (Tag p : parents) {
					assertEquality(p, parent);
				}
			}
		}
	}

	protected void testRelateFragments() {
		for (int i = 0; i < 6; ++i) {
			testPersistNewFragment();
		}
		for (int i = 1; i < temporalFragments.size(); ++i) {
			Fragment from = temporalFragments.get(i - 1);
			Fragment to = temporalFragments.get(i);
			fragmentDao.relateFragments(from.getId(), to.getId());
		}
//		for (int i = 1; i < temporalFragments.size(); ++i) {
//			Fragment from = temporalFragments.get(i - 1);
//			Fragment to = temporalFragments.get(i);
//			from.relateTo(to);
//		}
//		for (Fragment frg : temporalFragments) {
//			fragmentDao.save(frg);
//		}
		for (int i = 1; i < temporalFragments.size(); ++i) {
			Fragment from = fragmentDao
						.findById(temporalFragments.get(i - 1).getId(), false, true);
			Fragment to = fragmentDao
					.findById(temporalFragments.get(i).getId(), false, true);
			assertTrue(from.isRelatedTo(to));
			assertTrue(to.isRelatedTo(from));
		}
	}
	
	protected void testFindFragmentsByTagIds() {
		Collection<Tag> tags = tagDao.findAll();
		
		final int minTags = 4;
		if (tags.size() < minTags) {
			for (int i = 0; i < minTags; ++i) {
				testPersistNewTag();
			}
			tags = tagDao.findAll();
		}
		
		List<Long> ids = new ArrayList<Long>(tags.size());
		for (Tag t : tags) {
			ids.add(t.getId());
		}
		Collections.shuffle(ids);
		
		Collection<Fragment> fragments = null;
		List<Long> idsIn = new ArrayList<Long>();
		List<Long> idsEx = new ArrayList<Long>();
		idsEx.add(ids.get(0));
		idsEx.add(ids.get(1));
		
		// test edge cases
		fragments = fragmentDao.findByTagIds(null, null);
		assertNull(fragments);

		fragments = fragmentDao.findByTagIds(null, idsEx);
		assertNull(fragments);

		assertTrue(idsIn.isEmpty());
		fragments = fragmentDao.findByTagIds(idsIn, idsEx);
		assertNull(fragments);
		
		idsIn.add(ids.get(ids.size()-1));
		idsIn.add(ids.get(ids.size()-2));
		
		// The inclusive and exclusive filter should not have duplicate IDs!
		for (Long idIn : idsIn) {
			assertFalse(idsEx.contains(idIn));
		}
		
		// test with an inclusive filter only
		fragments = fragmentDao.findByTagIds(idsIn, null);
		for (Fragment f : fragments) {
			boolean contains = false;
			for (Long tid : idsIn) {
				if (f.containsTagId(tid)) {
					contains = true;
					break;
				}
			}
			assertTrue(contains);
		}
		
		// test with a duplicate inclusive filter
		idsIn.add(idsIn.get(0));
		Collection<Fragment> fragments2 = fragmentDao.findByTagIds(idsIn, null);
		assertEquals(fragments, fragments2);
		
		// test with an exclusive filter
		fragments = fragmentDao.findByTagIds(idsIn, idsEx);
		for (Fragment f : fragments) {
			boolean contains = false;
			for (Long tid : idsEx) {
				if (f.containsTagId(tid)) {
					contains = true;
					break;
				}
			}
			assertFalse(contains);
		}
		
		// test with a duplicate exclusive filter
		idsEx.add(idsEx.get(0));
		fragments2 = fragmentDao.findByTagIds(idsIn, idsEx);
		assertEquals(fragments, fragments2);
	}

	protected void testPagingFragments() {
	    Collection<Fragment> allFragments = fragmentDao.findAll(false);
	    int allCount = allFragments.size();
	    int first = 0, count = 0;
	    Collection<Fragment> someFragments = null;
	    final FragmentOrder fo = FragmentOrder.CREATION_DATETIME;
	    final boolean asc = false;
	    
	    // test edge cases
	    first = 0; count = 0; // zero range
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertTrue(someFragments.isEmpty());
	    first = -1; count = 1; // first index is negative
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertEquals(someFragments.size(), count);
	    first = -1; count = -1; // first index and count are negative
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertTrue(someFragments.isEmpty());
	    first = 1; count = -1; // count is negative
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertTrue(someFragments.isEmpty());
	    first = 0; count = allCount + 100; // count exceeds the max. available count
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertEquals(someFragments.size(), allCount);
	    first = allCount + 10; count = 1; // first index exceeds the max. available count
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertTrue(someFragments.isEmpty());
	    
	    // test normal cases
	    count = Math.max(1, TestUtil.getRandom().nextInt(allCount));
	    assertTrue(1 <= count && count < allCount);
	    first = Math.max(0, TestUtil.getRandom().nextInt(count));
	    assertTrue(0 <= first && first < count);
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertEquals(someFragments.size(), Math.min(count, allCount-first));
	    count = Math.max(1, TestUtil.getRandom().nextInt(allCount));
	    assertTrue(1 <= count && count < allCount);
	    first = Math.max(0, TestUtil.getRandom().nextInt(count));
	    assertTrue(0 <= first && first < count);
	    someFragments = fragmentDao.findSomeNonTrashed(first, count, fo, asc);
	    assertEquals(someFragments.size(), Math.min(count, allCount-first));
	    for (Fragment f : someFragments) {
	        assertTrue(allFragments.contains(f));
	    }
	}
	
	@SuppressWarnings("unchecked")
	protected void testPagingFragmentsWithOrder() {
		final Collection<Fragment> allFragments = fragmentDao.findAll(false);
		final int allCount = allFragments.size();
	    int first = 0, count = 0;
	    List<Fragment> someFragments = null;
	    
	    FragmentOrder orders[] = {
	    		FragmentOrder.UPDATE_DATETIME,
	    		FragmentOrder.CREATION_DATETIME,
	    		FragmentOrder.TITLE,
	    		FragmentOrder.ID,
	    };
	    
	    boolean asc[] = {
	    		false,
	    		false,
	    		true,
	    		false,
	    };
	    
	    Object comparators[] = new Object[orders.length];
	    for (int i=0; i<orders.length; ++i) {
	    	comparators[i] = FragmentOrder.getComparator(orders[i], asc[i]);
	    }
	    
	    for (int i=0; i<orders.length; ++i) {
	    	FragmentOrder order = orders[i];
			count = Math.max(1, TestUtil.getRandom().nextInt(allCount));
		    assertTrue(1 <= count && count < allCount);
		    first = Math.max(0, TestUtil.getRandom().nextInt(count));
		    assertTrue(0 <= first && first < count);
		    someFragments = fragmentDao.findSomeNonTrashed(first, count, order, asc[i]);
		    assertEquals(someFragments.size(), Math.min(count, allCount-first));
		    Comparator<Fragment> cmptr = (Comparator<Fragment>) comparators[i];
		    for (int j=1; j<someFragments.size(); ++j) {
		    	Fragment f0 = someFragments.get(j - 1);
	            Fragment f1 = someFragments.get(j);
	            final int r = cmptr.compare(f0, f1);
	            assertTrue(r <= 0);
		    }
		}
	}
	
	protected void testExplicitQuery() {
		testPersistNewFragment();
		testPersistNewFragment();
		final long id0 = temporalFragments.get(0).getId();
		final long id1 = temporalFragments.get(1).getId();
		final String sqlQuery = "insert into fragment2fragment(from_id, to_id) values ("+ id0 + ", "+ id1+ ")";
		fragmentDao.executeQuery(sqlQuery, true);
		final Fragment from = fragmentDao.findById(temporalFragments.get(0).getId(), false, true);
		final Fragment to = fragmentDao.findById(temporalFragments.get(1).getId(), false, true);
		assertTrue(from.isRelatedTo(to));
	}
	
	protected void testFileEntityQuery() {
		TestUtil.configure();
		final String filesHome = TestUtil.getFilesHomePath();
		
		List<FileEntity> fileEntitiesFromDB = fileEntityDao.findAll();
		assertNotNull(fileEntitiesFromDB);
		assertEquals(fileEntitiesFromDB.size(), fileEntityDao.countAll());
		
		for (FileEntity fe : fileEntitiesFromDB) {
			final File f = fe.toFile(filesHome);
			try {
				FileUtils.touch(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			assertEquals(true, f.isFile());
		}
		
		Collection<FileEntity> fileEntities = FileEntity.getFilesUnder(filesHome);
		assertNotNull(fileEntities);
		assertEquals(fileEntities.size(), fileEntitiesFromDB.size());
		
		for (FileEntity fe : fileEntities) {
//			System.out.println(fe);
			assertEquals(true, fileEntitiesFromDB.contains(fe));
			final FileEntity fe2 = fileEntityDao.findByName(fe.getFileName());
			assertNotNull(fe2);
			assertEquals(true, fileEntitiesFromDB.contains(fe2));
		}
		
		TestUtil.unconfigure();
	}
	
	protected void testFileEntityPersistence() {
		TestUtil.configure();
		final String filesHome = TestUtil.getFilesHomePath();
		
		final FileEntity fe = new FileEntity("~~~~temp~new~file~~~~");
		final File f = fe.toFile(filesHome);
		assertNotNull(f);
		if (f.isFile()) {
			assertTrue(f.delete());
		}
		assertEquals(false, fe.persisted(filesHome));
		
		assertEquals(fe, fileEntityDao.save(fe));
		assertNotNull(fe.getId());
		assertEquals(fe, fileEntityDao.findById(fe.getId()));
		
		try {
			assertTrue(f.createNewFile());
		} catch (IOException e) {
			e.printStackTrace();
		}		
		assertEquals(true, fe.persisted(filesHome));
		
		fileEntityDao.delete(fe);
		assertNull(fe.getId());
		assertEquals(false, fe.persisted(filesHome));
		
		TestUtil.unconfigure();
	}

}
