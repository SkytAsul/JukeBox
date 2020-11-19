package fr.skytasul.music.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.OfflinePlayer;

import com.xxmicloxx.NoteBlockAPI.model.Song;

import fr.skytasul.music.JukeBox;
import fr.skytasul.music.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class Placeholders extends PlaceholderExpansion {

	public static void registerPlaceholders(){
		new Placeholders().register();
		JukeBox.getInstance().getLogger().info("Placeholders registered");
	}
	
	private Placeholders() {}
	
	@Override
	public String getAuthor() {
		return JukeBox.getInstance().getDescription().getAuthors().toString();
	}
	
	@Override
	public String getIdentifier() {
		return "jukebox";
	}
	
	@Override
	public String getVersion() {
		return JukeBox.getInstance().getDescription().getVersion();
	}
	
	@Override
	public boolean canRegister() {
		return true;
	}
	
	@Override
	public List<String> getPlaceholders() {
		return Arrays.asList("playeroptions_volume", "playeroptions_shuffle", "playeroptions_join", "playeroptions_particles", "playeroptions_loop", "active", "active_title", "active_author", "active_original_author", "active_description", "playlist");
	}
	
	@Override
	public String onRequest(OfflinePlayer p, String params) {
		PlayerData pdata = JukeBox.getInstance().datas.getDatas(p.getUniqueId());
		if (pdata == null) return "§c§lunknown player data";
		if (params.startsWith("playeroptions_")) {
			switch (params.substring(params.indexOf("_") + 1)) {
			case "volume":
				return pdata.getVolume() + "%";
			case "shuffle":
				return pdata.isShuffle() ? Lang.ENABLED : Lang.DISABLED;
			case "join":
				return pdata.hasJoinMusic() ? Lang.ENABLED : Lang.DISABLED;
			case "particles":
				return pdata.hasParticles() ? Lang.ENABLED : Lang.DISABLED;
			case "loop":
				return pdata.isRepeatEnabled() ? Lang.ENABLED : Lang.DISABLED;
			default:
				return "§c§lunknown option";
			}
		}else if (params.startsWith("active")) {
			Song song;
			if (pdata.songPlayer == null) {
				if (pdata.getPlaylistType() == Playlists.RADIO) {
					song = JukeBox.radio.getSong();
				}else return Lang.NONE;
			}else song = pdata.songPlayer.getSong();
			if (params.equals("active_title")) return song.getTitle();
			if (params.equals("active_author")) return song.getAuthor();
			if (params.equals("active_original_author")) return song.getOriginalAuthor();
			if (params.equals("active_description")) return song.getDescription();
			return JukeBox.getSongName(song);
		}else if (params.equals("playlist")) {
			return pdata.getPlaylistType().name;
		}
		return null;
	}
	
}
