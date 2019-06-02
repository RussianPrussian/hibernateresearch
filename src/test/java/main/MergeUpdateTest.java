package main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;

import main.models.Author;
import main.models.Book;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Main.class })
public class MergeUpdateTest extends BaseTest {
	
	@Test
	public void updateResultsInNonUniqueException() {
		//set up data
		Author alex = new Author();
		alex.setName("Alex");
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		Long id = (Long) session.save(alex);
		tx.commit();//we're saved.
		
		//create a copy
		Author alexCopy = new Author();
		alexCopy.setId(alex.getId());
		alexCopy.setName(alex.getName());
		tx.begin();
		alexCopy.setName("New Name");
		
		//we shouldn't be able to update it
		try {
			session.update(alexCopy);
			fail();
		} catch(NonUniqueObjectException e) {}
		
		//but merging works just fine.
		session.merge(alexCopy);
		tx.commit();
		tx.begin();
		assertThat(getAuthorsByName("Alex", session)).isEmpty();
		assertThat(getAuthorsByName("New Name", session).get(0).getName()).isEqualTo("New Name");
		tx.commit();
		session.close();
	}
	
	@Test
	public void updatesOnEntityPropertiesDuringSessionIsPersisted() {
		//set up data
		Author alex = new Author();
		alex.setName("Alex");
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		Long id = (Long) session.save(alex);
		tx.commit();
		tx.begin();
		alex.setName("Yes");
		tx.commit();
		
		//sanity check - life is intact. 
		assertThat(getAuthorsByName("Yes", session).get(0).getName()).isEqualTo("Yes");
		
		//The entity in the session gets updated here.
		alex.setName("No");
		//So when we query in a hibernate-session-aware way, we get some interesting results.
		Author obj = (Author) session.createNativeQuery("select * from author a where auth_name = 'Yes'").addEntity(Author.class).getSingleResult();
		assertThat(obj.getName()).isEqualTo("No");
		assertThat(getAuthorsByName("Yes", session).get(0).getName()).isEqualTo("No");
		assertThat(session.load(Author.class, alex.getId()).getName()).isEqualTo("No");
		
		//This becomes evident when you query the database. (this method is not hibernate-session-aware)
		Object [] obj2 = (Object []) session.createNativeQuery("select * from author a where auth_name = 'Yes'").getSingleResult();
		assertThat(obj2[1]).isEqualTo("Yes");

		//So let's evict the value from the cache.
		session.evict(alex); //not in cache anymore
		
		//The update was never committed, so we won't find it with the value "No"
		assertThat(getAuthorsByName("No", session)).isEmpty();
		assertThat(getAuthorsByName("Yes", session).get(0).getName()).isEqualTo("Yes"); //not in cache, so looks in database
		session.merge(alex); // now we'll update the value.
		tx.begin(); 
		tx.commit(); //changes to alex are finally persisted
		//we verify.
		assertThat(getAuthorsByName("Yes", session)).isEmpty();
		assertThat(getAuthorsByName("No", session).get(0).getName()).isEqualTo("No");
		session.close();
	}
	
	@Test
	public void callSessionRefreshToLoadMappedEntities() {
		//Set up the data.
		Session session = getSession();
		Author author = new Author();
		author.setName("CatsElbow");
		Book book = new Book();
		book.setTitle("Bonjour!");
		author.setBooks(Sets.newHashSet(book));
		Author secondAuthor = new Author();
		secondAuthor.setName("Frenchie");
		secondAuthor.setBooks(Sets.newHashSet(book));
		session.beginTransaction();
		session.saveOrUpdate(author);
		session.saveOrUpdate(secondAuthor);
		session.getTransaction().commit();
		
		//Note that authors becomes no null and is fully loaded
		assertThat(book.getAuthors()).isNull();
		session.refresh(book);
		assertThat(book.getAuthors()).contains(author, secondAuthor);
		
		session.close();
	}
	
	@Test
	public void updateSharedObjectWithClearedCacheSuccessful() {
		//Set up Data
		Session session = getSession();
		Author author = new Author();
		author.setName("CatsElbow");
		Book book = new Book();
		book.setTitle("Bonjour!");
		author.setBooks(Sets.newHashSet(book));
		Author secondAuthor = new Author();
		secondAuthor.setName("Frenchie");
		secondAuthor.setBooks(Sets.newHashSet(book));
		session.beginTransaction();
		session.saveOrUpdate(author);
		session.saveOrUpdate(secondAuthor);
		session.getTransaction().commit();
		
		session.clear();
		session.beginTransaction();
		author.setName("CatsElbowRevised");
		secondAuthor.setName("FrenchieRevised");
		//We will have no problems here because all of these have the same
		//instance of book
		session.saveOrUpdate(author);
		session.update(book);
		session.update(secondAuthor);
		session.getTransaction().commit();
	}
	
