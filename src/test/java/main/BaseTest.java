package main;

import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import main.models.Author;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Main.class })
public abstract class BaseTest {
	
	@Autowired
	protected SessionFactory sessionFactory;
	
	@Before
	public void clearTable() {
		Session session = sessionFactory.openSession();
		org.hibernate.Transaction tx = session.beginTransaction();

		session.createNativeQuery("delete from book_authorship").executeUpdate();
		session.createNativeQuery("delete from author_award").executeUpdate();
		session.createNativeQuery("delete from author").executeUpdate();
		session.createNativeQuery("delete from book").executeUpdate();
		tx.commit();
		session.close();
	}
	
	protected Session getSession() {
		return sessionFactory.openSession();
	}
	
	protected List<Author> getAuthorsByName(String name, Session session) {
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Author> q = cb.createQuery(Author.class);
		Root<Author> c = q.from(Author.class);
		q.where(cb.equal(c.<String>get("name"), name));
		TypedQuery<Author> authorQuery = session.createQuery(q);
		return authorQuery.getResultList();
	}

}
