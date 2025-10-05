package repositories;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    default String fieldsOf(Map<String, Object> data) {
        return "(" + String.join(" ,", data.keySet()) + ")";
    }

    default String bindingTemplateOf(Map<String, Object> data) {
        return "(" + String.join(" ,", Collections.nCopies(data.size(), "?")) + ")";
    }

    default String setClauseOf(Map<String, Object> data) {
        return data.keySet().stream()
                .map(field -> field + " = ?")
                .collect(Collectors.joining(", "));
    }

    /**
     * Filter data to create or update certain entity
     * 
     * @param data
     * @return
     * @throws Exception
     */
    default Map<String, Object> filterToCOU(Map<String, Object> data) throws Exception {
        if (data.containsKey("id"))
            data.remove("id");

        if (data.isEmpty())
            throw new Exception("data is empty");

        return data;
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
    void update(T entity, Map<String, Object> fieldsToUpdate);

    /**
     * Delete an entity from the repository.
     * 
     * @param entity
     */
    void deleteById(String id);

}
