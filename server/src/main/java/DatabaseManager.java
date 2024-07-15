import java.sql.*;
import com.zaxxer.hikari.*;

public class DatabaseManager implements Runnable {
    private static final String DATABASE_URL = "jdbc:sqlite:src/main/resources/battleship.db";
    private HikariDataSource ds;

    DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DATABASE_URL);
        ds = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return DriverManager.getConnection("jdbc:sqlite:src/main/resources/battleship.db");
    }

    @Override
    public void run() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS Users " +
                    "(username VARCHAR(255) PRIMARY KEY NOT NULL, " +
                    " wins REAL DEFAULT 0.0, " +
                    " losses REAL DEFAULT 0.0, " +
                    " elo_ranking REAL DEFAULT 1300.0)";
            stmt.execute(sql);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public User getUser(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt  = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs  = pstmt.executeQuery();

            if (rs.next()) {
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                double elo_ranking = rs.getDouble("elo_ranking");
                return new User(username, wins, losses, elo_ranking);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public void addUser(String username) {
        String checkSql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        String insertSql = "INSERT INTO Users(username) VALUES(?)";

        try (Connection conn = getConnection();
             PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {

            checkPstmt.setString(1, username);
            ResultSet rs = checkPstmt.executeQuery();

            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setString(1, username);
                    insertPstmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void shutdown() {
        if (ds != null) {
            ds.close();
        }
        // TODO: Comment out in final implementation
//        String sql = "DROP TABLE IF EXISTS Users";
//
//        try (Connection conn = getConnection();
//             Statement stmt = conn.createStatement()) {
//            System.out.println("Dropping users table...");
//            stmt.execute(sql);
//
//        } catch (SQLException e) {
//            System.out.println(e.getMessage());
//        }
    }

    public void incrementWins(String username, double new_ranking) {
        String sql = "UPDATE Users SET wins = wins + 1, elo_ranking = ? WHERE username = ?";
        try (Connection conn = this.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, new_ranking);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void incrementLosses(String username, double new_ranking) {
        String sql = "UPDATE Users SET losses = losses + 1, elo_ranking = ? WHERE username = ?";
        try (Connection conn = this.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, new_ranking);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}