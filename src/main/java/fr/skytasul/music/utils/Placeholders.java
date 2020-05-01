package fr.skytasul.music.utils;

import org.bukkit.entity.Player;

import fr.skytasul.music.JukeBox;
import fr.skytasul.music.PlayerData;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;

public class Placeholders {

	public static void registerPlaceholders(){
		if (PlaceholderAPI.unregisterPlaceholderHook("jukebox")) JukeBox.getInstance().getLogger().warning("Previous PlaceholderHook with identifier \"jukebox\" unregistered");
		PlaceholderAPI.registerPlaceholderHook("jukebox", new PlaceholderHook() {
			@Override
			public String onPlaceholderRequest(Player p, String params){
				PlayerData pdata = PlayerData.players.get(p.getUniqueId());
				if (pdata == null) return "§c§lunknown player data";
				if (params.startsWith("playeroptions_")){
					switch (params.substring(params.indexOf("_") + 1)){
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
				}else if (params.equals("active")){
					if (pdata.songPlayer == null){
						if (pdata.getPlaylistType() == Playlists.RADIO) return JukeBox.getSongName(JukeBox.radio.getSong());
						return Lang.NONE;
					}
					return JukeBox.getSongName(pdata.songPlayer.getSong());
				}else if (params.equals("playlist")){
					return pdata.getPlaylistType().name;
				}
				return null;
			}
		});
		JukeBox.getInstance().getLogger().info("Placeholders registered");
	}
	
}
