package fr.skytasul.music;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.xxmicloxx.NoteBlockAPI.model.Song;

import fr.skytasul.music.utils.Database;
import fr.skytasul.music.utils.Database.JBStatement;

public class JukeBoxDatas {

	private static final String DB_TABLE = "`jukebox_players`";
	
	private Map<UUID, PlayerData> players = new HashMap<>();
	
	private Database db;
	private JBStatement getStatement;
	private JBStatement insertStatement;
	private JBStatement updateStatement;
	
	public JukeBoxDatas(List<Map<?, ?>> mapList, Map<String, Song> tmpSongs) {
		for (Map<?, ?> m : mapList) {
			PlayerData pdata = PlayerData.deserialize((Map<String, Object>) m, tmpSongs);
			players.put(pdata.getID(), pdata);
		}
	}
	
	public JukeBoxDatas(Database db) throws SQLException {
		this.db = db;
		db.getConnection().createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS " + DB_TABLE + "("
				+ "`player_uuid` VARCHAR(32) NOT NULL"
				+ "`join` BOOLEAN NOT NULL"
				+ "`shuffle` BOOLEAN NOT NULL"
				+ "`particles` BOOLEAN NOT NULL"
				+ "`repeat` BOOLEAN NOT NULL"
				+ "`volume` VARINT(3) NOT NULL"
				+ "PRIMARY KEY (`player_uuid`)"
				+ ")");
		getStatement = db.new JBStatement("SELECT * FROM " + DB_TABLE + " WHERE `player_uuid` = ?");
		insertStatement = db.new JBStatement("INSERT INTO " + DB_TABLE + " (`join`, `shuffle`, `volume`, `particles`, `repeat`, `player_uuid`) VALUES (?, ?, ?, ?, ?, ?)");
		updateStatement = db.new JBStatement("UPDATE " + DB_TABLE + " SET `join` = ?, `shuffle`= ?, `volume` = ?, `particles` = ?, `repeat` = ? WHERE `player_uuid` = ?");
	}
	
	public PlayerData getDatas(UUID uuid) {
		return players.get(uuid);
	}
	
	public PlayerData getDatas(Player p) {
		return players.get(p.getUniqueId());
	}
	
	public Object getSerializedList() {
		List<Map<String, Object>> list = new ArrayList<>();
		for (PlayerData pdata : players.values()) {
			if (pdata.songPlayer != null) pdata.stopPlaying(true);
			if (!pdata.isDefault(JukeBox.defaultPlayer)) list.add(pdata.serialize());
		}
		return list;
	}
	
	public void joins(Player p) {
		UUID id = p.getUniqueId();
		if (db == null) {
			PlayerData pdata = players.get(id);
			if (pdata == null) {
				pdata = PlayerData.create(id);
				players.put(id, pdata);
			}
			pdata.playerJoin(p, !JukeBox.worlds || JukeBox.worldsEnabled.contains(p.getWorld().getName()));
		}else {
			Bukkit.getScheduler().runTaskAsynchronously(JukeBox.getInstance(), () -> {
				synchronized (getStatement) {
					PlayerData pdata = null;
					try {
						PreparedStatement statement = getStatement.getStatement();
						statement.setString(1, id.toString().replace("-", ""));
						ResultSet resultSet = statement.executeQuery();
						if (resultSet.next()) {
							pdata = new PlayerData(id);
							pdata.setJoinMusic(resultSet.getBoolean("join"));
							pdata.setShuffle(resultSet.getBoolean("shuffle"));
							pdata.setParticles(resultSet.getBoolean("particles"));
							pdata.setRepeat(resultSet.getBoolean("repeat"));
							pdata.setVolume(resultSet.getInt("volume"));
						}
						resultSet.close();
					}catch (SQLException e) {
						e.printStackTrace();
					}
					if (pdata == null) pdata = PlayerData.create(id);
					players.put(id, pdata);
					pdata.playerJoin(p, !JukeBox.worlds || JukeBox.worldsEnabled.contains(p.getWorld().getName()));
				}
			});
		}
	}
	
	public void quits(Player p) {
		UUID id = p.getUniqueId();
		PlayerData pdata = players.get(id);
		if (pdata != null) {
			pdata.playerLeave();
			if (db == null) {
				if (!JukeBox.savePlayerDatas) players.remove(id);
			}else {
				Bukkit.getScheduler().runTaskAsynchronously(JukeBox.getInstance(), () -> {
					synchronized (updateStatement) {
						try {
							int i = 1;
							PreparedStatement statement = pdata.created ? insertStatement.getStatement() : updateStatement.getStatement();
							statement.setBoolean(i++, pdata.hasJoinMusic());
							statement.setBoolean(i++, pdata.isShuffle());
							statement.setBoolean(i++, pdata.hasParticles());
							statement.setBoolean(i++, pdata.isRepeatEnabled());
							statement.setInt(i++, pdata.getVolume());
							statement.setString(i++, id.toString().replace("-", ""));
							statement.executeUpdate();
						}catch (SQLException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
	}
	
}
