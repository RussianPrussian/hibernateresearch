package main;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import main.models.Author;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Main.class })
public class SavePersistTest extends BaseTest {
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private EntityManagerFactory factory;
	
	@Test
	public void tryEntityManagerFactory() {
		EntityManager em = factory.createEntityManager();
		Author authorSaved = new Author();
		authorSaved.setName("AlexSaved");
		em.persist(authorSaved);
		em.close();
	}
	
	@Test
	public void savesOccurOutsideTransactionButPersistsDoNot() {
		Session session = sessionFactory.openSession();
		
		//save
		Author authorSaved = new Author();
		authorSaved.setName("AlexSaved");
		Long id = (Long) session.save(authorSaved);
		Author savedVal = session.load(Author.class, id);
		session.close();
		assertThat(savedVal.getName()).isEqualTo("AlexSaved");
		
		//persist
		session = sessionFactory.openSession();
		Author authorPersisted = new Author();
		authorPersisted.setName("AlexPersisted");
		session.persist(authorPersisted);
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Author> q = cb.createQuery(Author.class);
		Root<Author> c = q.from(Author.class);
		q.where(cb.equal(c.<String>get("name"), "AlexPersisted"));
		TypedQuery<Author> authorQuery = session.createQuery(q);
		assertThat(authorQuery.getResultList()).isEmpty();
		
		//but if I do start a transaction, it will be there.
		Transaction tx = session.beginTransaction();
		List<Author> authors = session.createQuery(q).getResultList();
		assertThat(authors.get(0).getName()).isEqualTo("AlexPersisted");
		tx.commit();
		session.close();
	}
	
	@Test
	public void saveThenEvictThenCommit() {
		Author alex = new Author();
		alex.setName("alexyeah");
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		Long id = (Long) session.save(alex);
		session.evict(alex);// only affects cache.
		tx.commit();
		tx.begin();
		List<Author> authors = getAuthorsByName("alexyeah", session);
		assertThat(authors.isEmpty()).isFalse();
		tx.commit();
	}
}