	@Test
	public void updateASharedObjectThatExistsInCacheGetException() {
		//Set up the data.
		Session session = getSession();
		Author author = new Author();
		author.setName("CatsElbow");
		Book book = new Book();
		book.setTitle("Bonjour!");
		author.setBooks(Sets.newHashSet(book));
		Author secondAuthor = new Author();
		secondAuthor.setName("Frenchie");
		secondAuthor.setBooks(Sets.newHashSet(book));
		session.beginTransaction();
		session.saveOrUpdate(author);
		session.saveOrUpdate(secondAuthor);
		session.getTransaction().commit();
		
		session.clear();
		
		Author retrievedCatsElbowAgain = getAuthorsByName("CatsElbow", session).get(0);
		retrievedCatsElbowAgain.getBooks().stream().collect(Collectors.toList()).get(0);
		try {
			session.beginTransaction();
			session.saveOrUpdate(book);
			session.getTransaction().commit();// this will fail.
			Assert.fail();
		}catch (NonUniqueObjectException e) {
			// hibernate loaded the book (notice how I manually forced the lazy load to 
			// complete by calling get Books); it can't save the book!
		}
		
		try {
			session.saveOrUpdate(secondAuthor);
			session.getTransaction().commit();
			Assert.fail();
		}catch (NonUniqueObjectException e) {
			//shouldn't be able to do this either, because secondAuthor still has the original instance of book
		}
		session.close();
	}
	
	@Test
	public void saveLoadedObjectInClearedSessionGeneratesNewOne() {//does persist do that too?
		
		//Generate a new author.
		Author author = new Author();
		author.setName("Jack Daniels");
		Session session = getSession();
		
		//Save it twice and get the ids.
		session.beginTransaction();
		Long firstId = (Long) session.save(author);
		session.getTransaction().commit();
		session.clear();
		session.beginTransaction();
		Long secondId = (Long) session.save(author);
		session.getTransaction().commit();
		
		//It was saved twice with two different ids!
		assertThat(firstId).isNotEqualTo(secondId);
		
		//Let's retrieve the authors by name. we should get two of them.
		List<Author> authors = getAuthorsByName("Jack Daniels", session);
		assertThat(authors).hasSize(2);
		Map<Long, Author> authorMap = authors.stream().collect(Collectors.toMap(Author::getId, Function.identity()));
		
		//Here is the strange thing. Hibernate will reuse the original author for the new id, and create a new cached entity
		//for the old id.
		assertThat(authorMap.get(secondId) == author).isTrue();
		assertThat(authorMap.get(firstId) == author).isFalse();
		
		//For science, let's do one more test and save the author one more time since. Let's
		//convince ourselves that when the session hasn't been cleared, we wont' be creating a new record.
		session.save(author);
		authors = getAuthorsByName("Jack Daniels", session);
		assertThat(authors).hasSize(2);
		
		session.close();
	}
	
	@Test
	public void persistAnAlreadySavedObjectInClearedOrNewSessionDoesNotGenerateNewOne() {
		//Generate a new author.
		Author author = new Author();
		author.setName("Jack Daniels");
		Session session = getSession();
		
		//Save it twice and get the ids.
		session.beginTransaction();
		session.persist(author);
		session.getTransaction().commit();
		session.clear();
		session.beginTransaction();
		try {
			session.persist(author);
			Assert.fail();
		} catch(PersistenceException e) {
			//Hibernate recognizes that the author has become detached!
		}
		session.getTransaction().commit();
		
		List<Author> authors = getAuthorsByName("Jack Daniels", session);
		assertThat(authors).hasSize(1);
		session.close();
	}
	
	@Test
	public void persistLoadedObjectAgainInDifferentSessionDoesNotGenerateNewOne() {
		//Generate a new author.
		Author author = new Author();
		author.setName("Jack Daniels");
		Session session = getSession();
		
		//Save it twice and get the ids.
		session.beginTransaction();
		session.persist(author);
		session.getTransaction().commit();
		session.close();
		
		//Let's try again with a fresh session!
		session = getSession();
		session.beginTransaction();
		try {
			session.persist(author);
			Assert.fail();
		} catch(PersistenceException e) {
			//Hibernate recognizes that the author has become detached!
		}
		session.getTransaction().commit();
		List<Author> authors = getAuthorsByName("Jack Daniels", session);
		assertThat(authors).hasSize(1);
		session.close();
	}
	
	@Test
	public void iWasToldJoinFetchedEntityAsFilterIsBadButIdontSeeThatHere() {
		Author a1 = new Author();
		a1.setName("Mr. Tickles");
		
		Book b1 = new Book();
		b1.setTitle("Storm of Swords");
		
		Book b2 = new Book();
		b2.setTitle("The Shrimp");
		a1.setBooks(Sets.newHashSet(b1, b2));
		
		Session session = getSession();
		session.beginTransaction();
		session.save(a1);
		session.getTransaction().commit();
		
		session.beginTransaction();
		Query<Author> query = session.createQuery("from Author as a join fetch a.books b where b.title = 'Storm of Swords'");
		List<Author> authors = query.list();
		Author a = authors.get(0);
		a.setName("some name");
		session.update(authors.get(0));
		session.getTransaction().commit();
		session.clear();
		
		session.beginTransaction();
		query = session.createQuery("from Author as a join fetch a.books b");
		authors = query.list();
		assertThat(authors.get(0).getBooks()).hasSize(2);
		session.close();
	}
}
