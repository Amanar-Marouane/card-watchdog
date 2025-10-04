package repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import utils.Callback;
import utils.VoidCallback;

public interface RepositoryBase<T> {

    default <R> R pipeline(Callback<R> c) {
        try {
            return c.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default void pipeline(VoidCallback c) {
        try {
            c.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve all entities.
     * 
     * @return list of all entities
     */
    List<T> findAll();

    /**
     * Find an entity by a given key/value pair.
     * 
     * @param id the id to match
     * @return an Optional containing the entity if found, empty otherwise
     */
    Optional<T> findById(String id);

    /**
     * Save or update an entity in the repository.
     * 
     * @param entity the entity to save
     */
    T create(Map<String, Object> data);

    /**
     * Update specific fields of an entity in the repository.
     * 
     * @param entity
     * @param fieldsToUpdate
     */
    void updateById(T entity, Map<String, Object> fieldsToUpdate);

    /**
     * Delete an entity from the repository.
     * 
     * @param entity
     */
    void deleteById(String id);

}
