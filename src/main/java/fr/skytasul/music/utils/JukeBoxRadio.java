package fr.skytasul.music.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.xxmicloxx.NoteBlockAPI.event.SongNextEvent;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;

import fr.skytasul.music.JukeBox;

public class JukeBoxRadio implements Listener {

	private final CustomSongPlayer songPlayer;
	private List<Player> listening = new ArrayList<>();
	
	public JukeBoxRadio(Playlist songs){
		Bukkit.getPluginManager().registerEvents(this, JukeBox.getInstance());
		songPlayer = new CustomSongPlayer(songs);
		songPlayer.setRandom(true);
		songPlayer.setAutoDestroy(false);
		songPlayer.setRepeatMode(RepeatMode.ALL);
		songPlayer.setPlaying(true);
	}
	
	@EventHandler
	public void onSongNext(SongNextEvent e){
		if (e.getSongPlayer() == songPlayer) {
			for (UUID id : songPlayer.getPlayerUUIDs()){
				Player p = Bukkit.getPlayer(id);
				if (p != null) JukeBox.sendMessage(p, Lang.MUSIC_PLAYING + " " + JukeBox.getSongName(e.getSongPlayer().getSong()));
			}
		}
	}
	
	public Song getSong() {
		return songPlayer.getSong();
	}

	public void join(Player p){
		songPlayer.addPlayer(p);
		listening.add(p);
		JukeBox.sendMessage(p, Lang.MUSIC_PLAYING + " " + JukeBox.getSongName(songPlayer.getSong()));
	}
	
	public void leave(Player p){
		songPlayer.removePlayer(p);
		listening.remove(p);
	}

	public boolean isListening(Player p) {
		return listening.contains(p);
	}

	public void stop(){
		for (UUID id : songPlayer.getPlayerUUIDs()){
			Player p = Bukkit.getPlayer(id);
			if (p != null) JukeBox.sendMessage(p, Lang.MUSIC_STOPPED);
		}
		songPlayer.destroy();
	}
	
}
