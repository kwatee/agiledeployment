/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.ArrayList;
import java.util.Collection;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.repository.entity.User;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author mac
 * 
 */
@Repository
public class UserRepository {

	static private UserRepository instance;

	@Autowired
	private PersistenceService persistenceService;

	public UserRepository() {
		instance = this;
	}

	/**
	 * List of kwatee users
	 * 
	 * @return List of kwatee users
	 */
	public Collection<User> getUsers(boolean includeDisabled) {
		Collection<User> users = this.persistenceService.getUsers();
		if (!includeDisabled) {
			users = new ArrayList<>(users);
			CollectionUtils.filter(users,
					new Predicate() {

						public boolean evaluate(Object u) {
							return !((User) u).isDisabled();
						}
					});
		}
		return users;
	}

	/**
	 * Retrieves a user by its id or null if not found
	 * 
	 * @param userId
	 *            id of the user
	 */
	public User getUncheckedUser(long userId) {
		return this.persistenceService.getUserById(userId);
	}

	/**
	 * Retrieves a user by its id and throws an exception if not found
	 * 
	 * @param userId
	 *            id of the user
	 * @return The kwatee user
	 * @throws ObjectNotExistException
	 */
	public User getCheckedUser(long userId) throws ObjectNotExistException {
		User user = this.persistenceService.getUserById(userId);
		if (user == null) {
			throw new ObjectNotExistException(ObjectNotExistException.USER, userId);
		}
		return user;
	}

	/**
	 * Retrieves a user by its name or null if not found
	 * 
	 * @param userName
	 *            The login of the user
	 * @return The kwatee user or null if not found
	 */
	public User getUncheckedUser(String userName) {
		return this.persistenceService.getUserByName(userName);
	}

	/**
	 * Retrieves a user by its name and throws an exception if not found
	 * 
	 * @param userName
	 *            The login of the user
	 * @return The kwatee user
	 * @throws ObjectNotExistException
	 */
	public User getCheckedUser(String userName) throws ObjectNotExistException {
		User user = this.persistenceService.getUserByName(userName);
		if (user == null) {
			throw new ObjectNotExistException(ObjectNotExistException.USER, userName);
		}
		return user;
	}

	/**
	 * Saves or updates the user in the database
	 * 
	 * @param user
	 *            Kwatee user
	 */
	public void saveUser(User user) {
		this.persistenceService.saveEntity(user);
	}

	/**
	 * Deletes the user
	 * 
	 * @param user
	 *            Kwatee user
	 * @throws CannotDeleteObjectInUseException
	 */
	public void deleteUser(User user) throws CannotDeleteObjectInUseException {
		if (user.getLogin().equalsIgnoreCase(Constants.ADMIN_USER)) {
			throw new CannotDeleteObjectInUseException("admin", "kwatee");
		}
		this.persistenceService.deleteEntity(user);
	}

	static public UserRepository getInstance() {
		return instance;
	}

}