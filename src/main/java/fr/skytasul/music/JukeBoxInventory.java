package fr.skytasul.music;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.xxmicloxx.NoteBlockAPI.model.Song;

import fr.skytasul.music.utils.Lang;
import fr.skytasul.music.utils.Playlists;

/**
 * Thanks to <i>xigsag</i> and <I>SBPrime</I> for the custom skull utility
 * @author SkytAsul
 */
public class JukeBoxInventory implements Listener{
	
	public static int version = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3].split("_")[1]);
	public static List<String> discs13 = new ArrayList<>(Arrays.asList("MUSIC_DISC_11", "MUSIC_DISC_13", "MUSIC_DISC_BLOCKS", "MUSIC_DISC_CAT", "MUSIC_DISC_CHIRP", "MUSIC_DISC_FAR", "MUSIC_DISC_MALL", "MUSIC_DISC_MELLOHI", "MUSIC_DISC_STAL", "MUSIC_DISC_STRAD", "MUSIC_DISC_WAIT", "MUSIC_DISC_WARD"));
	public static List<String> discs8 = new ArrayList<>(Arrays.asList("RECORD_10", "RECORD_11", "RECORD_12", "RECORD_3", "RECORD_4", "RECORD_5", "RECORD_6", "RECORD_7", "RECORD_8", "RECORD_9", "GOLD_RECORD", "GREEN_RECORD"));
	
	private static ItemStack stopItem = item(Material.BARRIER, Lang.STOP);
	private static ItemStack menuItem = item(Material.TRAPPED_CHEST, Lang.MENU_ITEM);
	private static ItemStack toggleItem = item(version < 9 ? Material.STONE_BUTTON : Material.valueOf("END_CRYSTAL"), Lang.TOGGLE_PLAYING);
	private static ItemStack randomItem = item(Material.valueOf(version > 12 ? "FIRE_CHARGE" : "FIREBALL"), Lang.RANDOM_MUSIC);
	private static ItemStack playlistMenuItem = item(Material.CHEST, Lang.PLAYLIST_ITEM);
	private static ItemStack optionsMenuItem = item(Material.valueOf(version > 12 ? "COMPARATOR" : "REDSTONE_COMPARATOR"), Lang.OPTIONS_ITEM);
	private static ItemStack nextSongItem = item(Material.FEATHER, Lang.NEXT_ITEM);
	private static ItemStack clearItem = item(Material.LAVA_BUCKET, Lang.CLEAR_PLAYLIST);
	private static Material particles = version < 13 ? Material.valueOf("FIREWORK") : Material.valueOf("FIREWORK_ROCKET");
	private static Material sign = version < 14 ? Material.valueOf("SIGN") : Material.valueOf("OAK_SIGN");
	private static Material lead = version < 13 ? Material.valueOf("LEASH") : Material.valueOf("LEAD");
	private static List<String> playlistLore = Arrays.asList("", Lang.IN_PLAYLIST);
	
	private Material[] discs;
	private UUID id;
	public PlayerData pdata;
	
	private int page = 0;
	private ItemsMenu menu = ItemsMenu.DEFAULT;
	
	private Inventory inv;
	
	public JukeBoxInventory(Player p){
		Bukkit.getPluginManager().registerEvents(this, JukeBox.getInstance());
		id = p.getUniqueId();
		pdata = PlayerData.players.get(p.getUniqueId());
		pdata.linked = this;

		Random ran = new Random();
		Material defaultMat = JukeBox.songItem;
		discs = new Material[JukeBox.getSongs().size()];
		for (int i = 0; i < discs.length; i++){
			discs[i] = defaultMat != null ? defaultMat : Material.valueOf(version > 12 ? discs13.get(ran.nextInt(12)) : discs8.get(ran.nextInt(12)));
		}
		
		this.inv = Bukkit.createInventory(null, 54, Lang.INV_NAME);
		
		setSongsPage();
		
		openInventory(p);
	}
	
	public void openInventory(Player p) {
		inv = p.openInventory(inv).getTopInventory();
		menu = ItemsMenu.DEFAULT;
		setItemsMenu();
	}
	
	public void setSongsPage(){
		inv.setItem(52, item(Material.ARROW, Lang.LATER_PAGE, String.format(Lang.CURRENT_PAGE, page + 1, Math.max(JukeBox.maxPage, 1)))); // max to avoid 0 pages if no songs
		inv.setItem(53, item(Material.ARROW, Lang.NEXT_PAGE, String.format(Lang.CURRENT_PAGE, page + 1, Math.max(JukeBox.maxPage, 1))));
		
		for (int i = 0; i < 45; i++) inv.setItem(i, null);
		if (pdata.getPlaylistType() == Playlists.RADIO) return;
		if (JukeBox.getSongs().isEmpty()) return;
		int i = 0;
		for (; i < 45; i++){
			Song s = JukeBox.getSongs().get((page*45) + i);
			ItemStack is = getSongItem(s);
			if (pdata.isInPlaylist(s)) loreAdd(is, playlistLore);
			inv.setItem(i, is);
			if (JukeBox.getSongs().size() - 1 == (page*45) + i) break;
		}
	}
	
	public void setItemsMenu() {
		for (int i = 45; i < 52; i++) inv.setItem(i, null);
		if (menu != ItemsMenu.DEFAULT) inv.setItem(45, menuItem);
		
		switch (menu) {
		case DEFAULT:
			inv.setItem(45, stopItem);
			if (pdata.isListening()) inv.setItem(46, toggleItem);
			if (!JukeBox.getSongs().isEmpty()) inv.setItem(47, randomItem);
			inv.setItem(49, playlistMenuItem);
			inv.setItem(50, optionsMenuItem);
			break;
		case OPTIONS:
			inv.setItem(47, item(Material.BEACON, "§cerror", Lang.RIGHT_CLICK, Lang.LEFT_CLICK));
			if (JukeBox.particles) inv.setItem(48, item(particles, "§cerror"));
			inv.setItem(49, item(sign, "§cerror"));
			inv.setItem(50, item(Material.BLAZE_POWDER, "§cerror"));
			inv.setItem(51, item(lead, "§cerror"));
			volumeItem();
			shuffleItem();
			joinItem();
			particlesItem();
			repeatItem();
			break;
		case PLAYLIST:
			inv.setItem(47, nextSongItem);
			inv.setItem(48, clearItem);
			inv.setItem(50, pdata.getPlaylistType().item);
			break;
		}
	}
	
	public UUID getID(){
		return id;
	}
	
	@EventHandler
	public void onClick(InventoryClickEvent e){
		Player p = (Player) e.getWhoClicked();
		if (e.getClickedInventory() != inv) return;
		if (e.getCurrentItem() == null) return;
		if (!p.getUniqueId().equals(id)) return;
		e.setCancelled(true);
		int slot = e.getSlot();
		
		Material type = e.getCurrentItem().getType();
		if (JukeBox.songItem == null ? type.name().contains("RECORD") || type.name().contains("DISC") : type == JukeBox.songItem) {
			Song s = JukeBox.getSongs().get(page * 45 + slot);
			if (e.getClick() == ClickType.MIDDLE){
				if (pdata.isInPlaylist(s)) {
					pdata.removeSong(s);
					inv.setItem(slot, getSongItem(s));
				}else {
					if (pdata.addSong(s, false)) inv.setItem(slot, loreAdd(getSongItem(s), playlistLore));
				}
			}else if (pdata.playSong(s)) inv.setItem(slot, loreAdd(getSongItem(s), playlistLore));
			return;
		}
		 
		switch (slot){
			
		case 52:
		case 53:
			if (JukeBox.maxPage == 0) break;
			if (slot == 53){ //Next
				if (page == JukeBox.maxPage - 1) break;
				page++;
			}else if (slot == 52){ // Later
				if (page == 0) return;
				page--;
			}
			setSongsPage();
			break;
			
		default:
			if (slot == 45) {
				if (menu == ItemsMenu.DEFAULT) {
					pdata.stopPlaying(true);
					inv.setItem(46, null);
				}else {
					menu = ItemsMenu.DEFAULT;
					setItemsMenu();
				}
				return;
			}
			
			switch (menu) {
			case DEFAULT:
				switch (slot) {
				case 46:
					pdata.togglePlaying();
					break;
					
				case 47:
					pdata.playRandom();
					break;
				
				case 49:
					menu = ItemsMenu.PLAYLIST;
					setItemsMenu();
					break;
					
				case 50:
					if (pdata.getPlaylistType() == Playlists.RADIO){
						JukeBox.sendMessage(p, Lang.UNAVAILABLE_RADIO);
						return;
					}
					menu = ItemsMenu.OPTIONS;
					setItemsMenu();
					break;
					
				}
				break;
				
				
			case OPTIONS:
				switch (slot) {
				case 47:
					if (e.getClick() == ClickType.RIGHT) pdata.setVolume((byte) (pdata.getVolume() - 10));
					if (e.getClick() == ClickType.LEFT) pdata.setVolume((byte) (pdata.getVolume() + 10));
					if (pdata.getVolume() < 0) pdata.setVolume((byte) 0);
					if (pdata.getVolume() > 100) pdata.setVolume((byte) 100);
					break;
					
				case 48:
					pdata.setParticles(!pdata.hasParticles());
					break;
					
				case 49:
					if (!JukeBox.autoJoin) pdata.setJoinMusic(!pdata.hasJoinMusic());
					break;
					
				case 50:
					pdata.setShuffle(!pdata.isShuffle());
					break;
					
				case 51:
					pdata.setRepeat(!pdata.isRepeatEnabled());
					break;
				}
				break;
				
				
			case PLAYLIST:
				switch (slot) {
				case 47:
					pdata.nextSong();
					break;
				
				case 48:
					pdata.clearPlaylist();
					setSongsPage();
					break;
					
				case 50:
					pdata.nextPlaylist();
					setSongsPage();
					break;
				
				}
				break;
			}
			break;
		
		}
	}
	
	public ItemStack getSongItem(Song s){
		ItemStack is = item(discs[JukeBox.getSongs().indexOf(s)], JukeBox.getItemName(s));
		if (!StringUtils.isEmpty(s.getDescription())) loreAdd(is, splitOnSpace(s.getDescription(), 30));
		return is;
	}
	
	public void volumeItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(47), Lang.VOLUME + pdata.getVolume() + "%");
	}
	
	public void particlesItem(){
		if (menu != ItemsMenu.OPTIONS) return;
		if (!JukeBox.particles) return;
		if (!JukeBox.particles) inv.setItem(48, null);
		name(inv.getItem(48), ChatColor.AQUA + (pdata.hasParticles() ? Lang.DISABLE : Lang.ENABLE) + " " + Lang.PARTICLES);
	}
	
	public void joinItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(49), ChatColor.GREEN + (pdata.hasJoinMusic() ? Lang.DISABLE : Lang.ENABLE) + " " + Lang.CONNEXION_MUSIC);
	}
	
	public void shuffleItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(50), ChatColor.YELLOW + (pdata.isShuffle() ? Lang.DISABLE : Lang.ENABLE) + " " + Lang.SHUFFLE_MODE);
	}
	
	public void repeatItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(51), ChatColor.GOLD + (pdata.isRepeatEnabled() ? Lang.DISABLE : Lang.ENABLE) + " " + Lang.LOOP_MODE);
	}
	
	public void playingStarted() {
		if (menu == ItemsMenu.DEFAULT) inv.setItem(46, toggleItem);
	}
	
	public void playingStopped() {
		if (menu == ItemsMenu.DEFAULT) inv.setItem(46, null);
	}
	
	public void playlistItem(){
		if (menu == ItemsMenu.PLAYLIST) inv.setItem(50, pdata.getPlaylistType().item);
	}
	
	public void songItem(int id){
		if (!(id > page*45 && id < (page+1)*45) || pdata.getPlaylistType() == Playlists.RADIO) return;
		Song song = JukeBox.getSongs().get(id);
		ItemStack is = getSongItem(song);
		if (pdata.isInPlaylist(song)) loreAdd(is, playlistLore);
		inv.setItem(id - page*45, is);
	}
	

	

	public static ItemStack item(Material type, String name, String... lore) {
		ItemStack is = new ItemStack(type);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(name);
		im.setLore(Arrays.asList(lore));
		im.addItemFlags(ItemFlag.values());
		is.setItemMeta(im);
		return is;
	}
	
	public static ItemStack loreAdd(ItemStack is, List<String> lore){
		ItemMeta im = is.getItemMeta();
		List<String> ls = im.getLore();
		if (ls == null) ls = new ArrayList<>();
		ls.addAll(lore);
		im.setLore(ls);
		is.setItemMeta(im);
		return is;
	}

	public static ItemStack name(ItemStack is, String name) {
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(name);
		is.setItemMeta(im);
		return is;
	}

	public static final ItemStack radioItem;
    static {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();
        if (propertyMap == null) {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }
        propertyMap.put("textures", new Property("textures", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQ4YThjNTU4OTFkZWM3Njc2NDQ0OWY1N2JhNjc3YmUzZWU4OGEwNjkyMWNhOTNiNmNjN2M5NjExYTdhZiJ9fX0="));
        ItemStack item;
        if (version < 13){
        	item = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }else item = new ItemStack(Material.valueOf("PLAYER_HEAD"));
        ItemMeta headMeta = item.getItemMeta();
        Field profileField;
		try {
			profileField = headMeta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(headMeta, profile);
		}catch (ReflectiveOperationException e) {
			e.printStackTrace();
			JukeBox.getInstance().getLogger().severe("An error occured during initialization of Radio item. Please report it to an administrator !");
			item = new ItemStack(Material.TORCH);
			headMeta = item.getItemMeta();
		}
        headMeta.setDisplayName(Lang.CHANGE_PLAYLIST + Lang.RADIO);
        item.setItemMeta(headMeta);
        radioItem = item;
    }
	
	public static List<String> splitOnSpace(String string, int minSize){
		if (string == null || string.isEmpty()) return null; 
		List<String> ls = new ArrayList<>();
		if (string.length() <= minSize){
			ls.add(string);
			return ls;
		}
		
		for (String str : StringUtils.splitByWholeSeparator(string, "\\n")) {
			int lastI = 0;
			int ic = 0;
			for (int i = 0; i < str.length(); i++){
				String color = "";
				if (!ls.isEmpty()) color = ChatColor.getLastColors(ls.get(ls.size() - 1));
				if (ic >= minSize){
					if (str.charAt(i) == ' '){
						ls.add(color + str.substring(lastI, i));
						ic = 0;
						lastI = i + 1;
					}else if (i + 1 == str.length()){
						ls.add(color + str.substring(lastI, i + 1));
					}
				}else if (str.length() - lastI <= minSize){
					ls.add(color + str.substring(lastI, str.length()));
					break;
				}
				ic++;
			}
		}
		
		return ls;
	}
	
	enum ItemsMenu{
		DEFAULT, OPTIONS, PLAYLIST;
	}
	
}
