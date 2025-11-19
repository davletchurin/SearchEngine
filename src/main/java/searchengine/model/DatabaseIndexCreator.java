package searchengine.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Компонент для автоматического создания индексов в базе данных при запуске приложения.
 *
 * <p>Этот компонент решает проблему создания индексов для полей типа TEXT в Hibernate,
 * где стандартные аннотации JPA не позволяют указать длину индекса для TEXT полей.</p>
 *
 * <p>Компонент выполняет проверку существования индексов и создает их только при необходимости,
 * что позволяет избежать ошибок при повторном запуске приложения.</p>
 *
 * @see Component
 * @see PostConstruct
 * @see Transactional
 */
@Component
public class DatabaseIndexCreator {

/**
 * Менеджер сущностей для работы с JPA.
 * Используется для выполнения операций с базой данных в контексте JPA.
 */
    @PersistenceContext
    private EntityManager entityManager;

/**
 * Источник данных для получения соединения с базой данных.
 * Используется для выполнения нативных SQL-запросов.
 */
    @Autowired
    private DataSource dataSource;

/**
 * Создает необходимые индексы в базе данных после инициализации компонента.
 *
 * <p>Метод выполняется автоматически после создания бина и внедрения зависимостей.
 * Проверяет существование индексов и создает их только если они отсутствуют.</p>
 *
 * <p>В случае ошибки при создании индексов метод логирует сообщение об ошибке,
 * но не прерывает работу приложения, позволяя ему продолжить работу без индексов.</p>
 *
 * @throws SQLException если возникает ошибка при работе с базой данных
 *
 * @see PostConstruct
 * @see Transactional
 */
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

/**
 * Проверяет существование индекса с указанным именем в базе данных.
 *
 * <p>Метод выполняет запрос к системной таблице information_schema.statistics
 * для проверки существования индекса по имени индекса и имени таблицы.</p>
 *
 * @param connection активное соединение с базой данных
 * @param indexName имя индекса для проверки
 * @return true если индекс существует, false в противном случае
 * @throws SQLException если возникает ошибка при выполнении запроса
 */
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