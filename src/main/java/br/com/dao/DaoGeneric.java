package br.com.dao;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import br.com.jpautil.JPAUtil;

@Named // Onde tiver o Named, eu vou ter que implementar o Serializable
public class DaoGeneric<E> implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager entityManager; // Aqui deixamos um escopo global
	
	@Inject
	private JPAUtil jpaUtil;

	public void salvar(E entidade) {
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		entityManager.persist(entidade);
		// aqui tinha um close, mas tiramos para que o framework controle quando fechar
	}

	public E merge(E entidade) {
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		E retorno = entityManager.merge(entidade);

		entityTransaction.commit();
		return retorno;
	}

	public void delete(E entidade) {
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		entityManager.remove(entidade);

		entityTransaction.commit();
	}

	public void deletePorId(E entidade) {
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		Object id = jpaUtil.getPrimaryKey(entidade); // Esse getClass abaixo deixa genérico
		entityManager.createQuery("delete from " + entidade.getClass().getCanonicalName() + " where id = " + id)
				.executeUpdate();

		entityTransaction.commit();
	}

	public List<E> getListEntity(Class<E> entidade) {
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		List<E> retorno = entityManager.createQuery("from " + entidade.getName()).getResultList();

		entityTransaction.commit();

		return retorno;
	}

	public E consultar(Class<E> entidade, String codigo) {
		EntityManager entityManager = jpaUtil.getEntityManager();
		EntityTransaction entityTransaction = entityManager.getTransaction();
		entityTransaction.begin();

		E objeto = (E) entityManager.find(entidade, Long.parseLong(codigo));
		entityTransaction.commit();
		return objeto;
	}
	
}
