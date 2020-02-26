package fr.skytasul.music.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.music.JukeBoxInventory;

public enum Playlists{
	
	PLAYLIST(Lang.PLAYLIST, item(Material.JUKEBOX, Lang.PLAYLIST)), FAVORITES(Lang.FAVORITES, item(Material.NOTE_BLOCK, Lang.FAVORITES)), RADIO(Lang.RADIO, JukeBoxInventory.radioItem);
	
	public final ItemStack item;
	public final String name;
	
	private Playlists(String name, ItemStack item){
		this.item = item;
		this.name = name;
	}
	
	public static int indexOf(Playlists playlist){
		for (int i = 0; i < values().length; i++){
			if (values()[i] == playlist) return i;
		}
		return -1;
	}
	
	public static ItemStack item(Material material, String name) {
		return JukeBoxInventory.item(material, Lang.CHANGE_PLAYLIST + name, Lang.CHANGE_PLAYLIST_LORE.split("\n"));
	}
	
}