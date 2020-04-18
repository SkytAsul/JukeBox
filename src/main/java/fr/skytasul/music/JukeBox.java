package fr.skytasul.music;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import fr.skytasul.music.utils.JukeBoxRadio;
import fr.skytasul.music.utils.Lang;
import fr.skytasul.music.utils.Placeholders;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class JukeBox extends JavaPlugin implements Listener{

	public static int version = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3].split("_")[1]);
	private static JukeBox instance;
	private boolean disable = false;

	private static File playersFile;
	public static FileConfiguration players;
	public static File songsFolder;
	
	public static JukeBoxRadio radio = null;
	
	private static LinkedList<Song> songs;
	private static Map<String, Song> fileNames;
	//private static LinkedList<Song> ids;
	private static Playlist playlist;
	
	public static int maxPage;
	public static boolean jukeboxClick = false;
	public static boolean sendMessages = true;
	public static boolean async = false;
	public static boolean autoJoin = false;
	public static boolean radioEnabled = true;
	public static boolean radioOnJoin = false;
	public static boolean autoReload = true;
	public static PlayerData defaultPlayer = null;
	public static List<World> worldsEnabled;
	public static boolean worlds;
	public static boolean particles;
	public static boolean actionBar;
	public static Material songItem;
	public static String itemFormat;
	public static String songFormat;
	
	public ItemStack jukeboxItem;
	
	public void onEnable(){
		instance = this;
		
		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) Placeholders.registerPlaceholders();
		getLogger().info("This JukeBox version requires NoteBlockAPI version 1.5.0 or more. Please ensure you have the right version before using JukeBox (you are using NBAPI ver. " + getPlugin(NoteBlockAPI.class).getDescription().getVersion() + ")");
		
		saveDefaultConfig();
		
		initAll();
	}
	
	public void onDisable(){
		if (!disable) disableAll();
	}
	
	public void disableAll(){
		if (radio != null){
			radio.stop();
			radio = null;
		}
		List<Map<String, Object>> list = new ArrayList<>();
		for (PlayerData pdata : PlayerData.players.values()){
			if (pdata.songPlayer != null) pdata.stopPlaying(true);
			if (!pdata.isDefault(defaultPlayer)) list.add(pdata.serialize());
		}
		players.set("players", list);
		players.set("item", (jukeboxItem == null) ? null : jukeboxItem.serialize());
		try {
			players.save(playersFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		HandlerList.unregisterAll((JavaPlugin) this);
	}
	
	public void initAll(){
		reloadConfig();
		
		loadLang();
		
		FileConfiguration config = getConfig();
		jukeboxClick = config.getBoolean("jukeboxClick");
		sendMessages = config.getBoolean("sendMessages");
		async = config.getBoolean("asyncLoading");
		autoJoin = config.getBoolean("forceJoinMusic");
		defaultPlayer = PlayerData.deserialize(config.getConfigurationSection("defaultPlayerOptions").getValues(false), null);
		particles = config.getBoolean("noteParticles") && JukeBoxInventory.version > 8;
		actionBar = config.getBoolean("actionBar") && JukeBoxInventory.version > 8;
		radioEnabled = config.getBoolean("radio");
		radioOnJoin = radioEnabled && config.getBoolean("radioOnJoin");
		autoReload = config.getBoolean("reloadOnJoin");
		songItem = Material.matchMaterial(config.getString("songItem"));
		itemFormat = config.getString("itemFormat");
		songFormat = config.getString("songFormat");
		
		worldsEnabled = new ArrayList<>();
		for(String name : config.getStringList("enabledWorlds")){
			World world = Bukkit.getWorld(name);
			if (world != null) worldsEnabled.add(world);
		}
		worlds = !worldsEnabled.isEmpty();
		
		//if (getConfig().getBoolean("database.enabled")) SQLManager.connectDatabase(getConfig().getConfigurationSection("database"), getLogger());
		
		if (async){
			new BukkitRunnable() {
				@Override
				public void run() {
					loadDatas();
					finishEnabling();
				}
			}.runTaskAsynchronously(this);
		}else{
			loadDatas();
			finishEnabling();
		}
	}
	
	private void finishEnabling(){
		getCommand("music").setExecutor(new CommandMusic());
		CommandAdmin commandAdmin = new CommandAdmin();
		getCommand("adminmusic").setExecutor(commandAdmin);
		getCommand("adminmusic").setTabCompleter(commandAdmin);

		getServer().getPluginManager().registerEvents(this, this);
		
		radioEnabled = radioEnabled && !songs.isEmpty();
		if (radioEnabled){
			radio = new JukeBoxRadio(playlist);
		}else radioOnJoin = false;

		for (Player p : Bukkit.getOnlinePlayers()) {
			onJoin(new PlayerJoinEvent(p, ""));
		}
	}
	
	private void loadDatas(){
		/* --------------------------------------------- SONGS ------- */
		songs = new LinkedList<>();
		fileNames = new HashMap<>();
		//ids = new LinkedList<>();
		songsFolder = new File(getDataFolder(), "songs");
		if (!songsFolder.exists()) songsFolder.mkdirs();
		Map<String, Song> tmpSongs = new HashMap<>();
		//Map<String, Song> tmpFiles = new HashMap<>();
		for (File file : songsFolder.listFiles()){
			if (file.getName().substring(file.getName().lastIndexOf(".") + 1).equals("nbs")){
				Song song = NBSDecoder.parse(file);
				if (song == null) continue;
				String n = getInternal(song);
				if (tmpSongs.containsKey(n)){
					getLogger().warning("Song \"" + n + "\" is duplicated. Please delete one from the songs directory. File name: " + file.getName());
					continue;
				}
				fileNames.put(file.getName(), song);
				tmpSongs.put(n, song);
				//tmpFiles.put(file.getName(), song);
			}
		}
		getLogger().info(tmpSongs.size() + " songs loadeds. Sorting by name... ");
		List<String> names = new ArrayList<>(tmpSongs.keySet());
		Collections.sort(names, Collator.getInstance());
		for (String str : names){
			songs.add(tmpSongs.get(str));
		}
		/*names = new ArrayList<>(tmpFiles.keySet());
		Collections.sort(names, Collator.getInstance());
		for (String str : names){
			ids.add(tmpFiles.get(str));
		}*/
		
		setMaxPage();
		getLogger().info("Songs sorted ! " + songs.size() + " songs. Number of pages : " + maxPage);
		if (!songs.isEmpty()) playlist = new Playlist(songs.toArray(new Song[0]));

		/* --------------------------------------------- PLAYERS ------- */
		PlayerData.players = new HashMap<>();
		try {
			playersFile = new File(getDataFolder(), "datas.yml");
			boolean b = false;
			if (!playersFile.exists()){
				b = true;
				playersFile.createNewFile();
			}
			players = YamlConfiguration.loadConfiguration(playersFile);
			boolean last = getConfig().contains("players");
			if (!b || last){
				FileConfiguration tmpPlayers = last ? getConfig() : players;
				for (Map<?, ?> m : tmpPlayers.getMapList("players")){
					PlayerData pdata = PlayerData.deserialize((Map<String, Object>) m, tmpSongs);
					PlayerData.players.put(pdata.getID(), pdata);
				}
				if (tmpPlayers.get("item") != null) jukeboxItem = ItemStack.deserialize(tmpPlayers.getConfigurationSection("item").getValues(false));
				if (last){
					getLogger().info("Player datas were saved into config.yml; moved to players.yml"); 
					getConfig().set("players", null);
					saveConfig();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void setMaxPage(){
		maxPage = (int) StrictMath.ceil(songs.size() * 1.0 / 45);
	}
	

	private YamlConfiguration loadLang() {
		String s = "en.yml";
		if (getConfig().getString("lang") != null) s = getConfig().getString("lang") + ".yml";
		File lang = new File(getDataFolder(), s);
		if (!lang.exists()) {
			try {
				getDataFolder().mkdir();
				lang.createNewFile();
				InputStream defConfigStream = this.getResource(s);
				if (defConfigStream != null) {
					YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
					defConfig.save(lang);
					Lang.loadFromConfig(defConfig);
					getLogger().info("Created language file " + s);
					return defConfig;
				}
			} catch(IOException e) {
				e.printStackTrace();
				getLogger().severe("Couldn't create language file.");
				getLogger().severe("This is a fatal error. Now disabling.");
				disable = true;
				this.setEnabled(false);
			}
		}
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
		try {
			Lang.saveFile(conf, lang);
		} catch(IOException | IllegalArgumentException | IllegalAccessException e) {
			getLogger().warning("Failed to save lang.yml.");
			getLogger().warning("Report this stack trace to SkytAsul on SpigotMC.");
			e.printStackTrace();
		}
		Lang.loadFromConfig(conf);
		getLogger().info("Loaded language file " + s);
		return conf;
	}

	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		Player p = e.getPlayer();
		UUID id = p.getUniqueId();
		PlayerData pdata = PlayerData.players.get(id);
		if (pdata == null) {
			pdata = PlayerData.create(id);
			PlayerData.players.put(id, pdata);
		}
		pdata.playerJoin(p, worlds ? worldsEnabled.contains(p.getWorld()) : true);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e){
		PlayerData playerData = PlayerData.players.get(e.getPlayer().getUniqueId());
		if (playerData != null) playerData.playerLeave();
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent e){
		if (e.getItem() == null) return;
		if (jukeboxItem != null && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)){
			if (e.getItem().equals(jukeboxItem)){
				CommandMusic.open(e.getPlayer());
				e.setCancelled(true);
				return;
			}
		}
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && jukeboxClick){
			if (e.getClickedBlock().getType() == Material.JUKEBOX){
				String disc = e.getItem().getType().name();
				if (JukeBoxInventory.version < 13 ? JukeBoxInventory.discs8.contains(disc) : JukeBoxInventory.discs13.contains(disc)){
					CommandMusic.open(e.getPlayer());
					e.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler
	public void onTeleport(PlayerTeleportEvent e){
		if (!worlds) return;
		if (e.getFrom().getWorld() == e.getTo().getWorld()) return;
		if (worldsEnabled.contains(e.getTo().getWorld())) return;
		PlayerData pdata = PlayerData.players.get(e.getPlayer().getUniqueId());
		if (pdata.songPlayer != null) pdata.stopPlaying(true);
	}
	
	
	public static JukeBox getInstance(){
		return instance;
	}
	
	private static Random random = new Random();
	public static Song randomSong() {
		if (songs.isEmpty()) return null;
		if (songs.size() == 1) return songs.get(0);
		return songs.get(random.nextInt(songs.size() - 1));
	}
	
	public static Playlist getPlaylist(){
		return playlist;
	}
	
	public static List<Song> getSongs(){
		return songs;
	}

    public static Song getSongByFile(String fileName) {
        String fileNameForTest = fileName.replace(" ", "");
        for (Map.Entry<String, Song> stringSongEntry : fileNames.entrySet()) {
            if (stringSongEntry.getKey().replace(" ", "").equalsIgnoreCase(fileNameForTest)) {
                return stringSongEntry.getValue();
            }
        }
        return null;
    }
	/*public static Song[] getSongs(){
		return songsList.toArray(new Song[0]);
	}*/
	
	public static String getInternal(Song s) {
		if (s.getTitle() == null || s.getTitle().isEmpty()) return s.getPath().getName();
		return s.getTitle();
	}
	
	public static String getItemName(Song s){
		return format(itemFormat, s);
	}
	
	public static String getSongName(Song song) {
		return format(songFormat, song);
	}
	
	public static String format(String base, Song song) {
		String name = song.getTitle().isEmpty() ? song.getPath().getName() : song.getTitle();
		String author = song.getAuthor();
		String id = String.valueOf(songs.indexOf(song));
		return base.replace("{NAME}", name).replace("{AUTHOR}", author).replace("{ID}", id);
	}
	
	public static boolean sendMessage(Player p, String msg){
		if (JukeBox.sendMessages){
			if (JukeBox.actionBar){
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
			}else {
				p.sendMessage(msg);
			}
			return true;
		}
		return false;
	}
	
}
