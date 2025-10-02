package repositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RepositoryBase<T> {

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
    T updateById(String id, Map<String, Object> fieldsToUpdate);

    /**
     * Delete an entity from the repository.
     * 
     * @param entity
     */
    void deleteById(String id);
}
