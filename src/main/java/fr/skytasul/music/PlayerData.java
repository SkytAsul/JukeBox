package fr.skytasul.music;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.event.SongDestroyingEvent;
import com.xxmicloxx.NoteBlockAPI.event.SongLoopEvent;
import com.xxmicloxx.NoteBlockAPI.event.SongNextEvent;
import com.xxmicloxx.NoteBlockAPI.model.FadeType;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;

import fr.skytasul.music.utils.CustomSongPlayer;
import fr.skytasul.music.utils.Lang;
import fr.skytasul.music.utils.Playlists;

public class PlayerData implements Listener{
	
	boolean created = false;
	
	private UUID id;
	private boolean join = false;
	private boolean shuffle = false;
	private int volume = 100;
	private boolean particles = true;
	private boolean repeat = false;

	private boolean favoritesRemoved = false;
	private Playlist favorites;
	private Playlists listening = Playlists.PLAYLIST;
	
	public CustomSongPlayer songPlayer;
	private Player p;
	
	private List<Integer> randomPlaylist = new ArrayList<>();
	JukeBoxInventory linked = null;

	PlayerData(UUID id) {
		this.id = id;
		Bukkit.getPluginManager().registerEvents(this, JukeBox.getInstance());
	}
	
	private PlayerData(UUID id, PlayerData defaults){
		this(id);
		this.created = true;
		setJoinMusic(defaults.hasJoinMusic());
		setShuffle(defaults.isShuffle());
		setVolume(defaults.getVolume());
		setParticles(defaults.hasParticles());
		setRepeat(defaults.isRepeatEnabled());
	}
	
	@EventHandler
	public void onSongDestroy(SongDestroyingEvent e) {
		if (e.getSongPlayer() == songPlayer) {
			if (linked != null) linked.playingStopped();
			songPlayer = null;
			if (favoritesRemoved){
				favoritesRemoved = false;
				if (listening == Playlists.FAVORITES && favorites != null){
					playList(favorites);
				}
			}
		}
	}
	
	@EventHandler
	public void onLoop(SongLoopEvent e){
		if (e.getSongPlayer() == songPlayer){
			if ((listening == Playlists.FAVORITES && favorites == null) || (listening == Playlists.PLAYLIST && !(shuffle || repeat))) {
				songPlayer.destroy();
				return;
			}
			playSong(true);
		}
	}
	
