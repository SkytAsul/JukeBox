package fr.skytasul.music.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.bukkit.configuration.ConfigurationSection;

import fr.skytasul.music.JukeBox;

public class Database {

	private Properties properties;
	private String host, database;
	private int port;

	private Connection connection;

	public Database(ConfigurationSection config) {
		this.host = config.getString("host");
		this.database = config.getString("database");
		this.port = config.getInt("port");

		properties = new Properties();
		properties.setProperty("user", config.getString("username"));
		properties.setProperty("password", config.getString("password"));
		if (!config.getBoolean("ssl")) {
			properties.setProperty("verifyServerCertificate", "false");
			properties.setProperty("useSSL", "false");
		}
		if(config.getBoolean("allowPublicKeyRetrieval")){
			properties.setProperty("allowPublicKeyRetrieval", "true");
		}
	}

	public String getDatabase() {
		return database;
	}

	public boolean openConnection() {
		if (!isClosed()) return false;

		try {
			Class.forName("com.mysql.jdbc.Driver");
		}catch (ClassNotFoundException e) {
			JukeBox.getInstance().getLogger().severe("Database driver not found.");
			return false;
		}

		try {
			connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.getDatabase(), properties);
		}catch (SQLException ex) {
			JukeBox.getInstance().getLogger().severe("An exception occurred when connecting to the database.");
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean isClosed() {
		try {
			return connection == null || connection.isClosed() || !connection.isValid(0);
		}catch (SQLException e) {
			e.printStackTrace();
			return true;
		}
	}

	public void closeConnection() {
		if (!isClosed()) {
			try {
				connection.close();
			}catch (SQLException ex) {
				JukeBox.getInstance().getLogger().severe("An exception occurred when closing database connection.");
				ex.printStackTrace();
			}
			connection = null;
		}
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}

	public PreparedStatement prepareStatementGeneratedKeys(String sql) throws SQLException {
		return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	public Connection getConnection() {
		return connection;
	}

	public class JBStatement {
		private final String statement;
		private boolean returnGeneratedKeys;

		private PreparedStatement prepared;

		public JBStatement(String statement) {
			this(statement, false);
		}

		public JBStatement(String statement, boolean returnGeneratedKeys) {
			this.statement = statement;
			this.returnGeneratedKeys = returnGeneratedKeys;
		}

		public PreparedStatement getStatement() throws SQLException {
			if (prepared == null || prepared.isClosed() || isClosed()) {
				openConnection();
				prepared = returnGeneratedKeys ? connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(statement);
			}
			return prepared;
		}

		public String getStatementCommand() {
			return statement;
		}
	}

}
