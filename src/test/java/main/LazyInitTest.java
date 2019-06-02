package main;

import java.util.Set;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import main.models.Author;
import main.models.Book;

public class LazyInitTest extends BaseTest {
	
	@Test
	public void causeALazyInitException() {
		Author author = new Author();
		author.setName("CatsElbow");
		Book book = new Book();
		book.setTitle("Bonjour!");
		author.setBooks(Sets.newHashSet(book));
		
		Session session = getSession();
		
		session.beginTransaction();
		session.save(author);
		session.getTransaction().commit();
		
		session.clear();
		
		Author author2 = (Author) session.createQuery("from Author a where a.name = 'CatsElbow'").list().get(0);
		
		session.close();
		try {
			Set<Book> books = author2.getBooks();
			books.stream().findFirst().get(); //this is the offending operation. God forbid we should try to access it outside of the session.
			//well..how could we if it were never loaded?
			Assert.fail();
		} catch(LazyInitializationException e) {
		}
		
	}
	

}
