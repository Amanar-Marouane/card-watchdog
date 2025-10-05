package repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import utils.Callback;
import utils.VoidCallback;

public abstract class RepositoryBase {

    protected <R> R executeSafely(Callback<R> c) {
        try {
            return c.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void executeSafely(VoidCallback c) {
        try {
            c.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String fieldsOf(Map<String, Object> data) throws Exception {
        if (data == null || data.isEmpty())
            throw new Exception("No fields to continue");

        return "(" + String.join(", ", data.keySet()) + ")";
    }

    protected String bindingTemplateOf(Map<String, Object> data) throws Exception {
        if (data == null || data.isEmpty())
            throw new Exception("No fields to continue");
        return "(" + String.join(" ,", Collections.nCopies(data.size(), "?")) + ")";
    }

    protected String setClauseOf(Map<String, Object> data) throws Exception {
        if (data == null || data.isEmpty())
            throw new Exception("No fields to continue");
        return data.keySet().stream()
                .map(field -> field + " = ?")
                .collect(Collectors.joining(", "));
    }

    protected ResultSet executeQuery(Connection conn, String sql, Object... params) throws Exception {
        var stmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt.executeQuery();
    }

    protected int executeUpdate(Connection conn, String sql, Object... params) throws Exception {
        try (var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1); // return the generated ID
            }
            return -1; // no generated ID for update/delete
        }
    }

    protected Map<String, Object> filterID(Map<String, Object> data) throws Exception {
        Map<String, Object> mutableData = new HashMap<>(data);
        if (mutableData.containsKey("id"))
            mutableData.remove("id");
        return mutableData;
    }
}