	@EventHandler
	public void onSongNext(SongNextEvent e){
		if (e.getSongPlayer() == songPlayer){
			if (listening == Playlists.PLAYLIST && !shuffle){
				stopPlaying(false);
			}else playSong(true);
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e){
		Player p = e.getPlayer();
		if (!p.getUniqueId().equals(id)) return;
		if (songPlayer != null) songPlayer.setPlaying(false);
		p = null;
	}

	public void playList(Playlist list){
		if (listening == Playlists.RADIO){
			JukeBox.sendMessage(getPlayer(), Lang.UNAVAILABLE_RADIO);
			return;
		}
		if (songPlayer != null) stopPlaying(false);
		if (list == null) return;
		
		songPlayer = new CustomSongPlayer(list);
		songPlayer.setParticlesEnabled(particles);
		songPlayer.getFadeIn().setFadeDuration(JukeBox.fadeInDuration);
		if (JukeBox.fadeInDuration != 0) songPlayer.getFadeIn().setType(FadeType.LINEAR);
		songPlayer.getFadeOut().setFadeDuration(JukeBox.fadeOutDuration);
		if (JukeBox.fadeOutDuration != 0) songPlayer.getFadeOut().setType(FadeType.LINEAR);
		songPlayer.setAutoDestroy(true);
		songPlayer.addPlayer(getPlayer());
		songPlayer.setPlaying(true);
		songPlayer.setRandom(shuffle);
		songPlayer.setRepeatMode(repeat ? RepeatMode.ONE : RepeatMode.ALL);
		
		playSong(false);
		
		if (JukeBox.getInstance().stopVanillaMusic != null) JukeBox.getInstance().stopVanillaMusic.accept(p);
		if (linked != null) linked.playingStarted();
	}

	public boolean playSong(Song song){
		if (listening == Playlists.RADIO){
			JukeBox.sendMessage(getPlayer(), Lang.UNAVAILABLE_RADIO);
			return false;
		}
		if (songPlayer != null) stopPlaying(false);
		if (song == null) return false;
		addSong(song, true);
		return listening == Playlists.FAVORITES;
	}
	
	public boolean addSong(Song song, boolean playIndex) {
		Playlist toPlay = null;
		switch (listening){
		case FAVORITES:
			if (favorites == null){
				favorites = new Playlist(song);
			}else {
				if (favorites.contains(song)) break;
				if (playIndex && songPlayer != null){
					favorites.insert(songPlayer.getPlayedSongIndex() + 1, song);
					finishPlaying();
				}else favorites.add(song);
			}
			toPlay = favorites;
			break;
		case PLAYLIST:
			randomPlaylist.add(JukeBox.getPlaylist().getIndex(song));
			if (playIndex) finishPlaying();
			toPlay = JukeBox.getPlaylist();
			break;
		case RADIO:
			return false;
		}
		if (songPlayer == null && getPlayer() != null){
			playList(toPlay);
			return listening == Playlists.FAVORITES;
		}
		return true;
	}
	
	public void removeSong(Song song){
		switch (listening){
		case FAVORITES:
			if (favorites.getCount() == 1){
				favorites = null;
				favoritesRemoved = true;
				songPlayer.setRepeatMode(repeat ? RepeatMode.ONE : RepeatMode.NO);
			}else favorites.remove(song);
			break;
		case PLAYLIST:
			randomPlaylist.remove((Integer) JukeBox.getPlaylist().getIndex(song));
			break;
		case RADIO:
			break;
		}
	}
	
	public boolean isInPlaylist(Song song){
		switch (listening){
		case FAVORITES:
			if (favorites != null) return favorites.contains(song);
			break;
		case PLAYLIST:
			return randomPlaylist.contains(JukeBox.getPlaylist().getIndex(song));
		case RADIO:
			return false;
		}
		return false;
	}

	public void clearPlaylist(){
		switch (listening){
		case FAVORITES:
			if (favorites == null) break;
			for (int i = 0; i < favorites.getCount() - 1; i++){
				favorites.remove(favorites.get(0));
			}
			removeSong(favorites.get(0));
			break;
		case PLAYLIST:
			randomPlaylist.clear();
			break;
		case RADIO:
			break;
		}
	}
	
	public Song playRandom() {
		if (JukeBox.getSongs().isEmpty()) return null;
		setPlaylist(Playlists.PLAYLIST, false);
		Song song = JukeBox.randomSong();
		playSong(song);
		return song;
	}
	
	public void stopPlaying(boolean msg) {
		if (songPlayer == null) {
			if (listening == Playlists.RADIO) {
				JukeBox.radio.leave(getPlayer());
				this.listening = Playlists.PLAYLIST;
				setPlaylist(listening, false);
			}
			return;
		}
		CustomSongPlayer tmp = songPlayer;
		this.songPlayer = null;
		tmp.destroy();
		if (msg && p != null && p.isOnline()) JukeBox.sendMessage(getPlayer(), Lang.MUSIC_STOPPED);
		if (linked != null) linked.playingStopped();
	}
	
	public Playlists getPlaylistType(){
		return listening;
	}
	
	public void nextPlaylist(){
		Playlists toPlay = null;
		
		switch (listening) {
		case PLAYLIST:
			if (Playlists.FAVORITES.hasPermission(p)) {
				toPlay = Playlists.FAVORITES;
			}else if (Playlists.RADIO.hasPermission(p)) toPlay = Playlists.RADIO;
			break;
		case FAVORITES:
			if (JukeBox.radioEnabled && Playlists.RADIO.hasPermission(p)) {
				toPlay = Playlists.RADIO;
			}else toPlay = Playlists.PLAYLIST;
			break;
		case RADIO:
			toPlay = Playlists.PLAYLIST;
			break;
		}
		if (toPlay == null) return;
		setPlaylist(toPlay, true);
	}
	
	public void setPlaylist(Playlists list, boolean play){
		stopPlaying(false);
		this.listening = list;
		if (linked != null) {
			linked.playlistItem();
			linked.setSongsPage(p);
		}
		if (!play || getPlayer() == null) return;
		switch (listening){
		case PLAYLIST:
			playList(JukeBox.getPlaylist());
			break;
		case FAVORITES:
			playList(favorites);
			break;
		case RADIO:
			JukeBox.radio.join(getPlayer());
			if (linked != null) linked.playingStarted();
			break;
		}
	}
	
	public boolean isListening() {
		return songPlayer != null || listening == Playlists.RADIO;
	}
	
	public boolean isPlaying() {
		return p != null && (songPlayer == null ? listening == Playlists.RADIO : songPlayer.isPlaying());
	}

	private void finishPlaying(){
		if (songPlayer == null) return;
		songPlayer.setTick((short) (songPlayer.getSong().getLength() + 1));
		if (!songPlayer.isPlaying()) songPlayer.setPlaying(true);
	}
	
	public void nextSong() {
		if (listening == Playlists.RADIO){
			JukeBox.sendMessage(getPlayer(), Lang.UNAVAILABLE_RADIO);
			return;
		}
		if (songPlayer == null) {
			playList(listening == Playlists.PLAYLIST ? JukeBox.getPlaylist() : favorites);
		}else {
			finishPlaying();
		}
	}
	
	public Song getListeningTo() {
		if (songPlayer != null) return songPlayer.getSong();
		if (getPlaylistType() == Playlists.RADIO) return JukeBox.radio.getSong();
		return null;
	}
	
	public String getListeningSongName() {
		Song song = getListeningTo();
		return song == null ? null : JukeBox.getSongName(song);
	}
	
	public void playerJoin(Player player, boolean replay){
		this.p = player;
		if (!replay) return;
		if (JukeBox.radioOnJoin){
			setPlaylist(Playlists.RADIO, true);
			return;
		}
		if (listening == Playlists.RADIO) return;
		if (songPlayer == null){
			if (hasJoinMusic()) playRandom();
		}else if (!songPlayer.adminPlayed && JukeBox.autoReload) {
			songPlayer.setPlaying(true);
			JukeBox.sendMessage(getPlayer(), Lang.RELOAD_MUSIC + " (" + JukeBox.getSongName(songPlayer.getSong()) + ")");
		}	
	}
	
	public void togglePlaying() {
		if (songPlayer != null) {
			songPlayer.setPlaying(!songPlayer.isPlaying());
		}else {
			if (listening == Playlists.RADIO) {
				if (JukeBox.radio.isListening(getPlayer())) {
					JukeBox.radio.leave(getPlayer());
				}else JukeBox.radio.join(getPlayer());
			}
		}
		if (JukeBox.getInstance().stopVanillaMusic != null && isPlaying()) JukeBox.getInstance().stopVanillaMusic.accept(p);
	}

	public void playerLeave(){
		if (!JukeBox.autoReload) stopPlaying(false);
		p = null;
	}
	
	private void playSong(boolean next){
		if (listening == Playlists.PLAYLIST && !randomPlaylist.isEmpty()){
			songPlayer.playSong(randomPlaylist.get(0));
			int id = randomPlaylist.remove(0);
			if (next && linked != null) linked.songItem(id, getPlayer());
		}
		JukeBox.sendMessage(getPlayer(), Lang.MUSIC_PLAYING + " " + JukeBox.getSongName(songPlayer.getSong()));
	}
	

	public UUID getID(){
		return id;
	}

	public Player getPlayer() {
		return p;
	}

	public boolean hasJoinMusic(){
		return join;
	}

	public boolean setJoinMusic(boolean join){
		this.join = join;
		if (linked != null) linked.joinItem();
		return join;
	}

	public boolean isShuffle(){
		return shuffle;
	}

	public boolean setShuffle(boolean shuffle){
		this.shuffle = shuffle;
		if (songPlayer != null) songPlayer.setRandom(true);
		if (linked != null) linked.shuffleItem();
		return shuffle;
	}

	public int getVolume(){
		return volume;
	}

	public int setVolume(int volume){
		if (id != null) NoteBlockAPI.setPlayerVolume(id, (byte) volume);
		this.volume = volume;
		if (linked != null) linked.volumeItem();
		return volume;
	}

	public boolean hasParticles(){
		return particles;
	}

	public boolean setParticles(boolean particles){
		if (songPlayer != null) songPlayer.setParticlesEnabled(particles);
		this.particles = particles;
		if (linked != null) linked.particlesItem();
		return particles;
	}
	
	public boolean isRepeatEnabled(){
		return repeat;
	}
	
	public boolean setRepeat(boolean repeat){
		this.repeat = repeat;
		if (songPlayer != null) songPlayer.setRepeatMode(repeat ? RepeatMode.ONE : (listening == Playlists.FAVORITES && favorites == null ? RepeatMode.NO : RepeatMode.ALL));
		if (linked != null) linked.repeatItem();
		return repeat;
	}
	
	public Playlist getFavorites() {
		return favorites;
	}
	
	public void setFavorites(Song... songs) {
		favorites = new Playlist(songs);
	}

	public boolean isDefault(PlayerData base){
		if (base.hasJoinMusic() != hasJoinMusic()) if (!JukeBox.autoJoin) return false;
		if (base.isShuffle() != isShuffle()) return false;
		if (base.getVolume() != getVolume()) return false;
		if (base.hasParticles() != hasParticles()) return false;
		if (base.isRepeatEnabled() != isRepeatEnabled()) return false;
		return true;
	}

	public Map<String, Object> serialize(){
		Map<String, Object> map = new HashMap<>();
		map.put("id", id.toString());

		map.put("join", hasJoinMusic());
		map.put("shuffle", isShuffle());
		map.put("volume", getVolume());
		map.put("particles", hasParticles());
		map.put("repeat", isRepeatEnabled());
		map.put("playlist", listening.name());
		
		if (favorites != null) {
			List<String> list = new ArrayList<>();
			for (Song song : favorites.getSongList()) list.add(JukeBox.getInternal(song));
			if (!list.isEmpty()) map.put("favorites", list);
		}

		return map;
	}

	static PlayerData create(UUID id){
		PlayerData pdata = new PlayerData(id, JukeBox.defaultPlayer);
		if (JukeBox.autoJoin) pdata.setJoinMusic(true);
		return pdata;
	}

	public static PlayerData deserialize(Map<String, Object> map, Map<String, Song> songsName) {
		PlayerData pdata = new PlayerData(map.containsKey("id") ? UUID.fromString((String) map.get("id")) : null);

		pdata.setJoinMusic((boolean) map.get("join"));
		pdata.setShuffle((boolean) map.get("shuffle"));
		if (map.containsKey("volume")) pdata.setVolume((int) map.get("volume"));
		if (map.containsKey("particles")) pdata.setParticles((boolean) map.get("particles"));
		if (map.containsKey("repeat")) pdata.setRepeat((boolean) map.get("repeat"));
		
		if (map.containsKey("favorites")) {
			pdata.setPlaylist(Playlists.FAVORITES, false);
			for (String s : (List<String>) map.get("favorites")) {
				Song song = songsName.get(s);
				if (song == null) {
					JukeBox.getInstance().getLogger().warning("Unknown song \"" + s + "\" for favorite playlist of " + pdata.getID().toString());
				}else pdata.addSong(song, false);
			}
			pdata.setPlaylist(Playlists.PLAYLIST, false);
		}
		if (map.containsKey("playlist")) {
			pdata.setPlaylist(Playlists.valueOf((String) map.get("playlist")), false);
		}

		return pdata;
	}
	
}