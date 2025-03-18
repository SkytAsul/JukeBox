package fr.skytasul.music;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import fr.skytasul.music.utils.Lang;
import fr.skytasul.music.utils.Playlists;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Thanks to <i>xigsag</i> and <I>SBPrime</I> for the custom skull utility
 * @author SkytAsul
 */
public class JukeBoxInventory implements Listener{

	private static final String RADIO_TEXTURE_URL =
			"http://textures.minecraft.net/texture/148a8c55891dec76764449f57ba677be3ee88a06921ca93b6cc7c9611a7af";

	private static final Pattern NEWLINE_REGEX = Pattern.compile("\\\\n|\\n");

	private static ItemStack stopItem = item(Material.BARRIER, Lang.STOP);
	private static ItemStack menuItem = item(Material.TRAPPED_CHEST, Lang.MENU_ITEM);
	private static ItemStack toggleItem = item(JukeBox.version < 9 ? Material.STONE_BUTTON : Material.valueOf("END_CRYSTAL"), Lang.TOGGLE_PLAYING);
	private static ItemStack randomItem = item(Material.valueOf(JukeBox.version > 12 ? "FIRE_CHARGE" : "FIREBALL"), Lang.RANDOM_MUSIC);
	private static ItemStack playlistMenuItem = item(Material.CHEST, Lang.PLAYLIST_ITEM);
	private static ItemStack optionsMenuItem = item(Material.valueOf(JukeBox.version > 12 ? "COMPARATOR" : "REDSTONE_COMPARATOR"), Lang.OPTIONS_ITEM);
	private static ItemStack nextSongItem = item(Material.FEATHER, Lang.NEXT_ITEM);
	private static ItemStack clearItem = item(Material.LAVA_BUCKET, Lang.CLEAR_PLAYLIST);
	private static Material particles = JukeBox.version < 13 ? Material.valueOf("FIREWORK") : Material.valueOf("FIREWORK_ROCKET");
	private static Material sign = JukeBox.version < 14 ? Material.valueOf("SIGN") : Material.valueOf("OAK_SIGN");
	private static Material lead = JukeBox.version < 13 ? Material.valueOf("LEASH") : Material.valueOf("LEAD");
	private static List<String> playlistLore = Arrays.asList("", Lang.IN_PLAYLIST);

	private Material[] discs;
	private UUID id;
	public PlayerData pdata;

	private int page = 0;
	private ItemsMenu menu = ItemsMenu.DEFAULT;

	private Inventory inv;

	public JukeBoxInventory(Player p, PlayerData pdata) {
		Bukkit.getPluginManager().registerEvents(this, JukeBox.getInstance());
		this.id = p.getUniqueId();
		this.pdata = pdata;
		this.pdata.linked = this;

		Random ran = new Random();
		discs = new Material[JukeBox.getSongs().size()];
		for (int i = 0; i < discs.length; i++){
			discs[i] = JukeBox.songItems.get(ran.nextInt(JukeBox.songItems.size()));
		}

		this.inv = Bukkit.createInventory(null, 54, Lang.INV_NAME);

		setSongsPage(p);

		openInventory(p);
	}

	public void openInventory(Player p) {
		inv = p.openInventory(inv).getTopInventory();
		menu = ItemsMenu.DEFAULT;
		setItemsMenu();
	}

