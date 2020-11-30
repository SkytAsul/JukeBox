package fr.skytasul.music.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.music.JukeBoxInventory;

public enum Playlists{
	
	PLAYLIST(Lang.PLAYLIST, null, item(Material.JUKEBOX, Lang.PLAYLIST)), FAVORITES(Lang.FAVORITES, "music.favorites", item(Material.NOTE_BLOCK, Lang.FAVORITES)), RADIO(Lang.RADIO, "music.radio", JukeBoxInventory.radioItem);
	
	public final String permission;
	public final String name;
	public final ItemStack item;
	
	private Playlists(String name, String permission, ItemStack item) {
		this.name = name;
		this.permission = permission;
		this.item = item;
	}
	
	public boolean hasPermission(Player p) {
		return permission == null || p.hasPermission(permission);
	}
	
	public static int indexOf(Playlists playlist){
		for (int i = 0; i < values().length; i++){
			if (values()[i] == playlist) return i;
		}
		return -1;
	}
	
	public static ItemStack item(Material material, String name) {
		return JukeBoxInventory.item(material, Lang.CHANGE_PLAYLIST + name, Lang.CHANGE_PLAYLIST_LORE);
	}
	
}