package searchengine.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.*;

@Component
public class DatabaseIndexCreator {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DataSource dataSource;
    @PostConstruct
    @Transactional
    public void createIndexes() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            boolean indexExists = checkIndexExists(connection, "idx_page_path");

            if (!indexExists) {
                statement.execute("CREATE INDEX idx_page_path ON page(path(768))");
            }

        } catch (SQLException e) {
            System.err.println("Could not create index: " + e.getMessage());
        }
    }
    private boolean checkIndexExists(Connection connection, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.statistics " +
                "WHERE table_name = 'page' AND index_name = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, indexName);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}