	public void setSongsPage(Player p) {
		inv.setItem(52, item(Material.ARROW, Lang.LATER_PAGE, String.format(Lang.CURRENT_PAGE, page + 1, Math.max(JukeBox.maxPage, 1)))); // max to avoid 0 pages if no songs
		inv.setItem(53, item(Material.ARROW, Lang.NEXT_PAGE, String.format(Lang.CURRENT_PAGE, page + 1, Math.max(JukeBox.maxPage, 1))));

		for (int i = 0; i < 45; i++) inv.setItem(i, null);
		if (pdata.getPlaylistType() == Playlists.RADIO) return;
		if (JukeBox.getSongs().isEmpty()) return;
		int i = 0;
		for (; i < 45; i++){
			Song s = JukeBox.getSongs().get((page*45) + i);
			ItemStack is = getSongItem(s, p);
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
			if (!JukeBox.getSongs().isEmpty() && pdata.getPlayer().hasPermission("music.random")) inv.setItem(47, randomItem);
			inv.setItem(49, playlistMenuItem);
			inv.setItem(50, optionsMenuItem);
			break;
		case OPTIONS:
			inv.setItem(46, item(Material.REDSTONE_BLOCK, "§cerror", Lang.RIGHT_CLICK));
			inv.setItem(47, item(Material.BEACON, "§cerror", Lang.LEFT_CLICK));
			volumeItem();
			if (pdata.getPlaylistType() != Playlists.RADIO) {
				if (JukeBox.particles && pdata.getPlayer().hasPermission("music.particles")) inv.setItem(48, item(particles, "§cerror"));
				particlesItem();
				if (pdata.getPlayer().hasPermission("music.play-on-join")) inv.setItem(49, item(sign, "§cerror"));
				joinItem();
				if (pdata.getPlayer().hasPermission("music.shuffle")) inv.setItem(50, item(Material.BLAZE_POWDER, "§cerror"));
				shuffleItem();
				if (pdata.getPlayer().hasPermission("music.loop")) inv.setItem(51, item(lead, "§cerror"));
				repeatItem();
			}
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
		if (JukeBox.songItems.contains(type)) {
			Song s = JukeBox.getSongs().get(page * 45 + slot);
			if (e.getClick() == ClickType.MIDDLE){
				if (pdata.isInPlaylist(s)) {
					pdata.removeSong(s);
					inv.setItem(slot, getSongItem(s, p));
				}else {
					if (pdata.addSong(s, false)) inv.setItem(slot, loreAdd(getSongItem(s, p), playlistLore));
				}
			}else if (pdata.playSong(s)) inv.setItem(slot, loreAdd(getSongItem(s, p), playlistLore));
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
			setSongsPage(p);
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
					menu = ItemsMenu.OPTIONS;
					setItemsMenu();
					break;

				}
				break;


			case OPTIONS:
				switch (slot) {
				case 46:
					if(e.getClick() == ClickType.LEFT) pdata.setVolume((byte) (pdata.getVolume() - 10));
					if (pdata.getVolume() < 0) pdata.setVolume((byte) 0);
					break;
				case 47:
					if(e.getClick() == ClickType.LEFT) pdata.setVolume((byte) (pdata.getVolume() + 10)); 
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
					setSongsPage(p);
					break;

				case 50:
					pdata.nextPlaylist();
					setSongsPage(p);
					break;

				}
				break;
			}
			break;

		}
	}

	public ItemStack getSongItem(Song s, Player p) {
		ItemStack is = item(discs[JukeBox.getSongs().indexOf(s)], JukeBox.getItemName(s, p));
		if (s.getDescription() != null && !s.getDescription().isEmpty()) loreAdd(is, splitOnSpace(JukeBox.format(JukeBox.descriptionFormat, JukeBox.descriptionFormatWithoutAuthor, s), 30));
		return is;
	}

	public void volumeItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(47), Lang.VOLUME + pdata.getVolume() + "%");
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(46), Lang.VOLUME + pdata.getVolume() + "%");
	}

	public void particlesItem(){
		if (menu != ItemsMenu.OPTIONS) return;
		if (!JukeBox.particles) return;
		if (!JukeBox.particles) inv.setItem(48, null);
		name(inv.getItem(48), ChatColor.AQUA + replaceToggle(Lang.TOGGLE_PARTICLES, pdata.hasParticles()));
	}

	public void joinItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(49), ChatColor.GREEN + replaceToggle(Lang.TOGGLE_CONNEXION_MUSIC, pdata.hasJoinMusic()));
	}

	public void shuffleItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(50), ChatColor.YELLOW + replaceToggle(Lang.TOGGLE_SHUFFLE_MODE, pdata.isShuffle()));
	}

	public void repeatItem(){
		if (menu == ItemsMenu.OPTIONS) name(inv.getItem(51), ChatColor.GOLD + replaceToggle(Lang.TOGGLE_LOOP_MODE, pdata.isRepeatEnabled()));
	}

	private String replaceToggle(String string, boolean enabled) {
		return string.replace("{TOGGLE}", enabled ? Lang.DISABLE : Lang.ENABLE);
	}

	public void playingStarted() {
		if (menu == ItemsMenu.DEFAULT) inv.setItem(46, toggleItem);
	}

	public void playingStopped() {
		if (menu == ItemsMenu.DEFAULT) inv.setItem(46, null);
	}

	public void playlistItem(){
		if (menu == ItemsMenu.PLAYLIST)
			inv.setItem(50, pdata.getPlaylistType().item);
		else if (menu == ItemsMenu.OPTIONS) setItemsMenu();
	}

	public void songItem(int id, Player p) {
		if (!(id > page*45 && id < (page+1)*45) || pdata.getPlaylistType() == Playlists.RADIO) return;
		Song song = JukeBox.getSongs().get(id);
		ItemStack is = getSongItem(song, p);
		if (pdata.isInPlaylist(song)) loreAdd(is, playlistLore);
		inv.setItem(id - page*45, is);
	}




	public static ItemStack item(Material type, String name, String... lore) {
		ItemStack is = new ItemStack(type);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(name);
		im.setLore(Arrays.stream(lore).flatMap(NEWLINE_REGEX::splitAsStream).collect(Collectors.toList()));
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
		if (is == null) return null;
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(name);
		is.setItemMeta(im);
		return is;
	}

	public static final ItemStack radioItem;
    static {
        ItemStack item = new ItemStack(Material.valueOf("PLAYER_HEAD"));
        SkullMeta headMeta = (SkullMeta) item.getItemMeta();
		UUID uuid = UUID.randomUUID();
		PlayerProfile playerProfile = Bukkit.createPlayerProfile(uuid, uuid.toString().substring(0, 16));
		PlayerTextures textures = playerProfile.getTextures();
		try {
			textures.setSkin(new URI(RADIO_TEXTURE_URL).toURL());
		} catch (MalformedURLException | URISyntaxException ex) {
			JukeBox.getInstance().getLogger()
					.severe("An error occured during initialization of Radio item. Please report it to an administrator !");
			ex.printStackTrace();
		}
		playerProfile.setTextures(textures);
		headMeta.setOwnerProfile(playerProfile);
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

		for (String str : NEWLINE_REGEX.split(string)) {
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
