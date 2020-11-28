package fr.skytasul.music.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import fr.skytasul.music.JukeBox;

public class Lang{

	public static String NEXT_PAGE = ChatColor.AQUA + "Next page";
	public static String LATER_PAGE = ChatColor.AQUA + "Previous page";
	public static String CURRENT_PAGE = ChatColor.DARK_AQUA + "§oPage %d of %d";
	public static String PLAYER = ChatColor.RED + "You must be a player to do this command.";
	public static String RELOAD_MUSIC = ChatColor.GREEN + "Music reloaded.";
	public static String INV_NAME = ChatColor.LIGHT_PURPLE + "§lJukebox !";
	public static String TOGGLE_PLAYING = ChatColor.GOLD + "Pause/play";
	public static String VOLUME = ChatColor.BLUE + "Music volume : §b";
	public static String RIGHT_CLICK = "§eRight click: decrease by 10%";
	public static String LEFT_CLICK = "§eLeft click: increase by 10%";
	public static String RANDOM_MUSIC = ChatColor.DARK_AQUA + "Random music";
	public static String STOP = ChatColor.RED + "Stop the music";
	public static String MUSIC_STOPPED = ChatColor.GREEN + "Music stopped.";
	public static String ENABLE = "Enable";
	public static String DISABLE = "Disable";
	public static String ENABLED = "Enabled";
	public static String DISABLED = "Disabled";
	public static String SHUFFLE_MODE = "the shuffle mode";
	public static String LOOP_MODE = "the loop mode";
	public static String CONNEXION_MUSIC = "music when connecting";
	public static String PARTICLES = "particles";
	public static String MUSIC_PLAYING = ChatColor.GREEN + "Music while playing:";
	public static String INCORRECT_SYNTAX = ChatColor.RED + "Incorrect syntax.";
	public static String RELOAD_LAUNCH = ChatColor.GREEN + "Trying to reload.";
	public static String RELOAD_FINISH = ChatColor.GREEN + "Reload finished.";
	public static String AVAILABLE_COMMANDS = ChatColor.GREEN + "Available commands:";
	public static String INVALID_NUMBER = ChatColor.RED + "Invalid number.";
	public static String PLAYER_MUSIC_STOPPED = ChatColor.GREEN + "Music stopped for player: §b";
	public static String IN_PLAYLIST = ChatColor.BLUE + "§oIn Playlist";
	public static String PLAYLIST_ITEM = ChatColor.LIGHT_PURPLE + "Playlists";
	public static String OPTIONS_ITEM = ChatColor.AQUA + "Options";
	public static String MENU_ITEM = ChatColor.GOLD + "Return to menu";
	public static String CLEAR_PLAYLIST = ChatColor.RED + "Clear the current playlist";
	public static String NEXT_ITEM = ChatColor.YELLOW + "Next song";
	public static String CHANGE_PLAYLIST = ChatColor.GOLD + "§lSwitch playlist: §r";
	public static String CHANGE_PLAYLIST_LORE = ChatColor.YELLOW + "Middle-click on a music disc\n§e to add/remove the song";
	public static String PLAYLIST = ChatColor.DARK_PURPLE + "Playlist";
	public static String FAVORITES = ChatColor.DARK_RED + "Favorites";
	public static String RADIO = ChatColor.DARK_AQUA + "Radio";
	public static String UNAVAILABLE_RADIO = ChatColor.RED + "This action is unavailable while listening to the radio.";
	public static String NONE = ChatColor.RED + "none";

	public static void saveFile(YamlConfiguration cfg, File file) throws IllegalArgumentException, IllegalAccessException, IOException {
		for (Field f : Lang.class.getDeclaredFields()){
			if (f.getType() != String.class) continue;
			if (!cfg.contains(f.getName())) cfg.set(f.getName(), f.get(null));
		}
		cfg.save(file);
	}
	
	public static void loadFromConfig(YamlConfiguration cfg){
		for (String key : cfg.getValues(false).keySet()){
			try {
				String str = cfg.getString(key);
				str = ChatColor.translateAlternateColorCodes('&', str);
				if (JukeBox.version >= 16) str = translateHexColorCodes("(&|§)#", "", str);
				Lang.class.getDeclaredField(key).set(key, str);
			}catch (Exception e) {
				JukeBox.getInstance().getLogger().warning("Error when loading language value \"" + key + "\".");
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private static final char COLOR_CHAR = '\u00A7';
	
	private static String translateHexColorCodes(String startTag, String endTag, String message) {
		final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
		Matcher matcher = hexPattern.matcher(message);
		StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
		while (matcher.find()) {
			String group = matcher.group(2);
			matcher.appendReplacement(buffer, COLOR_CHAR + "x"
					+ COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
					+ COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
					+ COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5));
		}
		return matcher.appendTail(buffer).toString();
	}
	
}
